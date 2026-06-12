@echo off
echo Clearing old logs...
adb logcat -c

echo.
echo Waiting for refund attempt...
echo Please try to cancel a Stripe order now.
echo.
echo Press Ctrl+C to stop logging.
echo.

adb logcat -s OrdersViewModel:D OrdersViewModel:E OrdersViewModel:W
