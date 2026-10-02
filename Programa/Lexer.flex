import java_cup.runtime.Symbol;

%%

/*
============================================================
            ANALIZADOR LÉXICO - LEXER
============================================================

DESCRIPCIÓN:
Este archivo define las reglas del analizador léxico utilizando
JFlex. Su función es leer el código fuente carácter por carácter,
reconocer palabras reservadas, identificadores, números, cadenas,
caracteres, operadores y delimitadores, y convertirlos en tokens
que posteriormente serán utilizados por el analizador sintáctico
generado con CUP.

ENTRADA: Un archivo de código fuente

SALIDA: Tokens reconocidos por CUP.
Errores léxicos almacenados en una lista y mostrados en consola.
Cada token conserva su línea, columna y lexema.

RESTRICCIONES:
Los identificadores deben cumplir la estructura definida por la expresión IDENTIFICADOR.
Los números deben cumplir las reglas establecidas para enteros y números flotantes.
Las palabras reservadas no pueden utilizarse como identificadores.
Los caracteres, cadenas, operadores y delimitadores deben respetar
la sintaxis definida por el lenguaje.

OBJETIVO:
Separar el código fuente en unidades léxicas válidas y detectar
caracteres o estructuras que no pertenecen al lenguaje antes de
realizar el análisis sintáctico.

 */

%class Lexer
%public
%unicode
%line
%column
%cup


/*
============================================================
            ESTADOS DEL ANALIZADOR
============================================================

DESCRIPCIÓN:
Los estados permiten cambiar temporalmente las reglas que utiliza
JFlex. Se utilizan para procesar comentarios multilínea y cadenas
que quedaron abiertas.

ENTRADA: El inicio de un comentario multilínea o una cadena incompleta.

SALIDA: El analizador cambia temporalmente de estado y procesa el contenido correspondiente.

RESTRICCIONES: Mientras se encuentra dentro de uno de estos estados, las reglas
normales del analizador no se aplican.

OBJETIVO:
Evitar que el contenido de comentarios o cadenas incompletas sea interpretado como código normal.

 */

%xstate COMENTARIO_MULTILINEA
%xstate STRING_INCOMPLETO


%{
    /*
    ========================================================
            VARIABLES Y MÉTODOS AUXILIARES
    ========================================================
     */

    /*
    Guarda la posición donde comenzó un string o comentario
    multilínea que posteriormente puede generar un error.
     */
    private int lineaInicio;
    private int columnaInicio;


    /*
    Lista donde se almacenan todos los errores léxicos
    encontrados durante el análisis.
     */
    private final java.util.List<String> errores =
        new java.util.ArrayList<>();


    /*
    Permite obtener desde otras clases la lista de errores
    encontrados por el lexer.
     */
    public java.util.List<String> getErrores() {
        return errores;
    }


    /*
    -------------------------------------------------------
                    MÉTODO: symbol
    --------------------------------------------------------

    DESCRIPCIÓN:
    Crea un objeto Symbol de CUP para representar un token.

    ENTRADAS:
    tipo: código numérico del token definido en sym.java.

    SALIDA:
    Un objeto Symbol que contiene:
    tipo del token
    línea donde aparece
    columna donde aparece
    lexema reconocido

    OBJETIVO:
    Entregar al parser de CUP la información necesaria sobre
    cada token reconocido.
     */
    private Symbol symbol(int tipo) {
        return new Symbol(
            tipo,
            yyline + 1,
            yycolumn + 1,
            yytext()
        );
    }


    /*
    --------------------------------------------------------
                MÉTODO: errorLexico
    -------------------------------------------------------

    DESCRIPCIÓN:
    Registra un error léxico utilizando la posición actual
    del analizador.

    ENTRADA:
    detalle: descripción del error encontrado.
    
    SALIDA:
    El error se agrega a la lista de errores y se muestra
    en consola.
    
    OBJETIVO:
    Centralizar el reporte de errores léxicos.
     */
    private void errorLexico(String detalle) {
        errorLexico(
            detalle,
            yyline + 1,
            yycolumn + 1
        );
    }


    /*
    --------------------------------------------------------
            MÉTODO: errorLexico CON POSICIÓN
    --------------------------------------------------------
    
    DESCRIPCIÓN:
    Registra un error léxico indicando explícitamente la línea
    y columna donde comenzó el elemento incorrecto.
    
    ENTRADAS:
    detalle: descripción del error.
    linea: número de línea del error.
    columna: número de columna del error.
    
    SALIDA:
    El mensaje se almacena y se muestra en consola.
    
    OBJETIVO:
    Permitir reportar correctamente errores que abarcan varias
    líneas, como strings o comentarios sin cerrar.
     */
    private void errorLexico(
        String detalle,
        int linea,
        int columna
    ) {
        String msg =
            "Error léxico en línea " +
            linea +
            ", columna " +
            columna +
            ": " +
            detalle;

        errores.add(msg);
        System.out.println(msg);
    }
%}


/*
============================================================
                    MACROS
============================================================

DESCRIPCIÓN:son expresiones reutilizables que permiten definir
de forma clara los elementos que forman el lenguaje.

ENTRADA: Caracteres leídos por JFlex.

SALIDA: Patrones que pueden utilizarse posteriormente en las reglas.

OBJETIVO: Evitar repetir expresiones regulares y facilitar el mantenimiento
del analizador léxico.

 */

LETRA  = [a-zA-Z]
DIGITO   = [0-9]
NOCERODIGITO  = [1-9]
IDENTIFICADOR = {LETRA}( {LETRA}|{DIGITO}| \_({LETRA}|{DIGITO}) )*
ENTERO =  0| {NOCERODIGITO}{DIGITO}*
/*
 * Define la parte decimal de un número flotante.
 */
FRACCION = 0|  {DIGITO}*{NOCERODIGITO}
FLOTANTE ={ENTERO}\.{FRACCION}
SIMBOLO_SC = "@"|"#"|"$"|"%"|"^"|"&"|  "\*"|"("|")"|"-"|"\_"|"="|"+"|  "["|"]"|"{"|"}"|";"|":"|  "<"|">"|","|"."|"/"|"?"|"\\"|"\`"|"\~"|"!"|"|"
SIMBOLO = {SIMBOLO_SC}|"'"
CARACTER = {LETRA}|{DIGITO}|{SIMBOLO}|" "
CARACTER_SC = {LETRA}|{DIGITO}|{SIMBOLO_SC}|" "
LITERAL_CHAR = \'{CARACTER}\'
LITERAL_STRING =  \"{CARACTER}*\"

%%

/*
============================================================
            ERRORES: NÚMEROS
============================================================

DESCRIPCIÓN:
Detecta números que tienen una estructura no permitida por
las reglas del lenguaje.

ENTRADA: Números escritos incorrectamente.

SALIDA:
Un mensaje de error léxico.

RESTRICCIONES:
No se permiten enteros con ceros a la izquierda.
No se permiten flotantes con cero a la izquierda.
No se permiten flotantes terminados en cero, excepto x.0.
Un punto decimal debe ir acompañado de una parte decimal válida.

OBJETIVO:
Detectar errores en los números antes de entregarlos al parser.
 */


/* Enteros con ceros a la izquierda: 00, 05, 007 */
0{DIGITO}+ {
    errorLexico(
        "entero inválido '" + yytext() + "'"
    );
}


/* Flotante con cero a la izquierda: 05.1, 012.45 */
0{DIGITO}+\.{DIGITO}+ {
    errorLexico(
        "literal float inválido '" + yytext() + "'"
    );
}

/* Flotante terminado en cero: 1.50, 0.00, 131.8940 */
{ENTERO}\.{DIGITO}+0 {
    errorLexico(
        "literal float inválido '" + yytext() + "'"
    );
}


/* Flotante sin parte decimal: 5. */
{ENTERO}\. {
    errorLexico(
        "literal float incompleto '" + yytext() + "'"
    );
}


/*
============================================================
                ERRORES: IDENTIFICADORES
============================================================

DESCRIPCIÓN:
Detecta identificadores que no cumplen las reglas establecidas.

ENTRADA: Secuencias de caracteres que intentan formar identificadores.

SALIDA: Un mensaje de error léxico.

RESTRICCIONES:
Un identificador no puede comenzar con un número.
Un identificador no puede comenzar con un guion bajo.

OBJETIVO:
Evitar que identificadores inválidos lleguen al parser.
 */


/* Empieza con dígito: 1x, 123abc */
{DIGITO}+{LETRA}({LETRA}|{DIGITO}|\_)* {
    errorLexico(
        "identificador inválido '" + yytext() + "'"
    );
}


/* Empieza con guion bajo: _counter, __variable, _1 */
\_+({LETRA}|{DIGITO})({LETRA}|{DIGITO}|\_)* {
    errorLexico(
        "identificador inválido '" + yytext() + "'"
    );
}


/*
============================================================
                LITERALES CHAR
============================================================

DESCRIPCIÓN:
Reconoce caracteres individuales y detecta caracteres escritos
de manera incorrecta.

ENTRADA:
Un carácter encerrado entre comillas simples.

SALIDA:
LITERAL_CHAR si es válido.
Un error léxico si es inválido o está incompleto.

RESTRICCIONES:
Un char debe contener exactamente un carácter.

OBJETIVO:
Reconocer correctamente los literales de tipo char.
 */


/* Char válido */
{LITERAL_CHAR} {
    return symbol(sym.LITERAL_CHAR);
}

/* Char vacío: '' */
\'\' {
    errorLexico(
        "literal char vacío '" + yytext() + "'"
    );
}

/* Más de un carácter: 'ab' */
\'{CARACTER_SC}{CARACTER_SC}+\' {
    errorLexico(
        "literal char inválido '" + yytext() + "'"
    );
}


/* Char sin cerrar: 'a */
\'{CARACTER_SC}+ {
    errorLexico(
        "literal char incompleto '" + yytext() + "'"
    );
}


/* Comilla simple suelta */
\' {
    errorLexico(
        "literal char incompleto '" + yytext() + "'"
    );
}


/*
============================================================
                    STRINGS
============================================================

DESCRIPCIÓN:
Reconoce cadenas de texto y controla cadenas que quedaron
abiertas o contienen caracteres no permitidos.

ENTRADA:Texto encerrado entre comillas dobles.

SALIDA:
LITERAL_STRING si la cadena es válida.
Error léxico si la cadena es inválida o no está cerrada.

OBJETIVO:
Garantizar que las cadenas respeten las reglas del lenguaje.

 */


/* String válido */
{LITERAL_STRING} {
    return symbol(sym.LITERAL_STRING);
}

/* Comilla doble que no abre un string válido */
\" {
    lineaInicio = yyline + 1;
    columnaInicio = yycolumn + 1;
    yybegin(STRING_INCOMPLETO);
}

/*
Estado utilizado para procesar un string incompleto.
 */
<STRING_INCOMPLETO> {

    [^\r\n\"]+ {
        /* Se consume el contenido para poder continuar. */
    }

    /*
    Se encontró el cierre del string, pero el contenido
    ontiene caracteres no permitidos.
     */
    \" {
        errorLexico(
            "literal string inválido (carácter no permitido)",
            lineaInicio,
            columnaInicio
        );
        yybegin(YYINITIAL);
    }

    /*
    La cadena llegó al final de la línea sin cerrarse.
     */
    \r|\n {
        errorLexico(
            "literal string sin cerrar",
            lineaInicio,
            columnaInicio
        );
        yybegin(YYINITIAL);
    }

    /*
    La cadena llegó al final del archivo sin cerrarse.
     */
    <<EOF>> {
        errorLexico(
            "literal string sin cerrar",
            lineaInicio,
            columnaInicio
        );
        yybegin(YYINITIAL);
        return new Symbol(sym.EOF);
    }
}


/*
============================================================
            OPERADORES 
============================================================
 */

"++" { return symbol(sym.INCREMENT); }
"+"  { return symbol(sym.PLUS); }
"--" { return symbol(sym.DECREMENT); }
"-"  { return symbol(sym.MINUS); }
"\*" { return symbol(sym.MULTIPLY); }
"//" { return symbol(sym.ENTIREDIV); }
"/"  { return symbol(sym.FLOATDIV); }
"mod" { return symbol(sym.MOD); }
"pot" { return symbol(sym.POT); }
"<=" { return symbol(sym.LTE); }
">=" { return symbol(sym.GTE); }
"==" { return symbol(sym.EQUAL); }
"!=" { return symbol(sym.NEQ); }
"<"  { return symbol(sym.LT); }
">"  { return symbol(sym.GT); }
"λ" { return symbol(sym.AND); }
"θ" { return symbol(sym.OR); }
"Σ" { return symbol(sym.NOT); }


/*
============================================================
        DELIMITADORES Y OPERADORES ESPECIALES
============================================================
 */

"¿:" { return symbol(sym.OPBLOCK); }
":?" { return symbol(sym.CLBLOCK); }
"є:" { return symbol(sym.PARENOP); }
":э" { return symbol(sym.PARENCL); }
"ʃ:" { return symbol(sym.OPBRACKET); }
":ʅ" { return symbol(sym.CLBRACKET); }
"»"  { return symbol(sym.TERMINATOR); }
"Ͱ"  { return symbol(sym.ASIGN); }
","  { return symbol(sym.COMA); }


/*
============================================================
                PALABRAS RESERVADAS
============================================================
 */

"val"       { return symbol(sym.VAL); }
"principal" { return symbol(sym.PRINCIPAL); }
"int"       { return symbol(sym.INT); }
"float"     { return symbol(sym.FLOAT); }
"bool"      { return symbol(sym.BOOL); }
"char"      { return symbol(sym.CHAR); }
"string"    { return symbol(sym.STRING); }
"void"      { return symbol(sym.VOID); }
"true"      { return symbol(sym.TRUE); }
"false"     { return symbol(sym.FALSE); }
"if"        { return symbol(sym.IF); }
"elif"      { return symbol(sym.ELIF); }
"else"      { return symbol(sym.ELSE); }
"while"     { return symbol(sym.WHILE); }
"for"       { return symbol(sym.FOR); }
"return"    { return symbol(sym.RETURN); }
"break"     { return symbol(sym.BREAK); }
"print"     { return symbol(sym.PRINT); }
"read"      { return symbol(sym.READ); }
"write"     { return symbol(sym.WRITE); }


/*
============================================================
        LITERALES E IDENTIFICADORES
============================================================
 */

{ENTERO} {return symbol(sym.LITERAL_INT);}
{FLOTANTE} {return symbol(sym.LITERAL_FLOAT);}
{IDENTIFICADOR} {return symbol(sym.ID);}


/*
Identificador con guion bajo mal ubicado.
Esta regla detecta identificadores que comienzan correctamente
pero terminan utilizando el guion bajo de forma incorrecta.
 */

{LETRA}({LETRA}|{DIGITO}|\_)* {
    errorLexico(
        "identificador inválido '" + yytext() + "'"
    );
}


/*
============================================================
                ESPACIOS
============================================================

DESCRIPCIÓN:Ignora espacios, tabulaciones y saltos de línea.

ENTRADA:Espacios en blanco del código fuente.

SALIDA:Ningún token.

OBJETIVO:Evitar que los espacios interfieran con el análisis léxico.
 */

[ \t\r\n]+ { }

/* Comentario de una línea */
"|"[^\r\n]* {
    /* El comentario se ignora. */
}

/* Inicio de comentario multilínea */
"¡" {
    lineaInicio = yyline + 1;
    columnaInicio = yycolumn + 1;
    yybegin(COMENTARIO_MULTILINEA);
}

/*
 * Estado utilizado mientras se procesa un comentario multilínea.
 */
<COMENTARIO_MULTILINEA> {

    "!" {
        yybegin(YYINITIAL);
    }

    [^] {
        /* Se ignora el contenido del comentario. */
    }

    <<EOF>> {
        errorLexico(
            "comentario multilínea sin cerrar",
            lineaInicio,
            columnaInicio
        );
        yybegin(YYINITIAL);
        return new Symbol(sym.EOF);
    }
}


/*
============================================================
            CUALQUIER OTRO CARÁCTER
============================================================
DESCRIPCIÓN:
captura cualquier carácter que no haya sido reconocido por
as reglas anteriores.

ENTRADA:
Cualquier carácter no contemplado por el lenguaje.

SALIDA:
Un error léxico indicando el carácter no reconocido.

OBJETIVO:
Evitar que caracteres desconocidos pasen silenciosamente
al analizador sintáctico.

 */

. {
    errorLexico(
        "carácter no reconocido '" + yytext() + "'"
    );
}
