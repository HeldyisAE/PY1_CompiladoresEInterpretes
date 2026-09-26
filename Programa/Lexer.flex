%%
%class Lexer
%public
%unicode
%line
%column
%%

. {
    System.out.println("Caracter encontrado: " + yytext());
}


