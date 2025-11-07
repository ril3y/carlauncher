@echo off
REM Watch button event logs from launcher
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

echo ===================================
echo Watching Button Event Logs
echo ===================================
echo.
echo Press buttons on your head unit...
echo Button events will appear below.
echo Press Ctrl+C to stop.
echo.
echo ===================================
echo.

REM Clear old logs and watch for new button events
%ADB% -s %DEVICE% logcat -c
%ADB% -s %DEVICE% logcat -s MainActivity:D ButtonEvents:D
