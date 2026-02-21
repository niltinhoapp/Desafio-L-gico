$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$ts = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$outDir = Join-Path $root ("reports\audit_" + $ts)
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Run-Step([string]$name, [string]$cmd) {
  Write-Host ""
  Write-Host "==== $name ===="
  $log = Join-Path $outDir ($name + ".log")
  cmd /c $cmd 2>&1 | Tee-Object -FilePath $log
  if ($LASTEXITCODE -ne 0) { throw "Falhou: $name (exit=$LASTEXITCODE)" }
}

# --- Build / Lint / Tests ---
Run-Step "01_clean"         "gradlew.bat :app:clean --no-daemon"
Run-Step "02_assembleDebug" "gradlew.bat :app:assembleDebug --no-daemon"
Run-Step "03_lintDebug"     "gradlew.bat :app:lintDebug --no-daemon"
Run-Step "04_unitTests"     "gradlew.bat :app:testDebugUnitTest --no-daemon"
Run-Step "05_dependencies"  "gradlew.bat :app:dependencies --configuration debugRuntimeClasspath --no-daemon"

# (Opcional) já pega problemas de release antes da Play
Run-Step "06_assembleRelease" "gradlew.bat :app:assembleRelease --no-daemon"
Run-Step "07_lintRelease"     "gradlew.bat :app:lintRelease --no-daemon"

# --- Copiar relatórios principais (se existirem) ---
$lintHtmlDebug = Join-Path $root "app\build\reports\lint-results-debug.html"
if (Test-Path $lintHtmlDebug) { Copy-Item $lintHtmlDebug (Join-Path $outDir "lint-results-debug.html") -Force }

$lintHtmlRelease = Join-Path $root "app\build\reports\lint-results-release.html"
if (Test-Path $lintHtmlRelease) { Copy-Item $lintHtmlRelease (Join-Path $outDir "lint-results-release.html") -Force }

$testsHtml = Join-Path $root "app\build\reports\tests\testDebugUnitTest\index.html"
if (Test-Path $testsHtml) { Copy-Item $testsHtml (Join-Path $outDir "unit-tests-index.html") -Force }

# APKs
$apkDebugDir = Join-Path $root "app\build\outputs\apk\debug"
$apkDebug = Get-ChildItem -Path $apkDebugDir -Filter "*.apk" -ErrorAction SilentlyContinue |
  Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($apkDebug) { Copy-Item $apkDebug.FullName (Join-Path $outDir $apkDebug.Name) -Force }

$apkRelDir = Join-Path $root "app\build\outputs\apk\release"
$apkRel = Get-ChildItem -Path $apkRelDir -Filter "*.apk" -ErrorAction SilentlyContinue |
  Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($apkRel) { Copy-Item $apkRel.FullName (Join-Path $outDir $apkRel.Name) -Force }

# --- Resumo final ---
$summary = @()
$summary += "AUDIT OK - $ts"
$summary += "Pasta: $outDir"

if (Test-Path (Join-Path $outDir "lint-results-debug.html")) { $summary += "Lint(Debug): lint-results-debug.html" } else { $summary += "Lint(Debug): (não gerou html)" }
if (Test-Path (Join-Path $outDir "lint-results-release.html")) { $summary += "Lint(Release): lint-results-release.html" } else { $summary += "Lint(Release): (não gerou html)" }
if (Test-Path (Join-Path $outDir "unit-tests-index.html")) { $summary += "Tests: unit-tests-index.html" } else { $summary += "Tests: (não gerou html)" }

if ($apkDebug) { $summary += ("APK Debug: " + $apkDebug.Name) } else { $summary += "APK Debug: (não encontrado)" }
if ($apkRel)   { $summary += ("APK Release: " + $apkRel.Name) } else { $summary += "APK Release: (não encontrado)" }

$summary | Out-File (Join-Path $outDir "SUMMARY.txt") -Encoding UTF8

Write-Host ""
Write-Host "✅ AUDIT FINALIZADO"
Write-Host ("-> " + (Join-Path $outDir "SUMMARY.txt"))
