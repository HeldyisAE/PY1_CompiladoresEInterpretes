%%
%class Lexer
%public
%unicode
%line
%column
%type java_cup.runtime.Symbol

/*MACROS*/
LETRA = [a-zA-Z]
DIGITO = [0-9]
NOCERODIGITO = [1-9]
IDENTIFICADOR = {LETRA}({LETRA}|{DIGITO}|_)*
LITERAL_INT = 0|{NOCERODIGITO}{DIGITO}*
LITERAL_FLOAT = 0\.0|0\.{DIGITO}*{NOCERODIGITO}|{NOCERODIGITO}{DIGITO}*\.{DIGITO}*{NOCERODIGITO}
LITERAL_EXP = (0|{NOCERODIGITO}{DIGITO}*)[eE](0|{NOCERODIGITO}{DIGITO}*)
SIMBOLO = "@"|"#"|"$"|"%"|"^"|"&"|"*"|"("|")"|"-"|"_"|"="|"+"|"["|"]"|"{"|"}"|";"|":"|"'"|"<"|">"|","|"."|"/"|"?"|"\""|"\\"|"`"|"~"
CARACTER = {LETRA}|{DIGITO}|{SIMBOLO}
LITERAL_CHAR = \'{CARACTER}\'
LITERAL_STRING = \"{CARACTER}*\"

/*Operadores aritméticos*/
"++"    {...}
"+"     {...}
"--"    {...}
"-"     {...}
"*"     {...}
"//"    {...}
"/"     {...}
"mod"   {...}
"pot"   {...}

/*Operadores relacionales*/
"<="    {...}
">="    {...}
"=="    {...}
"!="    {...}
"<"     {...}
">"     {...}

/*Operadores logicos*/
"λ"     {...}
"θ"     {...}
"Σ"     {...}

/* --- Delimitadores y Bloques --- */
"¿:"    {...}
":?"    {...}
"є:"    {...}
":э"    {...}
"ʃ:"    {...}
":ʅ"    {...}
"»"     {...}

/*Asignacion y puntuacion*/
"Ͱ"     {...}
","     {...}
"."     {...}

/* Espacios y saltos de línea */
[ \t\r\n]+   { }

/*Palabras reservadas*/
"val"
"principal"
"defun"
"int"          
"float"       
"bool"         
"char"         
"string"       
"void"         
"true"         
"false"
"dg"
"dl"
"if"
"elif"
"else"
"while"
"for"
"return"
"break"
"read"
"write"

/*Comentarios*/
"|"
"!"
"¡"
%%

. {
    System.out.println("Caracter encontrado: " + yytext());
}


