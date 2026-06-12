@echo off
echo Getting last 200 lines of OrdersViewModel logs...
echo.
adb logcat -d -s OrdersViewModel:* | findstr /C:"CANCEL" /C:"REFUND" /C:"Payment Intent" /C:"Backend" /C:"Error" /C:"Failed" /C:"SUCCESS"
echo.
echo Done!
pause
