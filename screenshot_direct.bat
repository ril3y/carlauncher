@echo off
REM Direct ADB screenshot (faster, no device storage needed)
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

REM Generate timestamp
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "timestamp=%dt:~0,8%_%dt:~8,6%"
set "FILENAME=screenshot_%timestamp%.png"

REM Create screenshots directory if needed
if not exist "screenshots" mkdir screenshots

echo Capturing screenshot from head unit (direct method)...

REM Capture directly without device storage
%ADB% -s %DEVICE% exec-out screencap -p > "screenshots\%FILENAME%"

if exist "screenshots\%FILENAME%" (
    echo.
    echo ===================================
    echo Screenshot saved to: screenshots\%FILENAME%
    echo ===================================
) else (
    echo Failed to capture screenshot!
)
