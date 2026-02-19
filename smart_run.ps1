param(
    [switch]$FullBuild = $false,
    [switch]$VerboseBuild = $false,
    [switch]$Reinstall = $false,
    [switch]$ClearData = $false,
    [string[]]$LogTag,
    [switch]$Logcat = $false,
    [string]$PhoneIP = "192.168.0.152"
)

# ================= CONFIG =================
$APP_ID        = "com.sukoon.music"
$MAIN_ACTIVITY = ".MainActivity"
$STATE_HASH_FILE  = ".last_successful_state"
$STATE_FILES_FILE = ".last_successful_files"
$env:GRADLE_OPTS = "-Dorg.gradle.logging.level=info"
$BUILD_SUCCESS_FILE = ".last_build_success"

$ProgressPreference = 'SilentlyContinue'
$ErrorActionPreference = 'Continue'

$forceFullBuild = $FullBuild.IsPresent
$verboseGradle  = $VerboseBuild.IsPresent
$forceReinstall = $Reinstall.IsPresent
$forceClearData = $ClearData.IsPresent
$attachLogcat = $Logcat.IsPresent
$device = $env:ADB_DEVICE
$LAST_DEVICE_FILE = ".last_device"

# ============== DEVICE HELPERS ==============
$scriptStart = Get-Date
Import-Module BurntToast

function Format-Duration($ts) {
    return "{0:mm\:ss\.fff}" -f $ts
}

function Load-LastDevice {
    if (Test-Path $LAST_DEVICE_FILE) {
        $line = Get-Content $LAST_DEVICE_FILE -ErrorAction SilentlyContinue
        if ($line -match "^(.*):(\d+)$") {
            return @{ ip = $matches[1]; port = $matches[2] }
        }
    }
    return $null
}

function Save-LastDevice($ip, $port) {
    "$ip`:$port" | Out-File $LAST_DEVICE_FILE -Encoding ascii -Force
}

function Get-ConnectedDevices {
  $devices = @()
    adb devices | ForEach-Object {
        # Skip header line
        if ($_ -match "^List of devices") { return }

         # Split by ANY whitespace (tab or spaces)
        $parts = ($_ -split "\s+")

        if ($parts.Count -ge 2 -and $parts[1] -eq "device") {
            $devices += $parts[0]
        }
    }

    return $devices
}

function Prompt-For-AdbTarget {
    $ip = Read-Host "Enter device IP address"
    $port = Read-Host "Enter adb port"

    if (-not $ip -or -not $port) {
        Write-Error "IP and port are required"
        exit 1
    }
    return @{ ip = $ip; port = [int]$port }
}

function Try-Adb-Connect {
    param([string]$Ip, [int]$Port)
    Write-Host "🔄 adb connect ${Ip}:${Port}" -ForegroundColor Cyan
    adb connect "${Ip}:${Port}" | Out-Null
    Start-Sleep -Seconds 2
}

function Get-Device-Status {
      $devices = adb devices |
        Where-Object { $_ -match "device$" -and $_ -notmatch "List of devices" }

    if ($devices.Count -gt 0) {
        return "ONLINE"
    }

    $offline = adb devices | Where-Object { $_ -match "offline" }
    if ($offline) { return "OFFLINE" }

    $unauth = adb devices | Where-Object { $_ -match "unauthorized" }
    if ($unauth) { return "UNAUTHORIZED" }

    return "NONE"
}

function Get-WorkingTreeSnapshot {
    git ls-files --cached --others --exclude-standard |
        Sort-Object |
        ForEach-Object {
            $file = $_
            $hash = git hash-object $file
            "$hash $file"
        }
}


# ============ WORKING TREE STATE ============
function Get-WorkingTreeHash {
     git update-index -q --refresh

    $head = git rev-parse HEAD
    $staged = git diff --cached --name-only
    $unstaged = git diff --name-only
    $untracked = git ls-files --others --exclude-standard

    $state = "$head`n$staged`n$unstaged`n$untracked"
    return $state | git hash-object --stdin
}

function Get-ChangedFilesSinceLastRun {
        if (-not (Test-Path $STATE_FILES_FILE)) {
        return @()
    }

    $old = Get-Content $STATE_FILES_FILE
    $new = Get-WorkingTreeSnapshot
	$oldFiles = $oldMap.Keys
$newFiles = $new | ForEach-Object { ($_ -split ' ',2)[1] }

foreach ($f in $oldFiles) {
    if ($f -notin $newFiles) {
        $changed += $f
    }
}

    $oldMap = @{}
    foreach ($line in $old) {
        $hash, $file = $line -split ' ', 2
        $oldMap[$file] = $hash
    }

    $changed = @()

    foreach ($line in $new) {
        $hash, $file = $line -split ' ', 2

        if (-not $oldMap.ContainsKey($file)) {
            $changed += $file
        }
        elseif ($oldMap[$file] -ne $hash) {
            $changed += $file
        }
    }

    return $changed
}

function Save-Run-State {
      $snapshot = Get-WorkingTreeSnapshot
    $hash     = $snapshot | Out-String | git hash-object --stdin
    $hash     | Out-File $STATE_HASH_FILE  -Encoding ascii
    $snapshot | Out-File $STATE_FILES_FILE -Encoding ascii
}

function Launch-App {
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell am force-stop $APP_ID
    Start-Sleep -Milliseconds 300
    adb shell am start -n "$APP_ID/$MAIN_ACTIVITY"
}

function Wait-For-AppPid {
    param (
        [int]$TimeoutSeconds = 10
    )

    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        $apppid = adb -s $ADB_DEVICE shell pidof -s $APP_ID
        if ($apppid) {
            return $apppid
        }
        Start-Sleep -Milliseconds 300
        $elapsed += 0.3
    }

    Write-Host "⚠️ App PID not found — logcat may be empty" -ForegroundColor Yellow
    return $null
}


function Get-LogTagRegex {
    if (-not $LogTag -or $LogTag.Count -eq 0) {
        return $null
    }

    # Escape tags and join with OR
    $escaped = $LogTag | ForEach-Object { [Regex]::Escape($_) }
    return ($escaped -join "|")
}

function Attach-Logcat {
    Write-Host "📜 Attaching logcat (app-only)..." -ForegroundColor Cyan
    adb logcat -c
    $application_pid = Wait-For-AppPid
    $tagRegex = Get-LogTagRegex

    if ($application_pid) {
        if ($tagRegex) {
            Write-Host "🔎 Filtering logs by tag(s): $($LogTag -join ', ')" -ForegroundColor Yellow
            adb logcat --pid=$application_pid |
                ForEach-Object {
                    if ($_ -match $tagRegex) {
                        Write-Host $_
                    }
                }
        }
        else {
            adb logcat --pid=$application_pid
        }
    }
    else {
        adb logcat
    }
}

# ================= MAIN =================

Write-Host "📱 Starting adb..." -ForegroundColor Cyan
adb start-server | Out-Null
Start-Sleep -Seconds 3

if ((Get-Device-Status) -ne "ONLINE") {
    Write-Host "⚠️ Device not connected — manual connect required" -ForegroundColor Yellow
    $cfg = Load-LastDevice

if (-not $cfg) {
    $cfg = Prompt-For-AdbTarget
}

Try-Adb-Connect $cfg.ip $cfg.port
Save-LastDevice $cfg.ip $cfg.port
}

$devices = Get-ConnectedDevices
 Write-Host " devices $devices"
if ($devices.Count -eq 0) {
    Write-Host "❌ No device connected"
    exit 1
}

if ($devices.Count -eq 1  -or ![string]::IsNullOrWhiteSpace($env:ADB_DEVICE) ) {
     if ($devices.Count -eq 1) {
		Write-Host "📱 single device detected: $devices" -ForegroundColor Yellow
        $ADB_DEVICE = [string]$devices
		$env:ADB_DEVICE = $ADB_DEVICE
    }
    else {
        $ADB_DEVICE = $env:ADB_DEVICE
    }
}
else {
    Write-Host "📱 Multiple devices detected:" -ForegroundColor Yellow
    for ($i=0; $i -lt $devices.Count; $i++) {
        Write-Host " [$i] $($devices[$i])"
    }
    $choice = Read-Host "Select device index"
    $ADB_DEVICE = $devices[[int]$choice]	
	$env:ADB_DEVICE = $ADB_DEVICE
	Write-Host "DEBUG: Saved device in session → $env:ADB_DEVICE"
}

Write-Host "📱 Using device: $env:ADB_DEVICE" -ForegroundColor Green

# ---------- FORCE OVERRIDE (EARLY EXIT GUARD) ----------
$start = Get-Date
$currentHash = Get-WorkingTreeHash
$end = Get-Date
$duration = $end - $start
Write-Host "working tree hash: $($duration.ToString("hh\:mm\:ss"))"

$start = Get-Date
if ($forceFullBuild -or $forceReinstall -or $forceClearData) {
    Write-Host "🔥 Force flag detected — skipping working tree hash check" -ForegroundColor Cyan
}
else {
	$lastBuildSucceeded = Test-Path $BUILD_SUCCESS_FILE

if (Test-Path $STATE_HASH_FILE) {
    $lastHash = Get-Content $STATE_HASH_FILE

    if ($currentHash -eq $lastHash -and $lastBuildSucceeded) {
        Write-Host "📝 No changes + last build succeeded — skipping build"
        Launch-App
        if ($attachLogcat) { Attach-Logcat }
		New-BurntToastNotification -Text "No build needed ✅"
        exit 0
    }
    elseif ($currentHash -eq $lastHash -and -not $lastBuildSucceeded) {
        Write-Host "⚠️ No changes but last build FAILED — rebuilding"
    }
}

}


$changedFiles = Get-ChangedFilesSinceLastRun
$end = Get-Date
$duration = $end - $start
Write-Host "if loop $($duration.ToString("hh\:mm\:ss"))"
# ---------- DISPLAY CHANGES ----------
$start = Get-Date
if ($changedFiles.Count -gt 0) {
    Write-Host "📝 Files changed since last successful run:" -ForegroundColor Yellow
    foreach ($f in $changedFiles) {
        Write-Host "   • $f" -ForegroundColor Gray
    }
}
else {
    Write-Host "📝 No file changes since last successful run" -ForegroundColor DarkGray
}

# ---------- CHANGE ANALYSIS ----------
$needsBuild = $false
$needsClean = $false

foreach ($file in $changedFiles) {

    if ($file -match "build.gradle$" -or
        $file -match "build.gradle.kts$" -or
        $file -match "settings.gradle" -or
        $file -match "gradle.properties") {

        $needsClean = $true
        $needsBuild = $true
        break
    }

    if ($file -match "AndroidManifest.xml" -or
        $file -match "^app/src/main/res/" -or
        $file -match "^app/src/main/java/" -or
        $file -match "^app/src/main/kotlin/" -or
        $file -match "google-services.json") {

        $needsBuild = $true
    }
}

$end = Get-Date
$duration = $end - $start
Write-Host "for loop $($duration.ToString("hh\:mm\:ss"))"
# ---------- FORCE FULL BUILD ----------
# Force Gradle to use selected device
$env:ANDROID_SERIAL = $device
if ($forceFullBuild) {
    Write-Host "🔄 FullBuild requested — ignoring cached state" -ForegroundColor Cyan
    $needsClean = $true
    $needsBuild = $true
}

# ---------- DEVICE STATE ACTIONS ----------
if ($forceReinstall) {
	 Write-Host "🗑️ Reinstall requested — attempting uninstall (non-fatal)" -ForegroundColor Yellow
	 adb uninstall $APP_ID | Out-Null
}
elseif ($forceClearData) {
    Write-Host "🧹 ClearData requested — clearing app data" -ForegroundColor Yellow
    adb shell pm clear $APP_ID | Out-Null
}

# ---------- BUILD ----------
if (Test-Path $BUILD_SUCCESS_FILE) {
    Remove-Item $BUILD_SUCCESS_FILE -Force
}
Write-Host "▶ Gradle started..." -ForegroundColor Cyan

if ($needsClean) {
    Write-Host "🧹 CLEAN build" -ForegroundColor Yellow
	$buildStart = Get-Date
    if ($verboseGradle) {
        ./gradlew clean :app:installDebug --console=plain --profile
    }
    else {
        ./gradlew clean :app:installDebug --console=plain --profile |
            ForEach-Object {
                if ($_ -match "took\s+([\d\.]+)\s+secs") {
                    Write-Host $_ -ForegroundColor Yellow
                }
            }
    }
	$buildTime = (Get-Date) - $buildStart
	Write-Host ("CLEAN & Build took: {0}" -f (Format-Duration $buildTime))
}
elseif ($needsBuild) {
    Write-Host "🚀 INCREMENTAL build" -ForegroundColor Cyan
	$buildStart = Get-Date
    if ($verboseGradle) {
        ./gradlew :app:installDebug --console=plain --profile
    }
    else {
        ./gradlew :app:installDebug --console=plain --profile |
            ForEach-Object {
                if ($_ -match "took\s+([\d\.]+)\s+secs") {
                    Write-Host $_ -ForegroundColor Yellow
                }
            }
    }
	$buildTime = (Get-Date) - $buildStart
	Write-Host ("Build took: {0}" -f (Format-Duration $buildTime))
}

if ($LASTEXITCODE -ne 0) {    
	New-BurntToastNotification -Text "Build Failed ❌", "Check logs for errors."
    exit 1
}
"OK" | Out-File $BUILD_SUCCESS_FILE -Encoding ascii

# ---------- SAVE STATE ----------
Save-Run-State
Write-Host "💾 Saved current working tree state" -ForegroundColor Green
# ---------- AUTO-LAUNCH ----------
Launch-App
Write-Host "✅ App running. Ready for testing." -ForegroundColor Green

if ($attachLogcat) {
    Write-Host "📜 Logcat enabled" -ForegroundColor Cyan
    Attach-Logcat
}
else {
    Write-Host "ℹ️ Logcat disabled (use -Logcat to enable)" -ForegroundColor DarkGray
}
$totalTime = (Get-Date) - $scriptStart

Write-Host ("⏱ Total   : {0:mm\:ss}" -f (Format-Duration  $totalTime))

if ($LASTEXITCODE -eq 0) {
    New-BurntToastNotification -Text "Build Success ✅", "Your build completed successfully."
	 [console]::beep(800,500)
} else {
    New-BurntToastNotification -Text "Build Failed ❌", "Check logs for errors."
	[console]::beep(300,900)
}

# Auto-detect (default)
#powershell -ExecutionPolicy Bypass -File smart_run.ps1

# Force full build
#powershell -ExecutionPolicy Bypass -File smart_run.ps1 -forceFullBuild true

#$pairing device
#adb pair 192.168.0.140:37573
#adb start-server
#adb kill-server
#adb connect 192.168.0.140:45679
#logs check 
#adb logcat --pid=$(adb shell pidof -s com.sukoon.music) | Select-String "ContinueListeningCard"
#.\smart_run.ps1 -Logcat -LogTag "FeedbackRepositoryImpl","Firestore"
#todos
#ensure typed ip device is conncected
#work on pairing
#adb forward tcp:5277 tcp:5277