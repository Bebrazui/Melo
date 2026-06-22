# Creates the "playlists" and "favorites" collections (cloud library sync).
# Run:  powershell -ExecutionPolicy Bypass -File tools\appwrite_library_setup.ps1
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

# ---- Collection: playlists ----
Write-Host "Creating collection 'playlists'..." -ForegroundColor Green
# Private per-user: only users can create; each document readable/writable by its owner.
Api POST "/databases/$DbId/collections" @{
  collectionId = "playlists"
  name = "playlists"
  documentSecurity = $true
  permissions = @('create("users")')
} | Out-Null

$pp = "/databases/$DbId/collections/playlists"
Api POST "$pp/attributes/string" @{ key="ownerId"; size=64;      required=$true } | Out-Null
Api POST "$pp/attributes/string" @{ key="localId"; size=64;      required=$true } | Out-Null
Api POST "$pp/attributes/string" @{ key="name";    size=256;     required=$true } | Out-Null
Api POST "$pp/attributes/string" @{ key="tracks";  size=1000000; required=$false } | Out-Null

# ---- Collection: favorites ----
Write-Host "Creating collection 'favorites'..." -ForegroundColor Green
Api POST "/databases/$DbId/collections" @{
  collectionId = "favorites"
  name = "favorites"
  documentSecurity = $true
  permissions = @('create("users")')
} | Out-Null

$pf = "/databases/$DbId/collections/favorites"
Api POST "$pf/attributes/string" @{ key="ownerId"; size=64;      required=$true } | Out-Null
Api POST "$pf/attributes/string" @{ key="tracks";  size=1000000; required=$false } | Out-Null

Write-Host "Waiting for columns to be ready (8s)..." -ForegroundColor Green
Start-Sleep -Seconds 8

# ---- Indexes (query by owner) ----
Write-Host "Creating indexes..." -ForegroundColor Green
Api POST "$pp/indexes" @{ key="idx_owner"; type="key"; attributes=@("ownerId"); orders=@("ASC") } | Out-Null
Api POST "$pf/indexes" @{ key="idx_owner"; type="key"; attributes=@("ownerId"); orders=@("ASC") } | Out-Null

Write-Host ""
Write-Host "DONE. Collections 'playlists' and 'favorites' are ready." -ForegroundColor Cyan
