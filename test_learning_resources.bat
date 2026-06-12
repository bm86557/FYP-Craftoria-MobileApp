@echo off
echo ========================================
echo LEARNING RESOURCES DEBUG LOGS
echo ========================================
echo.
echo Filtering logs for LearningResources...
echo.

adb logcat -d | findstr /C:"LearningResources"

echo.
echo ========================================
echo Press any key to exit...
pause > nul
