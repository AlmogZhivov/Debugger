# Simple test for COBP ctx support
# Quick test to verify ctx.bthread works

Write-Host "Simple COBP ctx test..." -ForegroundColor Green

$BaseUrl = "http://localhost:8080/bpjs"
$userId = "b755e309-d329-4b4d-9148-122483f46151"

# COBP test code
$cobpCode = @'
ctx.bthread('test', 'Room.All', function() {
  ctx.sync({ request: ctx.Event('Hello') });
});
'@

$body = @{
    sourceCode = $cobpCode
    breakpoints = @()
    skipBreakpointsToggle = $false
    skipSyncStateToggle = $false
    waitForExternalEvents = $false
} | ConvertTo-Json -Compress

try {
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/debug" -Headers @{ userId = $userId } -ContentType "application/json" -Body $body
    
    Write-Host "Success: $($response.success)" -ForegroundColor $(if ($response.success) { "Green" } else { "Red" })
    
    if ($response.success) {
        Write-Host "✅ COBP ctx.bthread works!" -ForegroundColor Green
        
        # Show the full response structure for debugging
        Write-Host "`nResponse structure:" -ForegroundColor Yellow
        Write-Host "Full response keys: $($response.PSObject.Properties.Name -join ', ')" -ForegroundColor Cyan
        Write-Host "context: $($response.context -ne $null)" -ForegroundColor Cyan
        Write-Host "cobpContext: $($response.cobpContext -ne $null)" -ForegroundColor Cyan
        if ($response.cobpContext) {
            Write-Host "cobpContext.currentBThreadContext: $($response.cobpContext.currentBThreadContext)" -ForegroundColor Cyan
        }
        if ($response.context) {
            Write-Host "context.cobpContext: $($response.context.cobpContext -ne $null)" -ForegroundColor Cyan
            if ($response.context.cobpContext) {
                Write-Host "context.cobpContext.currentBThreadContext: $($response.context.cobpContext.currentBThreadContext)" -ForegroundColor Cyan
            }
        }
        
        if ($response.cobpContext) {
            Write-Host "✅ COBP context extracted!" -ForegroundColor Green
            Write-Host "Current context: $($response.cobpContext.currentBThreadContext)" -ForegroundColor Cyan
        } elseif ($response.context -and $response.context.cobpContext) {
            Write-Host "✅ COBP context extracted!" -ForegroundColor Green
            Write-Host "Current context: $($response.context.cobpContext.currentBThreadContext)" -ForegroundColor Cyan
        } else {
            Write-Host "⚠️ No COBP context found" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ COBP failed: $($response.errorCode)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "Test completed." -ForegroundColor Cyan
