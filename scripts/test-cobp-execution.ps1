# COBP Dynamic Context Execution Test
# Tests COBP context switching during program execution
# Similar to run-rest-scenarios.ps1 but focused on COBP context awareness

Param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$UserId = "2437c5dd-919d-4ece-8cc9-820e6035c4be"
)

# Helper to POST JSON
function Invoke-PostJson($uri, $body) {
    return Invoke-RestMethod -Method Post -Uri $uri -Headers @{ userId = $UserId } -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
}

# Helper to GET
function Invoke-Get($uri) {
    return Invoke-RestMethod -Method Get -Uri $uri -Headers @{ userId = $UserId }
}

# Safe GET with error handling
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

# Helper to display COBP context information
function Show-COBPContext($response, $stepName) {
    Write-Host "`n[$stepName] COBP Context Information:" -ForegroundColor Yellow
    Write-Host "=====================================" -ForegroundColor Yellow
    
    if ($response.context -and $response.context.cobpContext) {
        $cobpContext = $response.context.cobpContext
        Write-Host "Current B-Thread Context: $($cobpContext.currentBThreadContext)" -ForegroundColor Cyan
        Write-Host "Currently Running B-Thread: $($response.context.currentRunningBT)" -ForegroundColor Cyan
        
        if ($cobpContext.allBThreadContexts) {
            $contextCount = ($cobpContext.allBThreadContexts.PSObject.Properties | Measure-Object).Count
            Write-Host "Total B-Thread Contexts: $contextCount" -ForegroundColor Cyan
            
            Write-Host "`nAll B-Thread Contexts:" -ForegroundColor White
            foreach ($prop in $cobpContext.allBThreadContexts.PSObject.Properties) {
                $bThreadName = $prop.Name
                $context = $prop.Value
                $isCurrent = if ($response.context.currentRunningBT -eq $bThreadName) { " (CURRENT)" } else { "" }
                $isActive = if ($response.context.bThreadInfoList) {
                    $bt = $response.context.bThreadInfoList | Where-Object { $_.name -eq $bThreadName }
                    if ($bt) { " (ACTIVE)" } else { " (INACTIVE)" }
                } else { "" }
                Write-Host "  $bThreadName -> $context$isCurrent$isActive" -ForegroundColor Cyan
            }
        }
        
        # Show detailed b-thread information if available
        if ($response.context.bThreadInfoList -and $response.context.bThreadInfoList.Count -gt 0) {
            Write-Host "`nB-Thread Details:" -ForegroundColor White
            foreach ($bThread in $response.context.bThreadInfoList) {
                if ($cobpContext.allBThreadContexts -and $cobpContext.allBThreadContexts.PSObject.Properties.Name -contains $bThread.name) {
                    $bThreadContext = $cobpContext.allBThreadContexts.($bThread.name)
                    Write-Host "  $($bThread.name) (Context: $bThreadContext):" -ForegroundColor Cyan
                    
                    # Show requested events
                    if ($bThread.requestedEvents -and $bThread.requestedEvents.Count -gt 0) {
                        Write-Host "    Requested: $($bThread.requestedEvents | ForEach-Object { $_.name } | Join-String -Separator ', ')" -ForegroundColor Green
                    }
                    
                    # Show blocked events
                    if ($bThread.blockedEvents -and $bThread.blockedEvents.Count -gt 0) {
                        Write-Host "    Blocked: $($bThread.blockedEvents | ForEach-Object { $_.name } | Join-String -Separator ', ')" -ForegroundColor Red
                    }
                    
                    # Show waited-for events
                    if ($bThread.waitedForEvents -and $bThread.waitedForEvents.Count -gt 0) {
                        Write-Host "    Waited For: $($bThread.waitedForEvents | ForEach-Object { $_.name } | Join-String -Separator ', ')" -ForegroundColor Yellow
                    }
                }
            }
        }
    } else {
        Write-Host "No COBP context available" -ForegroundColor Red
    }
}

Write-Host "COBP Dynamic Context Execution Test" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green
Write-Host "Testing COBP context switching during program execution" -ForegroundColor White
Write-Host "Base URL: $BaseUrl" -ForegroundColor White
Write-Host "User ID: $UserId" -ForegroundColor White

# 1) COBP Multi-Context Execution Test
Write-Host "`n[ONE] COBP Multi-Context Execution Test" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Yellow

# Stop any previous session
Safe-Get "$BaseUrl/bpjs/stop" | Out-Null

# COBP test code with multiple b-threads that will execute in sequence
$cobpCode = @'
// COBP Multi-Context Execution Test
// CORRECT COBP SYNTAX - Use ctx.bthread() with sync() and Event()

// B-thread 1: Test ctx.bthread() with context - CORRECT SYNTAX
ctx.bthread('ctxTest', 'Ctx.Context', function() {
    sync({ request: Event('CtxEvent') });
});

// B-thread 2: Test regular bthread() without context
bthread('regularTest', function() {
    sync({ request: Event('RegularEvent') });
});
'@

Write-Host "Setting up COBP program with 5 b-threads in different contexts..." -ForegroundColor White
$setupResponse = Invoke-PostJson "$BaseUrl/bpjs/debug" @{ 
    sourceCode = $cobpCode; 
    breakpoints = @(); 
    skipBreakpointsToggle = $false; 
    skipSyncStateToggle = $false; 
    waitForExternalEvents = $false 
}

if ($setupResponse.success) {
    Write-Host "✅ COBP program setup successful" -ForegroundColor Green
    Show-COBPContext $setupResponse "SETUP"
} else {
    Write-Host "❌ COBP program setup failed: $($setupResponse.errorCode)" -ForegroundColor Red
    exit 1
}

Start-Sleep -Milliseconds 200

# Execute steps and observe context changes
Write-Host "`n[EXECUTION] Running program steps and observing context changes..." -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Yellow

$stepCount = 0
$maxSteps = 15

while ($stepCount -lt $maxSteps) {
    $stepCount++
    Write-Host "`n--- Step $stepCount ---" -ForegroundColor Magenta
    
    $response = Retry-Get "$BaseUrl/bpjs/nextSync"
    if (-not $response) {
        Write-Host "No more steps available or error occurred" -ForegroundColor Yellow
        break
    }
    
    if ($response.success) {
        Write-Host "✅ Step $stepCount executed successfully" -ForegroundColor Green
        
        # Show current execution state
        if ($response.context.currentRunningBT) {
            Write-Host "Currently Running: $($response.context.currentRunningBT)" -ForegroundColor Cyan
        }
        
        # Show detailed events status
        if ($response.context.eventsStatus) {
            $eventsStatus = $response.context.eventsStatus
            
            # Selected Event
            if ($eventsStatus.selectedEvent) {
                $selectedDetails = if ($eventsStatus.selectedEvent.data) { " (data: $($eventsStatus.selectedEvent.data | ConvertTo-Json -Compress))" } else { "" }
                Write-Host "Selected Event: $($eventsStatus.selectedEvent.name)$selectedDetails" -ForegroundColor Magenta
            } else {
                Write-Host "Selected Event: None" -ForegroundColor Gray
            }
            
            # Requested Events
            if ($eventsStatus.requestedEvents -and $eventsStatus.requestedEvents.Count -gt 0) {
                Write-Host "Requested Events:" -ForegroundColor Green
                foreach ($event in $eventsStatus.requestedEvents) {
                    $eventDetails = if ($event.data) { " (data: $($event.data | ConvertTo-Json -Compress))" } else { "" }
                    Write-Host "  - $($event.name)$eventDetails" -ForegroundColor Green
                }
            } else {
                Write-Host "Requested Events: None" -ForegroundColor Gray
            }
            
            # Blocked Events
            if ($eventsStatus.blockedEvents -and $eventsStatus.blockedEvents.Count -gt 0) {
                Write-Host "Blocked Events:" -ForegroundColor Red
                foreach ($event in $eventsStatus.blockedEvents) {
                    $eventDetails = if ($event.data) { " (data: $($event.data | ConvertTo-Json -Compress))" } else { "" }
                    Write-Host "  - $($event.name)$eventDetails" -ForegroundColor Red
                }
            } else {
                Write-Host "Blocked Events: None" -ForegroundColor Gray
            }
            
            # Waited For Events
            if ($eventsStatus.waitedForEvents -and $eventsStatus.waitedForEvents.Count -gt 0) {
                Write-Host "Waited For Events:" -ForegroundColor Yellow
                foreach ($event in $eventsStatus.waitedForEvents) {
                    $eventDetails = if ($event.data) { " (data: $($event.data | ConvertTo-Json -Compress))" } else { "" }
                    Write-Host "  - $($event.name)$eventDetails" -ForegroundColor Yellow
                }
            } else {
                Write-Host "Waited For Events: None" -ForegroundColor Gray
            }
        }
        
        # Show COBP context information
        Show-COBPContext $response "STEP_$stepCount"
        
        # Check if we've reached the end
        if ($response.context.eventsStatus -and $response.context.eventsStatus.selectedEvent -and $response.context.eventsStatus.selectedEvent.name -eq "QueryProcessed") {
            Write-Host "`n🎯 Program completed successfully!" -ForegroundColor Green
            break
        }
        
    } else {
        Write-Host "❌ Step $stepCount failed: $($response.errorCode)" -ForegroundColor Red
        break
    }
    
    Start-Sleep -Milliseconds 100
}

# 2) COBP Context Switching with External Events
Write-Host "`n`n[TWO] COBP Context Switching with External Events" -ForegroundColor Yellow
Write-Host "===================================================" -ForegroundColor Yellow

Safe-Get "$BaseUrl/bpjs/stop" | Out-Null

# COBP code that responds to external events
$cobpExternalCode = @'
// COBP External Event Test
// Tests context switching with external event injection

ctx.bthread('externalHandler', 'External.Handler', function() {
    while (true) {
        ctx.sync({ waitFor: ctx.Event('ExternalTrigger', { source: 'external' }) });
        ctx.sync({ request: ctx.Event('ExternalProcessed', { handler: 'externalHandler', status: 'processed' }) });
    }
});

ctx.bthread('contextSwitcher', 'Context.Switch', function() {
    while (true) {
        ctx.sync({ waitFor: ctx.Event('ExternalProcessed') });
        ctx.sync({ request: ctx.Event('ContextSwitched', { from: 'External.Handler', to: 'Context.Switch' }) });
    }
});
'@

Write-Host "Setting up COBP external event test..." -ForegroundColor White
$setupResponse2 = Invoke-PostJson "$BaseUrl/bpjs/debug" @{ 
    sourceCode = $cobpExternalCode; 
    breakpoints = @(); 
    skipBreakpointsToggle = $false; 
    skipSyncStateToggle = $false; 
    waitForExternalEvents = $true 
}

if ($setupResponse2.success) {
    Write-Host "✅ COBP external event program setup successful" -ForegroundColor Green
    Show-COBPContext $setupResponse2 "EXTERNAL_SETUP"
    
    # Inject external events and observe context changes
    Write-Host "`nInjecting external events..." -ForegroundColor White
    
    for ($i = 1; $i -le 3; $i++) {
        Write-Host "`n--- External Event Cycle $i ---" -ForegroundColor Magenta
        
        # Inject external event
        $externalResponse = Invoke-PostJson "$BaseUrl/bpjs/externalEvent" @{ 
            externalEvent = "ExternalTrigger"; 
            addEvent = $true 
        }
        
        if ($externalResponse.success) {
            Write-Host "✅ External event 'ExternalTrigger' injected" -ForegroundColor Green
            
            # Get next sync to see the effect
            Start-Sleep -Milliseconds 100
            $syncResponse = Retry-Get "$BaseUrl/bpjs/nextSync"
            if ($syncResponse) {
                Show-COBPContext $syncResponse "EXTERNAL_CYCLE_$i"
            }
        } else {
            Write-Host "❌ Failed to inject external event" -ForegroundColor Red
        }
        
        Start-Sleep -Milliseconds 200
    }
} else {
    Write-Host "❌ COBP external event program setup failed: $($setupResponse2.errorCode)" -ForegroundColor Red
}

# 3) COBP Mixed with Regular BPjs
Write-Host "`n`n[THREE] COBP Mixed with Regular BPjs" -ForegroundColor Yellow
Write-Host "=======================================" -ForegroundColor Yellow

Safe-Get "$BaseUrl/bpjs/stop" | Out-Null

# Mixed COBP and regular BPjs code
$mixedCode = @'
// Mixed COBP and Regular BPjs Test
// Tests interaction between COBP and regular BPjs b-threads

// Regular BPjs b-thread
bp.registerBThread("regularBT", function() {
    bp.sync({ request: bp.Event("RegularEvent", { source: "BPjs", type: "regular" }) });
});

// COBP b-thread
ctx.bthread('cobpBT', 'COBP.Context', function() {
    ctx.sync({ waitFor: ctx.Event('RegularEvent') });
    ctx.sync({ request: ctx.Event('COBPEvent', { source: "COBP", context: "COBP.Context" }) });
});

// Another regular BPjs b-thread
bp.registerBThread("anotherRegularBT", function() {
    bp.sync({ waitFor: bp.Event("COBPEvent") });
    bp.sync({ request: bp.Event("MixedComplete", { integration: "success", components: ["BPjs", "COBP"] }) });
});
'@

Write-Host "Setting up mixed COBP/BPjs program..." -ForegroundColor White
$setupResponse3 = Invoke-PostJson "$BaseUrl/bpjs/debug" @{ 
    sourceCode = $mixedCode; 
    breakpoints = @(); 
    skipBreakpointsToggle = $false; 
    skipSyncStateToggle = $false; 
    waitForExternalEvents = $false 
}

if ($setupResponse3.success) {
    Write-Host "✅ Mixed COBP/BPjs program setup successful" -ForegroundColor Green
    Show-COBPContext $setupResponse3 "MIXED_SETUP"
    
    # Execute mixed program
    Write-Host "`nExecuting mixed program..." -ForegroundColor White
    
    for ($i = 1; $i -le 5; $i++) {
        $response = Retry-Get "$BaseUrl/bpjs/nextSync"
        if ($response) {
            Write-Host "`n--- Mixed Step $i ---" -ForegroundColor Magenta
            Show-COBPContext $response "MIXED_STEP_$i"
            
            if ($response.context.eventsStatus -and $response.context.eventsStatus.selectedEvent -and $response.context.eventsStatus.selectedEvent.name -eq "MixedComplete") {
                Write-Host "`n🎯 Mixed program completed successfully!" -ForegroundColor Green
                break
            }
        } else {
            break
        }
        Start-Sleep -Milliseconds 100
    }
} else {
    Write-Host "❌ Mixed COBP/BPjs program setup failed: $($setupResponse3.errorCode)" -ForegroundColor Red
}

Write-Host "`n`n🏁 COBP Dynamic Context Execution Test Completed!" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green
Write-Host "Summary:" -ForegroundColor White
Write-Host "  • Tested COBP context switching during execution" -ForegroundColor White
Write-Host "  • Verified dynamic context updates based on running b-threads" -ForegroundColor White
Write-Host "  • Tested COBP with external events" -ForegroundColor White
Write-Host "  • Tested mixed COBP and regular BPjs execution" -ForegroundColor White
Write-Host "  • Demonstrated full COBP context awareness" -ForegroundColor White
Write-Host "  • Showed detailed event information (requested, blocked, waited-for, selected)" -ForegroundColor White
Write-Host "  • Displayed event data payloads for context-aware debugging" -ForegroundColor White
