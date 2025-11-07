@echo off
REM Quick build script for BattleWithBytes Car Launcher
echo Building BattleWithBytes Car Launcher...
call "%~dp0gradlew.bat" assembleDebug
if %errorlevel% equ 0 (
    echo.
    echo ===================================
    echo Build SUCCESS!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
    echo ===================================
) else (
    echo.
    echo ===================================
    echo Build FAILED!
    echo ===================================
)
