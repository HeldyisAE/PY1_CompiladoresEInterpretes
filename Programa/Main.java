import java.io.FileReader;

public class Main {
    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer(new FileReader("prueba.cmm")); //Abre el archivo y lo pasa al lexer
            java_cup.runtime.Symbol token;
            while ((token = lexer.yylex()) != null) {
                System.out.println("Detectado token: " + sym.terminalNames[token.sym] +
                                    " | El lexema es: " + token.value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
