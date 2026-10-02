@echo off
setlocal enabledelayedexpansion
set LIB=librerias

for %%f in (%LIB%\*.jar) do (
    set "n=%%~nxf"
    echo !n! | findstr /i "jflex" >nul && set "JFLEX=%LIB%\!n!"
    echo !n! | findstr /i "runtime" >nul && set "RUNTIME=%LIB%\!n!"
    echo !n! | findstr /i "cup" | findstr /v /i "runtime" >nul && set "CUP=%LIB%\!n!"
)
if not defined RUNTIME set "RUNTIME=%CUP%"

if not defined JFLEX echo No se encontro el jar de JFlex en %LIB% & exit /b 1
if not defined CUP echo No se encontro el jar de CUP en %LIB% & exit /b 1

echo JFlex   : %JFLEX%
echo CUP     : %CUP%
echo Runtime : %RUNTIME%

if not exist src mkdir src
if not exist bin mkdir bin

java -cp "%CUP%" java_cup.Main -destdir src -parser Parser -symbols sym Parser.cup || exit /b 1
java -jar "%JFLEX%" --encoding utf-8 -d src Lexer.flex || exit /b 1
javac -encoding UTF-8 -cp "%RUNTIME%" -d bin src\*.java || exit /b 1

echo Compilacion lista.
