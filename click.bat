@echo off
REM Send click/touch event to head unit
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

if "%~1"=="" (
    echo Usage: click.bat X Y
    echo Example: click.bat 500 300
    echo.
    echo This will send a touch event at coordinates X=500, Y=300
    exit /b 1
)

if "%~2"=="" (
    echo Usage: click.bat X Y
    echo Example: click.bat 500 300
    exit /b 1
)

set "X=%~1"
set "Y=%~2"

echo Sending click at coordinates: X=%X%, Y=%Y%
%ADB% -s %DEVICE% shell input tap %X% %Y%

if %errorlevel% equ 0 (
    echo Click sent successfully!
) else (
    echo Failed to send click!
)
