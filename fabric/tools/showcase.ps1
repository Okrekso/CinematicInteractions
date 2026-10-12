param(
    [ValidateSet('1.21','1.21.1','1.21.2','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7','1.21.8','1.21.9','1.21.10','1.21.11')][string]$Version = '1.21.1',
    [ValidatePattern('^[03456agsclmbrfukenhotyvjqw]$')][string]$From = '0',
    [switch]$Only,
    [switch]$WithSettings,
    [switch]$WithShaders
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$null = New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'build')
$logPath = Join-Path $projectRoot "build/showcase-$Version-$From$(if ($WithShaders) { '-iris' }).log"
$gradleArgs = @('--project-cache-dir', ".gradle/targets/$Version", "-Pmc_version=$Version", "-Pfrom=$From", "-Ponly=$($Only.IsPresent.ToString().ToLowerInvariant())")
if ($WithSettings) { $gradleArgs += '-PwithSettings=true' }
if ($WithShaders) {
    $modPath = Join-Path $projectRoot "test-mods/$Version"
    $iris = Get-ChildItem -LiteralPath $modPath -Filter 'iris*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
    $sodium = Get-ChildItem -LiteralPath $modPath -Filter 'sodium*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$iris -or !$sodium) { throw "Place matching Fabric Iris and Sodium JARs in $modPath first." }
    # Loom's development launch does not automatically expose Iris's nested Java libraries.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $libraryPath = Join-Path $modPath 'libraries'
    $null = New-Item -ItemType Directory -Force -Path $libraryPath
    $zip = [System.IO.Compression.ZipFile]::OpenRead($iris.FullName)
    try {
        foreach ($entry in ($zip.Entries | Where-Object { $_.FullName -match '^META-INF/jars/(antlr4|glsl-transformer|jcpp).*\.jar$' })) {
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $libraryPath $entry.Name), $true)
        }
    } finally { $zip.Dispose() }
    $irisConfig = Join-Path $projectRoot "run/$Version/showcase-iris/config/iris.properties"
    if (!(Test-Path -LiteralPath $irisConfig) -or !(Select-String -LiteralPath $irisConfig -Pattern '^enableShaders=true$' -Quiet)) {
        throw "Enable a shader pack in $irisConfig first."
    }
    $gradleArgs += @('-PwithShaders=true', '-PshowcaseWidth=1280', '-PshowcaseHeight=720')
}
$gradleArgs += 'runShowcase'
Push-Location $projectRoot
try {
    # cmd does the redirection: in PowerShell 5.1 a javac "Note:" on stderr would become a terminating error
    # and cut the log while the game keeps running; this way the log is also written as the game goes
    $argLine = ($gradleArgs | ForEach-Object { '"' + $_ + '"' }) -join ' '
    cmd /c "cd /d `"$projectRoot`" && .\gradlew.bat $argLine > `"$logPath`" 2>&1"
    if ($LASTEXITCODE -ne 0) { throw "Game run failed. See $logPath" }
    $failures = Select-String -LiteralPath $logPath -Pattern '\[Showcase\] check FAILED'
    if ($failures) { $failures | ForEach-Object { Write-Output $_.Line }; throw "Showcase checks failed. See $logPath" }
    Write-Output "Showcase passed: Minecraft $Version, act $From. Log: $logPath"
} finally { Pop-Location }
