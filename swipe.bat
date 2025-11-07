@echo off
REM Send swipe gesture to head unit
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

if "%~4"=="" (
    echo Usage: swipe.bat X1 Y1 X2 Y2 [DURATION_MS]
    echo Example: swipe.bat 100 500 900 500 300
    echo.
    echo This will swipe from (X1,Y1) to (X2,Y2) over DURATION_MS milliseconds
    echo Default duration is 300ms if not specified
    exit /b 1
)

set "X1=%~1"
set "Y1=%~2"
set "X2=%~3"
set "Y2=%~4"
set "DURATION=%~5"

if "%DURATION%"=="" set "DURATION=300"

echo Sending swipe from (%X1%,%Y1%) to (%X2%,%Y2%) over %DURATION%ms
%ADB% -s %DEVICE% shell input swipe %X1% %Y1% %X2% %Y2% %DURATION%

if %errorlevel% equ 0 (
    echo Swipe sent successfully!
) else (
    echo Failed to send swipe!
)
