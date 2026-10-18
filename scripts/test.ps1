$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

& "$PSScriptRoot\compile.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $env:JAVA_HOME) {
    if (Test-Path 'C:\Program Files\Java\jdk-26.0.2') {
        $env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2'
    } elseif ($jdk = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1) {
        $env:JAVA_HOME = $jdk.FullName
    } elseif ($javaCmd = Get-Command java -ErrorAction SilentlyContinue) {
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaCmd.Source)
    } else {
        throw 'Set JAVA_HOME to your JDK folder, for example C:\Program Files\Java\jdk-26.0.2.'
    }
}

$libJars = @(Get-ChildItem -Path 'lib' -Filter '*.jar' -ErrorAction SilentlyContinue | ForEach-Object FullName)
$extraCp = if ($libJars.Count -gt 0) { ($libJars -join ';') + ';' } else { '' }

$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
& $java --enable-native-access=ALL-UNNAMED -cp "${extraCp}out\main;out\test" com.peershare.AllTests
exit $LASTEXITCODE
