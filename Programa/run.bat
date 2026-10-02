@echo off
setlocal enabledelayedexpansion
set LIB=librerias

for %%f in (%LIB%\*.jar) do (
    set "n=%%~nxf"
    echo !n! | findstr /i "runtime" >nul && set "RUNTIME=%LIB%\!n!"
    echo !n! | findstr /i "cup" | findstr /v /i "runtime" >nul && set "CUP=%LIB%\!n!"
)
if not defined RUNTIME set "RUNTIME=%CUP%"

chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "bin;%RUNTIME%" Main %*
