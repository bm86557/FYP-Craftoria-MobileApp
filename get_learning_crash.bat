@echo off
adb logcat -d | findstr /C:"LearningResources" /C:"AndroidRuntime" /C:"FATAL"
pause
