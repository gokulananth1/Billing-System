@echo off
echo ===================================================
echo  Compiling BillPro Electronics Billing System...
echo ===================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -d bin -sourcepath src src\com\electro\Main.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Compilation finished without errors!
    echo Output directory: bin\
) else (
    echo.
    echo [ERROR] Compilation failed. Please check Java compiler output above.
)
pause
