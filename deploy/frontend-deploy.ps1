param(
  [string]$EnvName = "prod"
)

Set-Location "$PSScriptRoot\.."

Write-Host "[frontend] install dependencies"
pnpm install

Write-Host "[frontend] build"
pnpm run build

Write-Host "[frontend] done, output directory: public"
