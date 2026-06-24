# Creates the "reports" collection (UGC moderation: reports on map pins).
# Run:  powershell -ExecutionPolicy Bypass -File tools\appwrite_reports_setup.ps1

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

Write-Host "Creating collection 'reports'..." -ForegroundColor Green
# Любой залогиненный может создать жалобу; читать/модерировать — через консоль (API key).
Api POST "/databases/$DbId/collections" @{
  collectionId = "reports"
  name = "reports"
  documentSecurity = $true
  permissions = @('create("users")')
} | Out-Null

$p = "/databases/$DbId/collections/reports"
Api POST "$p/attributes/string" @{ key="reporterId"; size=64;   required=$true } | Out-Null
Api POST "$p/attributes/string" @{ key="targetType"; size=16;   required=$true } | Out-Null
Api POST "$p/attributes/string" @{ key="targetId";   size=64;   required=$true } | Out-Null
Api POST "$p/attributes/string" @{ key="reason";     size=64;   required=$false } | Out-Null
Api POST "$p/attributes/string" @{ key="title";      size=256;  required=$false } | Out-Null
Api POST "$p/attributes/string" @{ key="sourceUrl";  size=1024; required=$false } | Out-Null

Write-Host "Waiting for columns (8s)..." -ForegroundColor Green
Start-Sleep -Seconds 8
Api POST "$p/indexes" @{ key="idx_target"; type="key"; attributes=@("targetId"); orders=@("ASC") } | Out-Null

Write-Host ""
Write-Host "DONE. Collection 'reports' is ready." -ForegroundColor Cyan
