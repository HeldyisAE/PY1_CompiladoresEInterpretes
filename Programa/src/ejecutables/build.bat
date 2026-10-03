@echo off
setlocal enabledelayedexpansion

chcp 65001 >nul


REM ============================================================
REM UBICACION DEL PROYECTO
REM ============================================================
REM Descripcion:
REM Aqui obtengo la ubicacion del proyecto a partir de este archivo.
REM
REM Entrada:
REM La ubicacion donde se encuentra build.bat.
REM
REM Salida:
REM Las rutas principales del proyecto.
REM
REM Restricciones:
REM El archivo debe mantenerse dentro de la carpeta esperada.
REM
REM Objetivo:
REM Tener disponibles las rutas que necesito para compilar.

set "PROJECT=%~dp0..\.."

set "LIB=%PROJECT%\librerias"

set "BIN=%PROJECT%\bin"

set "SRC=%PROJECT%\src"


REM ============================================================
REM BUSCAR LIBRERIAS
REM ============================================================
REM Descripcion:
REM Busco los archivos jar que necesito para generar y compilar.
REM
REM Entrada:
REM Los archivos jar que estan dentro de librerias.
REM
REM Salida:
REM Las rutas de JFlex, CUP y su runtime.
REM
REM Restricciones:
REM Las librerias deben estar dentro de la carpeta librerias.
REM
REM Objetivo:
REM Encontrar automaticamente las librerias sin escribir sus nombres.

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
REM Descripcion:
REM Verifico que las librerias necesarias hayan sido encontradas.
REM
REM Entrada:
REM Las rutas encontradas anteriormente.
REM
REM Salida:
REM Un mensaje de error si falta alguna libreria.
REM
REM Restricciones:
REM JFlex y CUP deben estar disponibles para continuar.
REM
REM Objetivo:
REM Evitar que la compilacion continue si falta una libreria.

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
REM Descripcion:
REM Cambio la ubicacion actual a la carpeta principal del proyecto.
REM
REM Entrada:
REM La ruta almacenada en PROJECT.
REM
REM Salida:
REM Los siguientes comandos se ejecutan desde el proyecto.
REM
REM Restricciones:
REM La carpeta del proyecto debe existir.
REM
REM Objetivo:
REM Ejecutar todos los comandos desde la ubicacion correcta.

cd /d "%PROJECT%"


REM ============================================================
REM CREAR BIN
REM ============================================================
REM Descripcion:
REM Creo la carpeta donde se guardaran los archivos generados.
REM
REM Entrada:
REM La ruta de la carpeta bin.
REM
REM Salida:
REM La carpeta bin si no existia.
REM
REM Restricciones:
REM No se crea otra carpeta si bin ya existe.
REM
REM Objetivo:
REM Tener un lugar para guardar los archivos de compilacion.

if not exist "%BIN%" mkdir "%BIN%"


REM ============================================================
REM LIMPIAR ARCHIVOS GENERADOS
REM ============================================================
REM Descripcion:
REM Elimino los archivos generados anteriormente para comenzar limpio.
REM
REM Entrada:
REM Los archivos Java y class dentro de bin.
REM
REM Salida:
REM La carpeta bin queda lista para una nueva compilacion.
REM
REM Restricciones:
REM Solo se eliminan archivos .java y .class de bin.
REM
REM Objetivo:
REM Evitar conflictos con archivos generados anteriormente.

echo Limpiando archivos generados anteriores...

del /q "%BIN%\*.java" 2>nul

del /q "%BIN%\*.class" 2>nul

echo Limpieza de bin completada.

echo.



REM ============================================================
REM GENERAR PARSER CON CUP
REM ============================================================
REM Descripcion:
REM Uso CUP para generar el parser y los simbolos de la gramatica.
REM
REM Entrada:
REM El archivo Parser.cup y la libreria de CUP.
REM
REM Salida:
REM Se generan parser.java y sym.java dentro de bin.
REM
REM Restricciones:
REM Parser.cup debe tener una gramatica valida.
REM
REM Objetivo:
REM Crear el analizador sintactico que utilizara el proyecto.

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
REM Descripcion:
REM Uso JFlex para generar el analizador lexico.
REM
REM Entrada:
REM El archivo Lexer.flex y la libreria de JFlex.
REM
REM Salida:
REM Se genera Lexer.java dentro de bin.
REM
REM Restricciones:
REM Lexer.flex debe tener reglas lexicas validas.
REM
REM Objetivo:
REM Crear el analizador que reconoce los tokens del lenguaje.

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
REM Descripcion:
REM Compilo los archivos Java necesarios para ejecutar el proyecto.
REM
REM Entrada:
REM Main.java, Lexer.java, parser.java y sym.java.
REM
REM Salida:
REM Se generan los archivos .class dentro de bin.
REM
REM Restricciones:
REM Los archivos Java deben estar correctos y las librerias disponibles.
REM
REM Objetivo:
REM Dejar el proyecto compilado y listo para ejecutarse.

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