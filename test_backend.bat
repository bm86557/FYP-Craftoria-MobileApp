@echo off
echo Testing Backend Connection...
echo.
echo Backend URL: https://sandbox-backend-production.up.railway.app
echo.

curl -X GET https://sandbox-backend-production.up.railway.app

echo.
echo.
echo Testing Refund Endpoint...
echo.

curl -X POST https://sandbox-backend-production.up.railway.app/refund-payment ^
  -H "Content-Type: application/json" ^
  -d "{\"paymentIntentId\":\"test\",\"amount\":1000,\"reason\":\"test\",\"orderId\":\"test\"}"

echo.
echo.
pause
