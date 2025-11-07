@echo off
REM Monitor button events with readable key names
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

echo ===================================
echo Monitoring Button Events (Readable)
echo ===================================
echo.
echo Press buttons on your head unit now...
echo Logged to: button_events.log
echo Press Ctrl+C to stop monitoring.
echo.
echo ===================================
echo.

REM Monitor and log events
%ADB% -s %DEVICE% shell "getevent -lc | grep 'EV_KEY'" | tee button_events.log
