param(
  [Parameter(Mandatory = $true)]
  [string]$Version,
  [Parameter(Mandatory = $true)]
  [string]$Message
)

Set-Location "$PSScriptRoot\.."

$DateStr = Get-Date -Format "yyyyMMdd"
$TagName = "backup/v$Version-$DateStr"

Write-Host "[backup] stage changes"
git add .

Write-Host "[backup] commit"
git commit -m "chore(release): $Message" | Out-Null

Write-Host "[backup] create tag $TagName"
git tag -a $TagName -m $Message

Write-Host "[backup] done: $TagName"
Write-Host "Next: git push origin main ; git push origin --tags"
