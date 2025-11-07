@echo off
REM Monitor all button/key events from head unit
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

echo ===================================
echo Monitoring Button Events
echo ===================================
echo.
echo Press buttons on your head unit now...
echo Events will be logged below.
echo Press Ctrl+C to stop monitoring.
echo.
echo ===================================
echo.

REM Monitor input events - this shows ALL key presses
%ADB% -s %DEVICE% shell getevent -lt

REM Alternative: Monitor only key events with readable names
REM %ADB% -s %DEVICE% shell getevent -lc
