param([string]$Serial)

$ErrorActionPreference = 'Stop'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$target = if ($Serial) { @('-s', $Serial) } else { @() }
$fixture = Join-Path $PSScriptRoot 'lab7-fixtures'

& $adb @target reverse tcp:8765 tcp:8765
if ($LASTEXITCODE -ne 0) { throw 'ADB reverse failed. Start an emulator or pass -Serial for a connected device.' }
try {
    python -m http.server 8765 --bind 127.0.0.1 --directory $fixture
} finally {
    & $adb @target reverse --remove tcp:8765
}
