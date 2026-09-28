%%
%class Lexer
%public
%unicode
%line
%column
%state COMENTARIO_MULTILINEA
%type java_cup.runtime.Symbol

/*MACROS*/
LETRA = [a-zA-Z]
DIGITO = [0-9]
NOCERODIGITO = [1-9]
IDENTIFICADOR = {LETRA}({LETRA}|{DIGITO}|_)*
LITERAL_INT = 0|{NOCERODIGITO}{DIGITO}*
LITERAL_FLOAT = 0\.0|{NOCERODIGITO}{DIGITO}*\.{DIGITO}|0\.{DIGITO}*{NOCERODIGITO}|{NOCERODIGITO}{DIGITO}*\.{DIGITO}*{NOCERODIGITO}
LITERAL_EXP = (0|{NOCERODIGITO}{DIGITO}*)[eE](0|{NOCERODIGITO}{DIGITO}*)
SIMBOLO = "@"|"#"|"$"|"%"|"^"|"&"|"*"|"("|")"|"-"|"_"|"="|"+"|"["|"]"|"{"|"}"|";"|":"|"'"|"<"|">"|","|"."|"/"|"?"|"\""|"\\"|"`"|"~"
CARACTER = {LETRA}|{DIGITO}|{SIMBOLO}
LITERAL_CHAR = \'{CARACTER}\'
LITERAL_STRING = \"{CARACTER}*\" 

%%

/*Reglas de errores*/

//Regla para enteros que empiezan con 0
0(0|{NOCERODIGITO}{DIGITO}*) { //0031231
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": entero inválido '" + yytext() + "'"
    );
}

//Esta está aquí para evitar conflictos con la siguiente regla
{LITERAL_EXP}       {return new java_cup.runtime.Symbol(sym.LITERAL_EXP, yytext());} 

//Reglas para identificadores inválidos
{DIGITO}+{LETRA}({LETRA}|{DIGITO}|_)* { //1x
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": identificador inválido '" + yytext() + "'"
    );
}

_+{LETRA}({LETRA}|{DIGITO}|_)* { //_counter
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": identificador inválido '" + yytext() + "'"
    );
}

//Reglas para flotantes
0{DIGITO}+\.{DIGITO}+ { //05.1
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal float inválido '" + yytext() + "'"
    );
}

({NOCERODIGITO}{DIGITO}*|0)\.{DIGITO}+0 { //131.8940 o 0.8090
    System.out.println(
        "Error léxico en línea " + (yyline + 1) +
        ", columna " + (yycolumn + 1) +
        ": literal float inválido '" + yytext() + "'"
    );
}

/*Operadores aritméticos*/
"++"    { return new java_cup.runtime.Symbol(sym.INCREMENT); }
"+"     { return new java_cup.runtime.Symbol(sym.PLUS); }
"--"    { return new java_cup.runtime.Symbol(sym.DECREMENT); }
"-"     { return new java_cup.runtime.Symbol(sym.MINUS); }
"*"     { return new java_cup.runtime.Symbol(sym.MULTIPLY); }
"//"    { return new java_cup.runtime.Symbol(sym.ENTIREDIV); }
"/"     { return new java_cup.runtime.Symbol(sym.FLOATDIV); }
"mod"   { return new java_cup.runtime.Symbol(sym.MOD); }
"pot"   { return new java_cup.runtime.Symbol(sym.POT); }

/*Operadores relacionales*/
"<="    { return new java_cup.runtime.Symbol(sym.LTE); }
">="    { return new java_cup.runtime.Symbol(sym.GTE); }
"=="    { return new java_cup.runtime.Symbol(sym.EQUAL); }
"!="    { return new java_cup.runtime.Symbol(sym.NEQ); }
"<"     { return new java_cup.runtime.Symbol(sym.LT); }
">"     { return new java_cup.runtime.Symbol(sym.GT); }

/*Operadores logicos*/
"λ"     { return new java_cup.runtime.Symbol(sym.AND); }
"θ"     { return new java_cup.runtime.Symbol(sym.OR); }
"Σ"     { return new java_cup.runtime.Symbol(sym.NOT); }

/* --- Delimitadores y Bloques --- */
"¿:"    { return new java_cup.runtime.Symbol(sym.OPBLOCK); }
":?"    { return new java_cup.runtime.Symbol(sym.CLBLOCK); }
"є:"    { return new java_cup.runtime.Symbol(sym.PARENOP); }
":э"    { return new java_cup.runtime.Symbol(sym.PARENCL); }
"ʃ:"    { return new java_cup.runtime.Symbol(sym.OPBRACKET); }
":ʅ"    { return new java_cup.runtime.Symbol(sym.CLBRACKET); }
"»"     { return new java_cup.runtime.Symbol(sym.TERMINATOR); }

/*Asignacion y puntuacion*/
"Ͱ"     { return new java_cup.runtime.Symbol(sym.ASIGN); }
","     { return new java_cup.runtime.Symbol(sym.COMA); }
"."     { return new java_cup.runtime.Symbol(sym.DOT); }

/* Espacios y saltos de línea */
[ \t\r\n]+   { }

/*Palabras reservadas*/
"val"       { return new java_cup.runtime.Symbol(sym.VAL); }
"principal" { return new java_cup.runtime.Symbol(sym.PRINCIPAL); }
"defun"     { return new java_cup.runtime.Symbol(sym.DEFUN); }
"int"       { return new java_cup.runtime.Symbol(sym.INT); }
"float"     { return new java_cup.runtime.Symbol(sym.FLOAT); }  
"bool"      { return new java_cup.runtime.Symbol(sym.BOOL); }   
"char"      { return new java_cup.runtime.Symbol(sym.CHAR); }   
"string"    { return new java_cup.runtime.Symbol(sym.STRING); }   
"void"      { return new java_cup.runtime.Symbol(sym.VOID); }   
"true"      { return new java_cup.runtime.Symbol(sym.TRUE); }   
"false"     { return new java_cup.runtime.Symbol(sym.FALSE); }
"dg"        { return new java_cup.runtime.Symbol(sym.GLOBAL); }
"dl"        { return new java_cup.runtime.Symbol(sym.LOCAL); }
"if"        { return new java_cup.runtime.Symbol(sym.IF); }
"elif"      { return new java_cup.runtime.Symbol(sym.ELIF); }
"else"      { return new java_cup.runtime.Symbol(sym.ELSE); }
"while"     { return new java_cup.runtime.Symbol(sym.WHILE); }
"for"       { return new java_cup.runtime.Symbol(sym.FOR); }
"return"    { return new java_cup.runtime.Symbol(sym.RETURN); }
"break"     { return new java_cup.runtime.Symbol(sym.BREAK); }
"read"      { return new java_cup.runtime.Symbol(sym.READ); }
"write"     { return new java_cup.runtime.Symbol(sym.WRITE); }

/* Reglas para patrones que conservan lexema */
{LITERAL_INT}       {return new java_cup.runtime.Symbol(sym.LITERAL_INT, yytext());}
{LITERAL_FLOAT}     {return new java_cup.runtime.Symbol(sym.LITERAL_FLOAT, yytext());}

{LITERAL_CHAR}      {return new java_cup.runtime.Symbol(sym.LITERAL_CHAR, yytext());}
{LITERAL_STRING}    {return new java_cup.runtime.Symbol(sym.LITERAL_STRING, yytext());}
{IDENTIFICADOR}     {return new java_cup.runtime.Symbol(sym.ID, yytext());}

/*Comentarios*/
"|" [^\r\n]* { }
"¡" { yybegin(COMENTARIO_MULTILINEA); }

<COMENTARIO_MULTILINEA> {
    "!"       { yybegin(YYINITIAL); }
    .|\r|\n   { }
}

. {
    System.out.println(
        "Error léxico en la línea " + (yyline + 1) + 
        ", columna " + (yycolumn + 1) + 
        ": carácter no reconocido '" + yytext() + "'"
    );
}


