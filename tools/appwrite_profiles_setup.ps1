# Creates the "profiles" collection + "avatars" storage bucket (user profiles).
# Run:  powershell -ExecutionPolicy Bypass -File tools\appwrite_profiles_setup.ps1
# The API key is asked at runtime (not stored in this file).

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

Write-Host "Database id = $DbId" -ForegroundColor Cyan

# ---- Collection: profiles ----
Write-Host "Creating collection 'profiles'..." -ForegroundColor Green
# Public read, only users create; per-document owner can edit.
Api POST "/databases/$DbId/collections" @{
  collectionId = "profiles"
  name = "profiles"
  documentSecurity = $true
  permissions = @('read("any")', 'create("users")')
} | Out-Null

$pp = "/databases/$DbId/collections/profiles"
Api POST "$pp/attributes/string" @{ key="userId";    size=64;  required=$true } | Out-Null
Api POST "$pp/attributes/string" @{ key="name";      size=128; required=$true } | Out-Null
Api POST "$pp/attributes/string" @{ key="nameLower"; size=128; required=$false } | Out-Null
Api POST "$pp/attributes/string" @{ key="avatarUrl"; size=1024; required=$false } | Out-Null
Api POST "$pp/attributes/string" @{ key="bio";       size=500; required=$false } | Out-Null

Write-Host "Waiting for columns to be ready (8s)..." -ForegroundColor Green
Start-Sleep -Seconds 8

# Fulltext index for searching people by name.
Write-Host "Creating indexes..." -ForegroundColor Green
Api POST "$pp/indexes" @{ key="idx_userid"; type="key";      attributes=@("userId") } | Out-Null
Api POST "$pp/indexes" @{ key="idx_name";   type="fulltext"; attributes=@("nameLower") } | Out-Null

# ---- Storage bucket: avatars ----
Write-Host "Creating storage bucket 'avatars'..." -ForegroundColor Green
Api POST "/storage/buckets" @{
  bucketId = "avatars"
  name = "avatars"
  fileSecurity = $false
  permissions = @('read("any")', 'create("users")')
  maximumFileSize = 5242880
  allowedFileExtensions = @("jpg", "jpeg", "png", "webp")
} | Out-Null

Write-Host ""
Write-Host "DONE. 'profiles' collection and 'avatars' bucket are ready." -ForegroundColor Cyan
