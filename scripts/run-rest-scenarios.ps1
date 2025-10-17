# Usage example:
# $uid = '808b62ed-dde8-462a-a753-cbfef3f692f7'
# powershell -ExecutionPolicy Bypass -File .\scripts\run-rest-scenarios.ps1 -UserId $uid
Param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory=$true)][string]$UserId
)

# Helper to POST JSON
function Invoke-PostJson($uri, $body) {
    return Invoke-RestMethod -Method Post -Uri $uri -Headers @{ userId = $UserId } -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
}

# Helper to PUT JSON
function Invoke-PutJson($uri, $body) {
    return Invoke-RestMethod -Method Put -Uri $uri -Headers @{ userId = $UserId } -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
}

# Helper to GET
function Invoke-Get($uri) {
    return Invoke-RestMethod -Method Get -Uri $uri -Headers @{ userId = $UserId }
}

# Safe GET with error handling (returns $null on 4xx/5xx)
function Safe-Get($uri) {
    try { return Invoke-Get $uri } catch { return $null }
}

# Retry wrapper to reduce transient 5xx/empty-state races
function Retry-Get($uri, [int]$retries = 3, [int]$delayMs = 150) {
    for ($i=0; $i -lt $retries; $i++) {
        $res = Safe-Get $uri
        if ($res) { return $res }
        Start-Sleep -Milliseconds $delayMs
    }
    return $null
}

Write-Host "Running REST scenarios against $BaseUrl for userId: $UserId" -ForegroundColor Cyan

# 1) Competing requests + block
Write-Host "\n[ONE] Competing requests + block" -ForegroundColor Yellow
# Try to stop any previous session; ignore errors
Safe-Get "$BaseUrl/bpjs/stop" | Out-Null
$src1 = @'
bp.registerBThread("RequesterA", function () {
  while (true) { bp.sync({ request: bp.Event("A") }); }
});
bp.registerBThread("RequesterB", function () {
  while (true) { bp.sync({ request: bp.Event("B") }); }
});
bp.registerBThread("BlockBWhenA", function () {
  while (true) {
    bp.sync({ waitFor: bp.Event("A") });
    bp.sync({ block: bp.Event("B") });
  }
});
'@
Invoke-PostJson "$BaseUrl/bpjs/debug" @{ sourceCode = $src1; breakpoints = @(); skipBreakpointsToggle = $false; skipSyncStateToggle = $false; waitForExternalEvents = $false } | Out-Null
Start-Sleep -Milliseconds 100
for ($i=0; $i -lt 5; $i++) {
    $resp = Retry-Get "$BaseUrl/bpjs/nextSync"; if (-not $resp) { break }
    $resp.context | ConvertTo-Json -Depth 10
    Start-Sleep -Milliseconds 75
}

# 2) External-driven (Tick/Tock)
Write-Host "\n[TWO] External-driven Tick/Tock" -ForegroundColor Yellow
Safe-Get "$BaseUrl/bpjs/stop" | Out-Null
$src2 = @'
bp.registerBThread("Ticker", function () {
  while (true) {
    bp.sync({ waitFor: bp.Event("Tick") });
    bp.sync({ request: bp.Event("Tock") });
  }
});
'@
Invoke-PostJson "$BaseUrl/bpjs/debug" @{ sourceCode = $src2; breakpoints = @(); skipBreakpointsToggle = $false; skipSyncStateToggle = $false; waitForExternalEvents = $true } | Out-Null
Start-Sleep -Milliseconds 100
Invoke-PostJson "$BaseUrl/bpjs/externalEvent" @{ externalEvent = "Tick"; addEvent = $true } | Out-Null
Start-Sleep -Milliseconds 100
$r2 = Retry-Get "$BaseUrl/bpjs/nextSync"; if ($r2) { $r2.context | ConvertTo-Json -Depth 10 }
$eh2 = Retry-Get "$BaseUrl/bpjs/events?from=0&to=50"; if ($eh2) { $eh2 | ConvertTo-Json -Depth 10 }

# 3) Multi-step local state + breakpoints
Write-Host "\n[THREE] Multi-step local state + breakpoints" -ForegroundColor Yellow
Safe-Get "$BaseUrl/bpjs/stop" | Out-Null
$src3 = @'
bp.registerBThread("main", function () {
  var c = 0;
  c = c + 1;
  bp.sync({ request: bp.Event("Go") });
  c = c + 2;
  bp.sync({ request: bp.Event("Done") });
});
'@
Invoke-PostJson "$BaseUrl/bpjs/debug" @{ sourceCode = $src3; breakpoints = @(1,2,3,4,5); skipBreakpointsToggle = $false; skipSyncStateToggle = $false; waitForExternalEvents = $false } | Out-Null
Start-Sleep -Milliseconds 100
$c1 = Retry-Get "$BaseUrl/bpjs/continue"; if ($c1) { $c1.context | ConvertTo-Json -Depth 10 }
Start-Sleep -Milliseconds 75
$s1 = Retry-Get "$BaseUrl/bpjs/stepOver"; if ($s1) { $s1.context | ConvertTo-Json -Depth 10 }
Start-Sleep -Milliseconds 75
$s2 = Retry-Get "$BaseUrl/bpjs/stepOver"; if ($s2) { $s2.context | ConvertTo-Json -Depth 10 }
Start-Sleep -Milliseconds 75
$n3 = Retry-Get "$BaseUrl/bpjs/nextSync"; if ($n3) { $n3.context | ConvertTo-Json -Depth 10 }

# 4) Mutual exclusion
Write-Host "\n[FOUR] Mutual exclusion" -ForegroundColor Yellow
Safe-Get "$BaseUrl/bpjs/stop" | Out-Null
$src4 = @'
bp.registerBThread("Enter", function () {
  while (true) {
    bp.sync({ request: bp.Event("Enter") });
    bp.sync({ request: bp.Event("Inside") });
    bp.sync({ request: bp.Event("Exit") });
  }
});
bp.registerBThread("Mutex", function () {
  while (true) {
    bp.sync({ waitFor: bp.Event("Enter") });
    bp.sync({ block: bp.Event("Enter") });
    bp.sync({ waitFor: bp.Event("Exit") });
  }
});
'@
Invoke-PostJson "$BaseUrl/bpjs/debug" @{ sourceCode = $src4; breakpoints = @(); skipBreakpointsToggle = $false; skipSyncStateToggle = $false; waitForExternalEvents = $false } | Out-Null
Start-Sleep -Milliseconds 100
for ($i=0; $i -lt 6; $i++) {
    $r = Retry-Get "$BaseUrl/bpjs/nextSync"; if (-not $r) { break }
    $r.context | ConvertTo-Json -Depth 10
    Start-Sleep -Milliseconds 75
}

# 5) Toggle configs mid-run
Write-Host "\n[FIVE] Toggle configs mid-run" -ForegroundColor Yellow
Safe-Get "$BaseUrl/bpjs/stop" | Out-Null
# Start long-lived ticker so session persists while toggling
$tickerSrc = @'
bp.registerBThread("Ticker", function () {
  while (true) {
    bp.sync({ waitFor: bp.Event("Tick") });
    bp.sync({ request: bp.Event("Tock") });
  }
});
'@
Invoke-PostJson "$BaseUrl/bpjs/debug" @{ sourceCode = $tickerSrc; breakpoints=@(); skipBreakpointsToggle=$false; skipSyncStateToggle=$false; waitForExternalEvents=$true } | Out-Null
Start-Sleep -Milliseconds 150
Invoke-PutJson "$BaseUrl/bpjs/breakpoint" @{ skipBreakpoints = $true } | Out-Null
Invoke-PutJson "$BaseUrl/bpjs/syncStates" @{ skipSyncStates = $true } | Out-Null
Invoke-PutJson "$BaseUrl/bpjs/waitExternal" @{ waitForExternal = $true } | Out-Null
$cfg = Retry-Get "$BaseUrl/bpjs/nextSync"; if ($cfg) { $cfg.context.debuggerConfigs | Format-Table -AutoSize }

Write-Host "\nAll scenarios completed." -ForegroundColor Green


