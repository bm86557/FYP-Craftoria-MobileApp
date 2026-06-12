@echo off
echo Getting logs for MyApplication...
echo.

adb logcat -d | findstr /C:"OrdersViewModel" /C:"CANCEL" /C:"REFUND" /C:"Stripe"

echo.
echo.
echo If no logs above, try this:
echo.

adb shell "logcat -d | grep -i 'ordersviewmodel\|refund\|stripe'"

echo.
pause
