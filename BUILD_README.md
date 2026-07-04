# PokeClaw 自动构建指南

## 快速开始

### 方法1: 双击桌面快捷方式
- 双击桌面上的 **PokeClaw构建** 快捷方式
- 按提示操作

### 方法2: 运行批处理文件
- 双击 `构建APK.bat` 文件
- 按提示操作

### 方法3: 运行PowerShell脚本
```powershell
cd "C:\Users\Administrator\Documents\AI博主\PokeClaw"
.\build_apk.ps1
```

## 脚本功能

### build_apk.ps1
- 自动检测当前版本号
- 询问是否更新版本号（自动递增）
- 构建APK
- 复制到桌面
- 询问是否提交到Git

### 构建APK.bat
- 双击即可运行
- 自动调用build_apk.ps1

## 输出文件

APK文件会自动复制到桌面：
- `PokeClaw_v{版本号}.apk`

## 注意事项

1. 首次运行可能需要管理员权限
2. 构建过程需要1-2分钟
3. 如果构建失败，请检查错误信息
4. 提交到Git需要配置Git凭证

## 版本号规则

- 主版本.次版本.修订号
- 例如: 0.8.4
- 每次构建自动递增修订号
