%%
%class Lexer
%public
%unicode
%line
%column
%state COMENTARIO_MULTILINEA
%state STRING_INCOMPLETO
%type java_cup.runtime.Symbol

/* MACROS */
LETRA = [a-zA-Z]
DIGITO = [0-9]
NOCERODIGITO = [1-9]
IDENTIFICADOR = {LETRA}({LETRA}|{DIGITO}|_)*
LITERAL_INT = 0|{NOCERODIGITO}{DIGITO}*
LITERAL_FLOAT = 0\.0|{NOCERODIGITO}{DIGITO}*\.{DIGITO}|0\.{DIGITO}*{NOCERODIGITO}|{NOCERODIGITO}{DIGITO}*\.{DIGITO}*{NOCERODIGITO}
PARTE_ENTERA = 0|{NOCERODIGITO}{DIGITO}*
LITERAL_EXP_INVALIDO = {LITERAL_FLOAT}[eE]{PARTE_ENTERA}|{PARTE_ENTERA}[eE]{LITERAL_FLOAT}|{LITERAL_FLOAT}[eE]{LITERAL_FLOAT}
LITERAL_EXP = (0|{NOCERODIGITO}{DIGITO}*)[eE](0|{NOCERODIGITO}{DIGITO}*)
SIMBOLO = "@"|"#"|"$"|"%"|"^"|"&"|"*"|"("|")"|"-"|"-"|"_"|"="|"+"|"["|"]"|"{"|"}"|";"|":"|"'"|"<"|">"|","|"."|"/"|"?"|"\""|"\\"|"`"|"~"
CARACTER = {LETRA}|{DIGITO}|{SIMBOLO}
LITERAL_CHAR = \'{CARACTER}?\'
LITERAL_STRING = \"{CARACTER}*\"

%%

/* REGLAS DE ERRORES */

/* Regla para enteros que empiezan con 0 */
0(0|{NOCERODIGITO}{DIGITO}*) {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": entero inválido '" + yytext() + "'"
    );
}

/* Reglas para literales de exponente inválidos */

/* Exponente sin número */
[eE] {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal exp inválido '" + yytext() + "'"
    );
}

/* 0e */
0[eE](0|{NOCERODIGITO}{DIGITO}*) {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal exp inválido '" + yytext() + "'"
    );
}

/* Número inválido seguido de e */
0(0|{NOCERODIGITO}{DIGITO}*)[eE] {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal exp inválido '" + yytext() + "'"
    );
}

/* e seguido de número */

[eE](0|{NOCERODIGITO}{DIGITO}*) {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal exp inválido '" + yytext() + "'"
    );
}

/* Número seguido de e */
(0|{NOCERODIGITO}{DIGITO}*)[eE] {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal exp inválido '" + yytext() + "'"
    );
}

/*Sin flotantes en las partes del exponencial*/
{LITERAL_EXP_INVALIDO} {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal exp inválido '" + yytext() + "'"
    );
}

//Regla base de exponenciales, está acá para evitar conflictos ccon la regla de su error
{LITERAL_EXP} {return new java_cup.runtime.Symbol(sym.LITERAL_EXP, yytext());}

/* Reglas para identificadores inválidos */

/* 1x, 123abc, 4g5, etc */
{DIGITO}+{LETRA}({LETRA}|{DIGITO}|_)* {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": identificador inválido '" + yytext() + "'"
    );
}

/* _counter, __variable, etc */
_+{LETRA}({LETRA}|{DIGITO}|_)* {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": identificador inválido '" + yytext() + "'"
    );
}

/* Reglas para flotantes inválidos */

/* 05.1, 012.45, etc. */
0{DIGITO}+\.{DIGITO}+ {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal float inválido '" + yytext() + "'"
    );
}

/* 131.8940, 0.8090, etc */
({NOCERODIGITO}{DIGITO}*|0)\.{DIGITO}+0 {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal float inválido '" + yytext() + "'"
    );
}

/* Literal char con más de un carácter */
\'{CARACTER}{CARACTER}+\' {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal char inválido '" + yytext() + "'"
    );
}


/* Literal char incompleto: 'a */
\'{CARACTER} {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal char incompleto '" + yytext() + "'"
    );
}

/* Literal char incompleto: ' */
\' {
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal char incompleto '" + yytext() + "'"
    );
}

/*
 * Cuando aparece una comilla doble sin que exista
 * un string completo, se entra en este estado.
 */
\" {
    yybegin(STRING_INCOMPLETO);
}


/* Contenido de un string incompleto */
<STRING_INCOMPLETO> {
    [^\r\n\"]+ {
        // Se continúa consumiendo el contenido para no detener la ejecución
    }

    \" {
        System.out.println(
            "Error léxico en línea " + (yyline + 1) +
            ", columna " + (yycolumn + 1) +
            ": literal string incompleto"
        );
        yybegin(YYINITIAL);
    }

    \r|\n {
        System.out.println(
            "Error léxico en línea " + (yyline + 1) +
            ", columna " + (yycolumn + 1) +
            ": literal string incompleto"
        );
        yybegin(YYINITIAL);
    }

    <<EOF>> {
        System.out.println(
            "Error léxico en línea " + (yyline + 1) +
            ", columna " + (yycolumn + 1) +
            ": literal string incompleto"
        );
        return null;
    }
}

/* Operadores aritméticos */
"++" {return new java_cup.runtime.Symbol(sym.INCREMENT);}

"+" {return new java_cup.runtime.Symbol(sym.PLUS);}

"--" {return new java_cup.runtime.Symbol(sym.DECREMENT);}

"-" {return new java_cup.runtime.Symbol(sym.MINUS);}

"*" {return new java_cup.runtime.Symbol(sym.MULTIPLY);}

"//" {return new java_cup.runtime.Symbol(sym.ENTIREDIV);}

"/" {return new java_cup.runtime.Symbol(sym.FLOATDIV);}

"mod" {return new java_cup.runtime.Symbol(sym.MOD);}

"pot" {return new java_cup.runtime.Symbol(sym.POT);}


/* Operadores relacionales */
"<=" {return new java_cup.runtime.Symbol(sym.LTE);}

">=" {return new java_cup.runtime.Symbol(sym.GTE);}

"==" {return new java_cup.runtime.Symbol(sym.EQUAL);}

"!=" {return new java_cup.runtime.Symbol(sym.NEQ);}

"<" {return new java_cup.runtime.Symbol(sym.LT);}

">" {return new java_cup.runtime.Symbol(sym.GT);}


/* Operadores lógicos */
"λ" {return new java_cup.runtime.Symbol(sym.AND);}

"θ" {return new java_cup.runtime.Symbol(sym.OR);}

"Σ" {return new java_cup.runtime.Symbol(sym.NOT);}


/* Delimitadores y bloques */
"¿:" {return new java_cup.runtime.Symbol(sym.OPBLOCK);}

":?" {return new java_cup.runtime.Symbol(sym.CLBLOCK);}

"є:" {return new java_cup.runtime.Symbol(sym.PARENOP);}

":э" {return new java_cup.runtime.Symbol(sym.PARENCL);}

"ʃ:" {return new java_cup.runtime.Symbol(sym.OPBRACKET);}

":ʅ" {return new java_cup.runtime.Symbol(sym.CLBRACKET);}

"»" {return new java_cup.runtime.Symbol(sym.TERMINATOR);}


/* Asignación y puntuación */
"Ͱ" {return new java_cup.runtime.Symbol(sym.ASIGN);}

"," {return new java_cup.runtime.Symbol(sym.COMA);}

"." {return new java_cup.runtime.Symbol(sym.DOT);}


/* Espacios y saltos de línea */
[ \t\r\n]+ { }


/* Palabras reservadas */
"val" {return new java_cup.runtime.Symbol(sym.VAL);}

"principal" {return new java_cup.runtime.Symbol(sym.PRINCIPAL);}

"defun" {return new java_cup.runtime.Symbol(sym.DEFUN);}

"int" {return new java_cup.runtime.Symbol(sym.INT);}

"float" {return new java_cup.runtime.Symbol(sym.FLOAT);}

"bool" {return new java_cup.runtime.Symbol(sym.BOOL);}

"char" {return new java_cup.runtime.Symbol(sym.CHAR);}

"string" {return new java_cup.runtime.Symbol(sym.STRING);}

"void" {return new java_cup.runtime.Symbol(sym.VOID);}

"true" {return new java_cup.runtime.Symbol(sym.TRUE);}

"false" {return new java_cup.runtime.Symbol(sym.FALSE);}

"dg" {return new java_cup.runtime.Symbol(sym.GLOBAL);}

"dl" {return new java_cup.runtime.Symbol(sym.LOCAL);}

"if" {return new java_cup.runtime.Symbol(sym.IF);}

"elif" {return new java_cup.runtime.Symbol(sym.ELIF);}

"else" {return new java_cup.runtime.Symbol(sym.ELSE);}

"while" {return new java_cup.runtime.Symbol(sym.WHILE);}

"for" {return new java_cup.runtime.Symbol(sym.FOR);}

"return" {return new java_cup.runtime.Symbol(sym.RETURN);}

"break" {return new java_cup.runtime.Symbol(sym.BREAK);}

"read" {return new java_cup.runtime.Symbol(sym.READ);}

"write" {return new java_cup.runtime.Symbol(sym.WRITE);}


/* Reglas para patrones que conservan lexema */
{LITERAL_INT} {return new java_cup.runtime.Symbol(sym.LITERAL_INT, yytext());}

{LITERAL_FLOAT} {return new java_cup.runtime.Symbol(sym.LITERAL_FLOAT, yytext());}

{LITERAL_CHAR} {return new java_cup.runtime.Symbol(sym.LITERAL_CHAR, yytext());}

{LITERAL_STRING} {return new java_cup.runtime.Symbol(sym.LITERAL_STRING, yytext());}

{IDENTIFICADOR} {return new java_cup.runtime.Symbol(sym.ID, yytext());}


/* Comentarios */
"|" [^\r\n]* {
}

"¡" {
    yybegin(COMENTARIO_MULTILINEA);
}

<COMENTARIO_MULTILINEA> {
    "!" {
        yybegin(YYINITIAL);
    }
    .|\r|\n {
    }
}

/* Regla general para caracteres no reconocidos */
. {
    System.out.println(
        "Error léxico en la línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": carácter no reconocido '" + yytext() + "'"
    );
}