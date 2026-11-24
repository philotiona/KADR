$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbPath = Join-Path $sdkRoot 'platform-tools\adb.exe'
$buildRoot = Join-Path $projectRoot 'output\database-build'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -PlegacySmoke --no-daemon --max-workers=2 '-Pandroid.injected.testOnly=false' "-Pandroid.injected.apk.location=$buildRoot"
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    & $adbPath install -r "$buildRoot\debug\app-debug.apk"
    if ($LASTEXITCODE -ne 0) { throw 'App install failed; start an emulator first' }
    & $adbPath install -r -t "$buildRoot\androidTest\debug\app-debug-androidTest.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Test APK install failed' }
    $result = & $adbPath shell am instrument -w -e databaseOnly true 'com.example.kaspikids.test/com.example.kaspikids.CinemaSmokeTest'
    $result | Tee-Object -FilePath 'build\database-explorer-tests.log'
    if (-not ($result -match '^PASS:') -or ($result -match '^FAIL:')) { throw 'Database verification failed' }
    New-Item -ItemType Directory -Force -Path 'output\screenshots' | Out-Null
    foreach ($image in @('10-database-er','11-database-zoom','12-database-rows','13-database-checks','14-database-viewer')) {
        & $adbPath pull "/sdcard/Android/data/com.example.kaspikids.debug/files/$image.png" "output/screenshots/$image.png"
    }
    Copy-Item -LiteralPath "$buildRoot\debug\app-debug.apk" -Destination 'output\KADR.apk' -Force
} finally { Pop-Location }
