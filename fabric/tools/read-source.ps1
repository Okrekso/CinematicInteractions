param([string]$Version = '1.21.1', [string[]]$Names)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$cache = Join-Path $PSScriptRoot '../.gradle/loom-cache/minecraftMaven'
$sourceJar = Get-ChildItem $cache -Recurse -Filter '*sources.jar' | Where-Object { $_.FullName -like "*-$Version-*" } | Select-Object -First 1
if (!$sourceJar) { throw "Run genSources for $Version first" }
$zip = [System.IO.Compression.ZipFile]::OpenRead($sourceJar.FullName)
try {
    foreach ($name in $Names) {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $name -or $_.FullName.EndsWith("/$name.java") } | Select-Object -First 1
        if (!$entry) { Write-Output "Missing source: $name"; continue }
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try { Write-Output $reader.ReadToEnd() } finally { $reader.Dispose() }
    }
} finally { $zip.Dispose() }
