$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$target = Join-Path $root "external\androidx-media"
$repo = "https://github.com/androidx/media.git"
$zip = Join-Path $env:TEMP "androidx-media-release.zip"

New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null

if (Test-Path $target) {
    Write-Host "AndroidX Media already exists at $target"
    exit 0
}

if (Get-Command git -ErrorAction SilentlyContinue) {
    git clone --depth 1 $repo $target
    exit 0
}

Invoke-WebRequest -Uri "https://github.com/androidx/media/archive/refs/heads/release.zip" -OutFile $zip
Expand-Archive -LiteralPath $zip -DestinationPath (Split-Path $target) -Force
$expanded = Get-ChildItem -Path (Split-Path $target) -Directory | Where-Object { $_.Name -like "media-*" } | Select-Object -First 1
if ($expanded) {
    Rename-Item -LiteralPath $expanded.FullName -NewName "androidx-media"
}
Remove-Item -LiteralPath $zip -Force
