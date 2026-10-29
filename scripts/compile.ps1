$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not $env:JAVA_HOME) {
    if (Test-Path 'C:\Program Files\Java\jdk-26.0.2') {
        $env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2'
    } elseif ($jdk = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1) {
        $env:JAVA_HOME = $jdk.FullName
    } elseif ($javacCmd = Get-Command javac -ErrorAction SilentlyContinue) {
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javacCmd.Source)
    } else {
        throw 'Set JAVA_HOME to your JDK folder, for example C:\Program Files\Java\jdk-26.0.2.'
    }
}

New-Item -ItemType Directory -Force 'out\main', 'out\test' | Out-Null
$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'

$libJars = @(Get-ChildItem -Path 'lib' -Filter '*.jar' -ErrorAction SilentlyContinue | ForEach-Object FullName)
$extraCp = if ($libJars.Count -gt 0) { $libJars -join ';' } else { '' }

$mainSources = Get-ChildItem -Recurse 'src\main\java' -Filter '*.java' | ForEach-Object FullName
if ($mainSources) {
    if ($extraCp) {
        & $javac --release 17 -cp $extraCp -d out\main $mainSources
    } else {
        & $javac --release 17 -d out\main $mainSources
    }
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$testSources = Get-ChildItem -Recurse 'src\test\java' -Filter '*.java' | ForEach-Object FullName
if ($testSources) {
    $testCp = if ($extraCp) { "out\main;$extraCp" } else { "out\main" }
    & $javac --release 17 -cp $testCp -d out\test $testSources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
