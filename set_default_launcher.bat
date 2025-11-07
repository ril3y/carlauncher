@echo off
REM Set BattleWithBytes Launcher as default HOME launcher
set ADB="C:\Users\riley\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set DEVICE=192.168.1.119:5555

echo ===================================
echo Setting BattleWithBytes as Default Launcher
echo ===================================
echo.

echo Step 1: Clearing current default launcher...
%ADB% -s %DEVICE% shell pm clear-default-home

echo.
echo Step 2: Simulating HOME button press...
echo This will show the launcher picker dialog.
echo.
echo PLEASE SELECT "BattleWithBytes Launcher" on your head unit
echo and tap "ALWAYS" to set it as default!
echo.
pause

%ADB% -s %DEVICE% shell input keyevent KEYCODE_HOME

echo.
echo ===================================
echo If you selected "ALWAYS", the launcher is now default!
echo Press HOME button to return to it anytime.
echo ===================================
