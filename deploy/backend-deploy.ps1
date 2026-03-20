param(
  [string]$EnvName = "prod"
)

Set-Location "$PSScriptRoot\..\server\supply-task-service"

Write-Host "[backend] package"
mvn clean package -DskipTests

Write-Host "[backend] run"
java -jar target\supply-task-service-1.0.0.jar --spring.profiles.active=$EnvName
