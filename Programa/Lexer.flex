%%
%class Lexer
%public
%unicode
%line
%column
%type java_cup.runtime.Symbol
%%

. {
    System.out.println("Caracter encontrado: " + yytext());
}


