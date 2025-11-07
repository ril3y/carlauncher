@echo off
REM Build and install in one command
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"

echo ===================================
echo Building and Deploying to Device
echo ===================================
echo.

echo [1/2] Building APK...
call "%~dp0gradlew.bat" assembleDebug
if %errorlevel% neq 0 (
    echo Build FAILED!
    exit /b 1
)

echo.
echo [2/2] Installing to device...
%ADB% -s 192.168.1.119:5555 install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% equ 0 (
    echo.
    echo ===================================
    echo Deploy SUCCESS!
    echo Launching app...
    echo ===================================
    %ADB% -s 192.168.1.119:5555 shell am start -n io.battlewithbytes.carlauncher/.MainActivity
) else (
    echo Deploy FAILED!
    exit /b 1
)
