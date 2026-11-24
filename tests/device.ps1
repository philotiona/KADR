$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
Push-Location $projectRoot
try {
    if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -PlegacySmoke --no-daemon --max-workers=2
    if ($LASTEXITCODE -ne 0) { throw 'Smoke APK build failed' }
    & $adb install -r 'app\build\outputs\apk\debug\app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'App install failed; start an emulator first' }
    & $adb install -r -t 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Test APK install failed' }
    $result = & $adb shell am instrument -w 'com.example.kaspikids.test/com.example.kaspikids.CinemaSmokeTest'
    $result | Write-Output
    if ($result -notmatch 'PASS:') { throw 'Device verification failed' }
    New-Item -ItemType Directory -Force -Path 'output\screenshots' | Out-Null
    $images = @('01-login','02-home','03-movie','04-seats','05-ticket','06-profile','07-admin','08-schedule','09-registration','sample-ticket')
    foreach ($image in $images) {
        & $adb pull "/sdcard/Android/data/com.example.kaspikids.debug/files/$image.png" "output/screenshots/$image.png"
    }
} finally { Pop-Location }
