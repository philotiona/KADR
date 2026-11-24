$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$testOutput = Join-Path $projectRoot 'build\cinema-tests'
New-Item -ItemType Directory -Force $testOutput | Out-Null
$sources = @(
    (Join-Path $projectRoot 'app\src\main\java\com\example\kaspikids\CinemaRules.java'),
    (Join-Path $projectRoot 'app\src\main\java\com\example\kaspikids\TicketQr.java'),
    (Join-Path $projectRoot 'app\src\main\java\com\example\kaspikids\CalendarFile.java'),
    (Join-Path $PSScriptRoot 'CinemaRulesTest.java')
)
& (Join-Path $jdk 'bin\javac.exe') -encoding UTF-8 -d $testOutput $sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& (Join-Path $jdk 'bin\java.exe') -cp $testOutput com.example.kaspikids.CinemaRulesTest (Join-Path $testOutput 'qr-matrix.txt')
if ($LASTEXITCODE -ne 0) { throw 'Logic tests failed' }

