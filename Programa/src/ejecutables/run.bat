@echo off
setlocal

chcp 65001 >nul

REM Este archivo esta en: Programa\src\ejecutables\run.bat

for %%I in ("%~dp0..\..") do set "PROJECT=%%~fI"

set "LIB=%PROJECT%\librerias"
set "BIN=%PROJECT%\bin"
set "PRUEBAS=%PROJECT%\pruebas"

REM ============================================================
REM SOLICITAR ARCHIVO
REM ============================================================

echo.
echo   Carpeta de pruebas : pruebas\
echo.

set /p "ARCHIVO=  Nombre del archivo .cmm: "

if "%ARCHIVO%"=="" (
    echo.
    echo   ERROR: No se ingreso ningun archivo.
    exit /b 1
)

if /i not "%ARCHIVO:~-4%"==".cmm" (
    set "ARCHIVO=%ARCHIVO%.cmm"
)

set "FUENTE=%PRUEBAS%\%ARCHIVO%"
set "TOKENS=%PRUEBAS%\tokens.txt"

if not exist "%FUENTE%" (
    echo.
    echo   ERROR: No se encontro el archivo: pruebas\%ARCHIVO%
    echo.
    exit /b 1
)

REM ============================================================
REM EJECUTAR (Main imprime todo el reporte)
REM ============================================================

cls

cd /d "%PROJECT%"

java -cp "%BIN%;%LIB%\java-cup-11b.jar" Main "%FUENTE%" "%TOKENS%"

if errorlevel 1 (
    echo.
    echo   La ejecucion finalizo con errores inesperados.
    echo.
    exit /b 1
)

endlocal