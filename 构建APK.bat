@echo off
chcp 65001 >nul
cd /d "C:\Users\Administrator\Documents\AI??\PokeClaw"
powershell -ExecutionPolicy Bypass -File build_apk.ps1
pause
