```bat
@echo off
setlocal enabledelayedexpansion

chcp 65001 >nul

REM ============================================================
REM UBICACION DEL PROYECTO
REM ============================================================

REM Este archivo esta en:
REM Programa\src\ejecutables\build.bat

REM PROJECT apunta a:
REM Programa

set "PROJECT=%~dp0..\.."
set "LIB=%PROJECT%\librerias"
set "BIN=%PROJECT%\bin"
set "SRC=%PROJECT%\src"

REM ============================================================
REM BUSCAR LIBRERIAS
REM ============================================================

for %%f in ("%LIB%\*.jar") do (
    set "n=%%~nxf"

    echo !n! | findstr /i "jflex" >nul && set "JFLEX=%LIB%\!n!"

    echo !n! | findstr /i "runtime" >nul && set "RUNTIME=%LIB%\!n!"

    echo !n! | findstr /i "cup" | findstr /v /i "runtime" >nul && set "CUP=%LIB%\!n!"
)

if not defined RUNTIME set "RUNTIME=%CUP%"

REM ============================================================
REM VALIDAR LIBRERIAS
REM ============================================================

if not defined JFLEX (
    echo.
    echo ERROR: No se encontro el jar de JFlex en:
    echo %LIB%
    exit /b 1
)

if not defined CUP (
    echo.
    echo ERROR: No se encontro el jar de CUP en:
    echo %LIB%
    exit /b 1
)

echo.
echo ============================================================
echo                COMPILACION DEL PROYECTO
echo ============================================================
echo.
echo Proyecto : %PROJECT%
echo JFlex    : %JFLEX%
echo CUP      : %CUP%
echo Runtime  : %RUNTIME%
echo Bin      : %BIN%
echo.

REM ============================================================
REM MOVERSE A LA CARPETA DEL PROYECTO
REM ============================================================

cd /d "%PROJECT%"

REM ============================================================
REM CREAR BIN
REM ============================================================

if not exist "%BIN%" mkdir "%BIN%"

REM ============================================================
REM LIMPIAR ARCHIVOS GENERADOS
REM ============================================================

echo Limpiando archivos generados anteriores...

del /q "%BIN%\*.java" 2>nul
del /q "%BIN%\*.class" 2>nul

echo Limpieza de bin completada.
echo.

REM ============================================================
REM GENERAR PARSER CON CUP
REM ============================================================

echo Generando Parser con CUP...

java -cp "%CUP%" java_cup.Main ^
    -destdir "%BIN%" ^
    -parser parser ^
    -symbols sym ^
    Parser.cup

if errorlevel 1 (
    echo.
    echo ERROR: CUP no pudo generar el parser.
    exit /b 1
)

echo CUP generado correctamente.
echo.

REM ============================================================
REM GENERAR LEXER CON JFLEX
REM ============================================================

echo Generando Lexer con JFlex...

java -jar "%JFLEX%" ^
    --encoding utf-8 ^
    -d "%BIN%" ^
    Lexer.flex

if errorlevel 1 (
    echo.
    echo ERROR: JFlex no pudo generar el Lexer.
    exit /b 1
)

echo JFlex generado correctamente.
echo.

REM ============================================================
REM COMPILAR JAVA
REM ============================================================

echo Compilando archivos Java...

javac ^
    -encoding UTF-8 ^
    -cp "%RUNTIME%;%BIN%" ^
    -d "%BIN%" ^
    "%SRC%\Main.java" ^
    "%BIN%\Lexer.java" ^
    "%BIN%\parser.java" ^
    "%BIN%\sym.java"

if errorlevel 1 (
    echo.
    echo ============================================================
    echo ERROR: LA COMPILACION DE JAVA FALLO.
    echo ============================================================
    exit /b 1
)

echo.
echo ============================================================
echo              COMPILACION COMPLETADA
echo ============================================================
echo.
echo Todos los archivos generados se encuentran en:
echo %BIN%
echo.

dir "%BIN%"

echo.
echo ============================================================

endlocal
```
