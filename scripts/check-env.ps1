$checks = @(
  @{ Name = 'Git'; Cmd = 'git --version' },
  @{ Name = 'Node'; Cmd = 'node -v' },
  @{ Name = 'pnpm'; Cmd = 'pnpm -v' },
  @{ Name = 'Java'; Cmd = 'java -version' },
  @{ Name = 'Maven'; Cmd = 'mvn -v' },
  @{ Name = 'Docker'; Cmd = 'docker --version' }
)

Write-Host "Environment check for supply-task project" -ForegroundColor Cyan
foreach ($item in $checks) {
  try {
    $output = Invoke-Expression $item.Cmd 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0) {
      Write-Host "[OK] $($item.Name)" -ForegroundColor Green
      Write-Host ($output.Trim())
    } else {
      Write-Host "[MISSING] $($item.Name)" -ForegroundColor Yellow
      Write-Host ($output.Trim())
    }
  } catch {
    Write-Host "[MISSING] $($item.Name)" -ForegroundColor Yellow
    Write-Host $_.Exception.Message
  }
  Write-Host "------------------------------"
}
