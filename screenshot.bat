@echo off
REM Capture screenshot from head unit
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

REM Generate timestamp for filename
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "timestamp=%dt:~0,8%_%dt:~8,6%"

set "FILENAME=screenshot_%timestamp%.png"
set "DEVICE_PATH=/sdcard/%FILENAME%"
set "LOCAL_PATH=screenshots\%FILENAME%"

echo Capturing screenshot from head unit...

REM Create screenshots directory if it doesn't exist
if not exist "screenshots" mkdir screenshots

REM Capture screenshot on device
%ADB% -s %DEVICE% shell screencap -p %DEVICE_PATH%

REM Pull screenshot to local machine
%ADB% -s %DEVICE% pull %DEVICE_PATH% %LOCAL_PATH%

REM Delete screenshot from device
%ADB% -s %DEVICE% shell rm %DEVICE_PATH%

if exist %LOCAL_PATH% (
    echo.
    echo ===================================
    echo Screenshot saved to: %LOCAL_PATH%
    echo ===================================
    start %LOCAL_PATH%
) else (
    echo Failed to capture screenshot!
)
