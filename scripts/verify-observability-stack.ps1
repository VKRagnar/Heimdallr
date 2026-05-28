param(
    [string]$PrometheusBaseUrl = "http://192.168.3.2:9090",
    [string]$BackendBaseUrl = "http://192.168.3.11:8080",
    [string]$ExpectedTarget = "192.168.3.11:8080",
    [string]$ExpectedJob = "heimdallr-api-local"
)

$ErrorActionPreference = "Stop"

function Write-Check {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )

    $status = if ($Passed) { "OK" } else { "FAIL" }
    Write-Host ("[{0}] {1} - {2}" -f $status, $Name, $Detail)
}

function Invoke-HealthCheck {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    try {
        $result = & $Action
        Write-Check -Name $Name -Passed $true -Detail $result
        return $true
    }
    catch {
        Write-Check -Name $Name -Passed $false -Detail $_.Exception.Message
        return $false
    }
}

$allPassed = $true

$allPassed = (Invoke-HealthCheck "Backend health" {
    $health = Invoke-RestMethod -TimeoutSec 5 "$BackendBaseUrl/health"
    "status=$($health.data.status)"
}) -and $allPassed

$allPassed = (Invoke-HealthCheck "Backend Prometheus endpoint" {
    $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 "$BackendBaseUrl/actuator/prometheus"
    "http=$($response.StatusCode)"
}) -and $allPassed

$allPassed = (Invoke-HealthCheck "Prometheus readiness" {
    $ready = Invoke-RestMethod -TimeoutSec 5 "$PrometheusBaseUrl/-/ready"
    "$ready"
}) -and $allPassed

$activeTargets = $null
$allPassed = (Invoke-HealthCheck "Prometheus active targets" {
    $targets = Invoke-RestMethod -TimeoutSec 5 "$PrometheusBaseUrl/api/v1/targets?state=active"
    $script:activeTargets = $targets.data.activeTargets
    "count=$($script:activeTargets.Count)"
}) -and $allPassed

if ($null -ne $activeTargets) {
    $target = $activeTargets | Where-Object { $_.scrapePool -eq $ExpectedJob } | Select-Object -First 1
    if ($null -eq $target) {
        Write-Check -Name "Expected target $ExpectedJob" -Passed $false -Detail "not found"
        $allPassed = $false
    }
    else {
        $hasExpectedTarget = $target.scrapeUrl -like "*$ExpectedTarget*"
        Write-Check -Name "Expected scrape URL" -Passed $hasExpectedTarget -Detail $target.scrapeUrl
        $allPassed = $hasExpectedTarget -and $allPassed

        $isUp = $target.health -eq "up"
        $targetDetail = if ($isUp) { "health=up" } else { "health=$($target.health), error=$($target.lastError)" }
        Write-Check -Name "Expected target health" -Passed $isUp -Detail $targetDetail
        $allPassed = $isUp -and $allPassed
    }
}

if (-not $allPassed) {
    exit 1
}
