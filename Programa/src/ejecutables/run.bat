@echo off

setlocal

chcp 65001 >nul


REM ============================================================
REM UBICACION DEL PROYECTO
REM ============================================================
REM Descripcion:
REM Obtengo la ubicacion principal del proyecto.
REM
REM Entrada:
REM La ubicacion donde se encuentra este archivo.
REM
REM Salida:
REM La ruta del proyecto y sus carpetas principales.
REM
REM Restricciones:
REM El archivo debe estar dentro de la estructura del proyecto.
REM
REM Objetivo:
REM Tener las rutas necesarias para ejecutar el programa.

for %%I in ("%~dp0..\..") do set "PROJECT=%%~fI"

set "LIB=%PROJECT%\librerias"

set "BIN=%PROJECT%\bin"

set "PRUEBAS=%PROJECT%\pruebas"


REM ============================================================
REM SOLICITAR ARCHIVO
REM ============================================================
REM Descripcion:
REM Le pido al usuario el nombre del archivo que quiere analizar.
REM
REM Entrada:
REM El nombre de un archivo con extension .cmm.
REM
REM Salida:
REM La ruta del archivo que se va a analizar.
REM
REM Restricciones:
REM El archivo debe existir dentro de la carpeta pruebas.
REM
REM Objetivo:
REM Permitir seleccionar facilmente el archivo de prueba.

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


REM ============================================================
REM VALIDAR ARCHIVO
REM ============================================================
REM Descripcion:
REM Verifico que el archivo seleccionado exista.
REM
REM Entrada:
REM El nombre del archivo ingresado por el usuario.
REM
REM Salida:
REM El programa continua si el archivo existe.
REM
REM Restricciones:
REM El archivo debe estar dentro de la carpeta pruebas.
REM
REM Objetivo:
REM Evitar ejecutar el analizador con un archivo inexistente.

if not exist "%FUENTE%" (

    echo.

    echo   ERROR: No se encontro el archivo: pruebas\%ARCHIVO%

    echo.

    exit /b 1

)


REM ============================================================
REM EJECUTAR PROGRAMA
REM ============================================================
REM Descripcion:
REM Ejecuto el programa Java con el archivo seleccionado.
REM
REM Entrada:
REM El archivo .cmm y la ruta donde se guardaran los tokens.
REM
REM Salida:
REM El analisis lexico y sintactico mostrado por Main.
REM
REM Restricciones:
REM El proyecto debe estar compilado antes de ejecutar este archivo.
REM
REM Objetivo:
REM Ejecutar el analizador sobre el archivo de prueba.

cls

cd /d "%PROJECT%"

java -cp "%BIN%;%LIB%\java-cup-11b.jar" Main "%FUENTE%" "%TOKENS%"


REM ============================================================
REM VALIDAR EJECUCION
REM ============================================================
REM Descripcion:
REM Verifico si el programa termino con algun error inesperado.
REM
REM Entrada:
REM El resultado de la ejecucion de Main.
REM
REM Salida:
REM Un mensaje si la ejecucion fallo.
REM
REM Restricciones:
REM El programa debe terminar correctamente.
REM
REM Objetivo:
REM Informar al usuario si la ejecucion no pudo completarse.

if errorlevel 1 (

    echo.

    echo   La ejecucion finalizo con errores inesperados.

    echo.

    exit /b 1

)

endlocal
