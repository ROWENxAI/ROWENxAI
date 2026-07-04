# PokeClaw 自动构建脚本
# 用法: .\build_apk.ps1

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  PokeClaw 自动构建脚本" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 设置工作目录
Set-Location "C:\Users\Administrator\Documents\AI博主\PokeClaw"

# 获取当前版本号
$gradleContent = Get-Content "app\build.gradle.kts" -Raw
$versionMatch = [regex]::Match($gradleContent, 'readLocalOrEnvString\("POKECLAW_VERSION_NAME", "([^"]+)"\)')
$currentVersion = $versionMatch.Groups[1].Value
Write-Host "当前版本: v$currentVersion" -ForegroundColor Yellow

# 询问是否更新版本号
$updateVersion = Read-Host "是否更新版本号? (y/N)"
if ($updateVersion -eq 'y' -or $updateVersion -eq 'Y') {
    # 自动递增版本号
    $parts = $currentVersion.Split('.')
    $parts[2] = [int]$parts[2] + 1
    $newVersion = $parts -join '.'
    
    # 更新版本号
    $gradleContent = $gradleContent -replace "readLocalOrEnvString\(`"POKECLAW_VERSION_NAME`", `"$currentVersion`"\)", "readLocalOrEnvString(`"POKECLAW_VERSION_NAME`", `"$newVersion`")"
    
    # 更新versionCode
    $versionCodeMatch = [regex]::Match($gradleContent, 'readLocalOrEnvInt\("POKECLAW_VERSION_CODE", (\d+)\)')
    $currentCode = [int]$versionCodeMatch.Groups[1].Value
    $newCode = $currentCode + 1
    $gradleContent = $gradleContent -replace "readLocalOrEnvInt\(`"POKECLAW_VERSION_CODE`", $currentCode\)", "readLocalOrEnvInt(`"POKECLAW_VERSION_CODE`", $newCode)"
    
    Set-Content "app\build.gradle.kts" -Value $gradleContent -Encoding UTF8
    Write-Host "版本号已更新: v$currentVersion -> v$newVersion (code: $currentCode -> $newCode)" -ForegroundColor Green
    $currentVersion = $newVersion
}

Write-Host ""
Write-Host "开始构建APK..." -ForegroundColor Cyan
Write-Host ""

# 构建APK
$buildResult = & .\gradlew assembleDebug 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "构建失败!" -ForegroundColor Red
    Write-Host $buildResult | Select-String -Pattern "Error|FAILURE" | Select-Object -First 5
    exit 1
}

# 获取APK文件
$apk = Get-ChildItem -Path "app\build\outputs\apk\debug" -Filter "*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$apkSize = [math]::Round($apk.Length / 1MB, 2)

# 复制到桌面
$desktopPath = "C:\Users\Administrator\Desktop"
$destPath = Join-Path $desktopPath "PokeClaw_v$currentVersion.apk"
Copy-Item $apk.FullName $destPath -Force

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  构建成功!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "APK信息:" -ForegroundColor Yellow
Write-Host "  版本: v$currentVersion" -ForegroundColor White
Write-Host "  大小: $apkSize MB" -ForegroundColor White
Write-Host "  位置: $destPath" -ForegroundColor White
Write-Host ""

# 询问是否提交到Git
$commit = Read-Host "是否提交到Git? (y/N)"
if ($commit -eq 'y' -or $commit -eq 'Y') {
    git add -A
    $commitMsg = Read-Host "输入提交信息 (直接回车使用默认信息)"
    if ([string]::IsNullOrEmpty($commitMsg)) {
        $commitMsg = "bump: v$currentVersion"
    }
    git commit -m $commitMsg
    git push origin main
    Write-Host "已提交并推送到GitHub" -ForegroundColor Green
}

Write-Host ""
Write-Host "完成!" -ForegroundColor Cyan
