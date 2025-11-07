@echo off
REM Install to connected device
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"

echo Installing BattleWithBytes Car Launcher to device...
%ADB% -s 192.168.1.119:5555 install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% equ 0 (
    echo.
    echo ===================================
    echo Install SUCCESS!
    echo ===================================
) else (
    echo.
    echo ===================================
    echo Install FAILED!
    echo ===================================
)
