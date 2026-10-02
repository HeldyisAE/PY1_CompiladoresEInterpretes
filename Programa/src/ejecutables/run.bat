@echo off
setlocal

chcp 65001 >nul

REM ============================================================
REM UBICACION DEL PROYECTO
REM ============================================================

REM Este archivo esta en: Programa\src\ejecutables\run.bat

for %%I in ("%~dp0..\..") do set "PROJECT=%%~fI"

set "LIB=%PROJECT%\librerias"
set "BIN=%PROJECT%\bin"
set "PRUEBAS=%PROJECT%\pruebas"

REM ============================================================
REM SOLICITAR ARCHIVO DE PRUEBA
REM ============================================================

echo.
echo ============================================================
echo                  EJECUCION DEL PROYECTO
echo ============================================================
echo.
echo Los archivos de prueba se encuentran en:
echo %PRUEBAS%
echo.

set /p "ARCHIVO=Ingrese el nombre del archivo .cmm: "

REM ============================================================
REM VALIDAR NOMBRE
REM ============================================================

if "%ARCHIVO%"=="" (
    echo.
    echo ERROR: No se ingreso ningun archivo.
    exit /b 1
)

REM Agregar .cmm automaticamente si no se escribio
if /i not "%ARCHIVO:~-4%"==".cmm" (
    set "ARCHIVO=%ARCHIVO%.cmm"
)

set "FUENTE=%PRUEBAS%\%ARCHIVO%"
set "TOKENS=%PRUEBAS%\tokens.txt"

REM ============================================================
REM VALIDAR ARCHIVO
REM ============================================================

if not exist "%FUENTE%" (
    echo.
    echo ERROR: No se encontro el archivo:
    echo %FUENTE%
    echo.
    exit /b 1
)

REM ============================================================
REM EJECUTAR ANALIZADOR
REM (Main imprime todos los encabezados y resultados)
REM ============================================================

cd /d "%PROJECT%"

java -cp "%BIN%;%LIB%\java-cup-11b.jar" Main "%FUENTE%" "%TOKENS%"

if errorlevel 1 (
    echo.
    echo ============================================================
    echo              LA EJECUCION FINALIZO CON ERRORES
    echo ============================================================
    echo.
    exit /b 1
)

endlocal