# Test Regular BPjs (Non-COBP) to compare with COBP behavior
# This will help us understand if the issue is COBP-specific or system-wide

$BaseUrl = "http://localhost:8080"
$UserId = "fac0b8fb-7e82-4aa5-a662-b11245bb892f"

Write-Host "Regular BPjs Test (Non-COBP)" -ForegroundColor Yellow
Write-Host "=============================" -ForegroundColor Yellow
Write-Host "Base URL: $BaseUrl" -ForegroundColor White

# Stop any previous session
try {
    Invoke-RestMethod -Uri "$BaseUrl/bpjs/stop" -Method Get -ErrorAction SilentlyContinue | Out-Null
} catch {
    # Ignore errors
}

# Regular BPjs test code (no ctx object)
$regularBpCode = @'
// Regular BPjs Test - No COBP, just standard BPjs
// This should work and show events

// B-thread 1: Simple test - just request one event
bthread('regularTest1', function() {
    sync({ request: Event('RegularEvent1', { message: 'Hello from regular BPjs' }) });
});

// B-thread 2: Another simple test
bthread('regularTest2', function() {
    sync({ request: Event('RegularEvent2', { message: 'Hello from another regular BPjs b-thread' }) });
});
'@

Write-Host "`nSetting up regular BPjs program..." -ForegroundColor White
$setupResponse = Invoke-RestMethod -Uri "$BaseUrl/bpjs/debug" -Method Post -Body (@{
    sourceCode = $regularBpCode; 
    breakpoints = @(); 
    isSkipBreakpoints = $false; 
    isSkipSyncPoints = $false; 
    isWaitForExternalEvents = $false
} | ConvertTo-Json) -ContentType "application/json"

if ($setupResponse.success) {
    Write-Host "✓ Regular BPjs program setup successful" -ForegroundColor Green
    
    Write-Host "`n[SETUP] Regular BPjs Information:" -ForegroundColor Yellow
    Write-Host "=====================================" -ForegroundColor Yellow
    
    if ($setupResponse.context.currentRunningBT) {
        Write-Host "Currently Running B-Thread: $($setupResponse.context.currentRunningBT)" -ForegroundColor Cyan
    } else {
        Write-Host "Currently Running B-Thread: None" -ForegroundColor Gray
    }
    
    # Show events status
    if ($setupResponse.context.eventsStatus) {
        $eventsStatus = $setupResponse.context.eventsStatus
        
        if ($eventsStatus.requestedEvents -and $eventsStatus.requestedEvents.Count -gt 0) {
            Write-Host "Requested Events:" -ForegroundColor Green
            foreach ($event in $eventsStatus.requestedEvents) {
                $eventDetails = if ($event.data) { " (data: $($event.data | ConvertTo-Json -Compress))" } else { "" }
                Write-Host "  - $($event.name)$eventDetails" -ForegroundColor Green
            }
        } else {
            Write-Host "Requested Events: None" -ForegroundColor Gray
        }
        
        if ($eventsStatus.selectedEvent) {
            $selectedDetails = if ($eventsStatus.selectedEvent.data) { " (data: $($eventsStatus.selectedEvent.data | ConvertTo-Json -Compress))" } else { "" }
            Write-Host "Selected Event: $($eventsStatus.selectedEvent.name)$selectedDetails" -ForegroundColor Magenta
        } else {
            Write-Host "Selected Event: None" -ForegroundColor Gray
        }
    }
    
    # Try to execute one step
    Write-Host "`n[EXECUTION] Running one step..." -ForegroundColor Yellow
    Write-Host "================================" -ForegroundColor Yellow
    
    try {
        $stepResponse = Invoke-RestMethod -Uri "$BaseUrl/bpjs/nextSync" -Method Post -Body (@{
            userId = $UserId
        } | ConvertTo-Json) -ContentType "application/json"
        
        if ($stepResponse.success) {
            Write-Host "✓ Step executed successfully" -ForegroundColor Green
            
            if ($stepResponse.context.currentRunningBT) {
                Write-Host "Currently Running: $($stepResponse.context.currentRunningBT)" -ForegroundColor Cyan
            }
            
            # Show events status after step
            if ($stepResponse.context.eventsStatus) {
                $eventsStatus = $stepResponse.context.eventsStatus
                
                if ($eventsStatus.requestedEvents -and $eventsStatus.requestedEvents.Count -gt 0) {
                    Write-Host "Requested Events:" -ForegroundColor Green
                    foreach ($event in $eventsStatus.requestedEvents) {
                        $eventDetails = if ($event.data) { " (data: $($event.data | ConvertTo-Json -Compress))" } else { "" }
                        Write-Host "  - $($event.name)$eventDetails" -ForegroundColor Green
                    }
                } else {
                    Write-Host "Requested Events: None" -ForegroundColor Gray
                }
                
                if ($eventsStatus.selectedEvent) {
                    $selectedDetails = if ($eventsStatus.selectedEvent.data) { " (data: $($eventsStatus.selectedEvent.data | ConvertTo-Json -Compress))" } else { "" }
                    Write-Host "Selected Event: $($eventsStatus.selectedEvent.name)$selectedDetails" -ForegroundColor Magenta
                } else {
                    Write-Host "Selected Event: None" -ForegroundColor Gray
                }
            }
        } else {
            Write-Host "✗ Step execution failed: $($stepResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ Error executing step: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} else {
    Write-Host "✗ Regular BPjs program setup failed: $($setupResponse.message)" -ForegroundColor Red
}

Write-Host "`n🎯 Regular BPjs Test Completed!" -ForegroundColor Yellow

