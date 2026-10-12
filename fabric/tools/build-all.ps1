param([string[]]$Versions = @('1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10', '1.21.11'))
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$releasePath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot '../release/fabric'))
$null = New-Item -ItemType Directory -Force -Path $releasePath
Add-Type -AssemblyName System.IO.Compression.FileSystem
$modVersion = ((Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties')) -match '^mod_version=' | Select-Object -First 1).Split('=')[1].Trim()
Push-Location $projectRoot
try {
    foreach ($gameVersion in $Versions) {
        if ($gameVersion -notin @('1.21','1.21.1','1.21.2','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7','1.21.8','1.21.9','1.21.10','1.21.11')) { throw "Unsupported Minecraft version: $gameVersion" }
        # through cmd: a javac "Note:" on stderr must not stop the script (PowerShell 5.1 treats it as an error)
        cmd /c "cd /d `"$projectRoot`" && .\gradlew.bat --project-cache-dir `".gradle/targets/$gameVersion`" `"-Pmc_version=$gameVersion`" build 2>&1"
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $gameVersion" }
        # the libs folder keeps older versions too: take exactly this build's game JAR
        $jar = Get-Item -LiteralPath "build/$gameVersion/libs/Cinematic Interaction-Fabric-$gameVersion-$modVersion.jar" -ErrorAction SilentlyContinue
        if (!$jar) { throw "Missing game JAR: $gameVersion" }
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
        try {
            $entry = $zip.GetEntry('fabric.mod.json')
            if (!$entry) { throw "Missing Fabric metadata: $gameVersion" }
            $reader = [System.IO.StreamReader]::new($entry.Open())
            try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            if ($metadata.depends.minecraft -ne $gameVersion -or $metadata.id -ne 'cinematic') { throw "Wrong metadata: $gameVersion" }
            if ($zip.GetEntry('META-INF/neoforge.mods.toml')) { throw "Unexpected NeoForge metadata: $gameVersion" }
            if (!$zip.GetEntry($metadata.accessWidener) -or !$zip.GetEntry($metadata.mixins[0])) { throw "Missing runtime configuration: $gameVersion" }
        } finally { $zip.Dispose() }
        Copy-Item -LiteralPath $jar.FullName -Destination $releasePath -Force
    }
    $hashes = Get-ChildItem -LiteralPath $releasePath -Filter '*.jar' | Sort-Object Name | ForEach-Object {
        $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $($_.Name)"
    }
    [System.IO.File]::WriteAllLines((Join-Path $releasePath 'SHA256SUMS.txt'), $hashes, [System.Text.UTF8Encoding]::new($false))
} finally { Pop-Location }
