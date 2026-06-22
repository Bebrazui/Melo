# Creates columns/indexes/permissions for the "drops" table in Appwrite.
# Run:  powershell -ExecutionPolicy Bypass -File tools\appwrite_setup.ps1
# The API key is asked at runtime (not stored in this file).

param([string]$Key)

$Base = "https://fra.cloud.appwrite.io/v1"
$Proj = "6a38f49f0024c6b9b473"
$CollId = "6a38fc75002e786fae6c"

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

# Database id (known)
$DbId = "6a38fc430015b7804515"
Write-Host "Database id = $DbId" -ForegroundColor Cyan

$p = "/databases/$DbId/collections/$CollId"

# 2) Columns
Write-Host "Creating columns..." -ForegroundColor Green
Api POST "$p/attributes/float"  @{ key="lat";  required=$true } | Out-Null
Api POST "$p/attributes/float"  @{ key="lng";  required=$true } | Out-Null
Api POST "$p/attributes/string"  @{ key="title";        size=256;  required=$true } | Out-Null
Api POST "$p/attributes/string"  @{ key="artist";       size=256;  required=$false } | Out-Null
Api POST "$p/attributes/string"  @{ key="thumbnailUrl"; size=1024; required=$false } | Out-Null
Api POST "$p/attributes/string"  @{ key="sourceUrl";    size=1024; required=$true } | Out-Null
Api POST "$p/attributes/string"  @{ key="source";       size=32;   required=$false } | Out-Null
Api POST "$p/attributes/string"  @{ key="caption";      size=500;  required=$false } | Out-Null
Api POST "$p/attributes/string"  @{ key="ownerId";      size=64;   required=$true } | Out-Null
Api POST "$p/attributes/integer" @{ key="likes"; required=$false; default=0 } | Out-Null

Write-Host "Waiting for columns to be ready (8s)..." -ForegroundColor Green
Start-Sleep -Seconds 8

# 3) Indexes
Write-Host "Creating indexes..." -ForegroundColor Green
Api POST "$p/indexes" @{ key="idx_lat"; type="key"; attributes=@("lat"); orders=@("ASC") } | Out-Null
Api POST "$p/indexes" @{ key="idx_lng"; type="key"; attributes=@("lng"); orders=@("ASC") } | Out-Null

# 4) Permissions + document security
Write-Host "Setting permissions (Read=Any, Create=Users)..." -ForegroundColor Green
Api PUT "$p" @{
  name = "drops"
  documentSecurity = $true
  permissions = @('read("any")', 'create("users")')
} | Out-Null

Write-Host ""
Write-Host "DONE. Send me this value:" -ForegroundColor Cyan
Write-Host "DATABASE_ID = $DbId" -ForegroundColor White
