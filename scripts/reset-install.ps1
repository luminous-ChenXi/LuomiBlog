# 重新安装脚本
# 使用方法: 在 PowerShell 中运行 .\scripts\reset-install.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  LuomiBlog 重新安装工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查后端目录
$backendDir = "..\luomiblog-backend"
if (-not (Test-Path $backendDir)) {
    $backendDir = ".\luomiblog-backend"
}

if (-not (Test-Path $backendDir)) {
    Write-Host "错误: 找不到后端目录" -ForegroundColor Red
    exit 1
}

# 1. 删除安装锁文件
$lockFile = "$backendDir\install.lock"
if (Test-Path $lockFile) {
    Remove-Item $lockFile -Force
    Write-Host "✓ 已删除安装锁文件" -ForegroundColor Green
} else {
    Write-Host "○ 安装锁文件不存在" -ForegroundColor Yellow
}

# 2. 删除自定义配置文件
$configFile = "$backendDir\config\custom-application.yml"
if (Test-Path $configFile) {
    Remove-Item $configFile -Force
    Write-Host "✓ 已删除自定义配置文件" -ForegroundColor Green
} else {
    Write-Host "○ 自定义配置文件不存在" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  重置完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "请手动完成以下步骤:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 清空数据库:" -ForegroundColor White
Write-Host "   - 删除 luomiblog 数据库中的所有表" -ForegroundColor Gray
Write-Host "   - 或删除整个数据库后重新创建" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 重启后端服务" -ForegroundColor White
Write-Host ""
Write-Host "3. 访问安装页面:" -ForegroundColor White
Write-Host "   http://localhost:4321/install" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
