# Adds privacy / role flags:
#   playlists.isPublic (bool, default true), favorites.isPublic (bool, default true),
#   profiles.isDeveloper (bool, default false), profiles.isVerified (bool, default false).
# Run:  powershell -ExecutionPolicy Bypass -File tools\appwrite_privacy_setup.ps1

param([string]$Key)

$Base = "https://fra.cloud.appwrite.io/v1"
$Proj = "6a38f49f0024c6b9b473"
$DbId = "6a38fc430015b7804515"

if (-not $Key) { $Key = Read-Host "Paste API Key (secret)" }
$H = @{ "X-Appwrite-Project" = $Proj; "X-Appwrite-Key" = $Key; "Content-Type" = "application/json" }

function Api($method, $path, $body) {
  $uri = "$Base$path"
  try {
    if ($body) { return Invoke-RestMethod -Method $method -Uri $uri -Headers $H -Body ($body | ConvertTo-Json -Depth 6) }
    else { return Invoke-RestMethod -Method $method -Uri $uri -Headers $H }
  } catch {
    Write-Host ("  ! " + $_.Exception.Message) -ForegroundColor Yellow
    return $null
  }
}

Write-Host "Adding boolean flags..." -ForegroundColor Green
Api POST "/databases/$DbId/collections/playlists/attributes/boolean" @{ key="isPublic";    required=$false; default=$true } | Out-Null
Api POST "/databases/$DbId/collections/favorites/attributes/boolean" @{ key="isPublic";    required=$false; default=$true } | Out-Null
Api POST "/databases/$DbId/collections/profiles/attributes/boolean"  @{ key="isDeveloper"; required=$false; default=$false } | Out-Null
Api POST "/databases/$DbId/collections/profiles/attributes/boolean"  @{ key="isVerified";  required=$false; default=$false } | Out-Null

Write-Host ""
Write-Host "DONE. Privacy/role flags added." -ForegroundColor Cyan
