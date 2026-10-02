import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java_cup.runtime.Symbol;


/* ============================================================
                    ANALIZADOR DEL LENGUAJE
   ============================================================ */

/*
Descripción:
Programa principal del compilador.
El análisis se realiza en dos fases:
FASE 1: Análisis léxico.
FASE 2: Análisis sintáctico.

Primero se lee el archivo .cmm, se reconocen sus tokens
y se genera un archivo tokens.txt.

Posteriormente se vuelve a leer el archivo fuente para
realizar el análisis sintáctico utilizando el parser
generado por Java CUP.

Entrada:Archivo fuente con extensión .cmm.

Salida:
Tokens reconocidos.
Archivo tokens.txt.
Errores léxicos.
Errores sintácticos.


 */


public class Main {


    public static void main(String[] args) throws Exception {


        /* ========================================================
                        VALIDAR ARGUMENTOS
           ======================================================== */

        /*
         * Descripción:
         * Verifica que se haya proporcionado al menos un argumento.
         *
         * Entrada:
         * - Ruta del archivo fuente.
         * - Opcionalmente, ruta del archivo de tokens.
         *
         * Salida:
         * Mensaje de uso si no se proporciona el archivo fuente.
         *
         * Objetivo:
         * Evitar ejecutar el programa sin un archivo de entrada.
         */

        if (args.length < 1) {

            System.out.println(
                    "Uso: java Main <archivo_fuente> [archivo_tokens]"
            );

            return;
        }


        String rutaFuente = args[0];

        String rutaTokens = args.length > 1
                ? args[1]
                : "tokens.txt";


        /* ========================================================
                        MAPA DE TOKENS
           ======================================================== */

        /*
         * Descripción:
         * CUP genera los tokens utilizando números enteros.
         *
         * Este mapa permite convertir esos números en nombres
         * comprensibles para mostrar los tokens.
         *
         * Ejemplo:
         *
         * 52 -> ID
         * 53 -> LITERAL_INT
         *
         * Entrada:
         * Clase sym generada por CUP.
         *
         * Salida:
         * Mapa número -> nombre del token.
         */

        Map<Integer, String> nombres = nombresDeTokens();


        /* ========================================================
                    VARIABLES DEL ANÁLISIS
           ======================================================== */

        /*
         * Cantidad total de tokens reconocidos durante el
         * análisis léxico.
         */

        int totalTokens = 0;

        Lexer lexer;


        /* ========================================================
                    FASE 1: ANÁLISIS LÉXICO
           ======================================================== */

        /*
         * Descripción:
         * Esta fase analiza el archivo fuente carácter por carácter
         * utilizando el lexer generado por JFlex.
         *
         * Entrada:
         * Archivo fuente .cmm.
         *
         * Salida:
         * - Tokens reconocidos.
         * - Errores léxicos.
         * - Archivo tokens.txt.
         *
         * Objetivo:
         * Identificar las unidades léxicas del lenguaje.
         */

        System.out.println();

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "                    ANALISIS LEXICO"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println();


        /*
         * --------------------------------------------------------
         *          LECTURA DEL ARCHIVO EN UTF-8
         * --------------------------------------------------------
         *
         * UTF-8 es necesario porque utilizaMOA caracteres
         * especiales como:
         *
         * ¿: :?
         * є: :э
         * ʃ: :ʅ
         * λ
         * θ
         * Σ
         * Ͱ
         * »
         */

        try (
                Reader lector = new InputStreamReader(
                        new FileInputStream(rutaFuente),
                        StandardCharsets.UTF_8
                );

                PrintWriter salida = new PrintWriter(
                        rutaTokens,
                        "UTF-8"
                )
        ) {


            lexer = new Lexer(lector);


            /* ----------------------------------------------------
                        ENCABEZADO DE TOKENS
               ---------------------------------------------------- */

            /*
             * Descripción:
             * Escribe el encabezado del archivo tokens.txt.
             *
             * Salida:
             * Columnas para línea, columna, token y lexema.
             */

            salida.printf(
                    "%-8s %-8s %-18s %s%n",
                    "LINEA",
                    "COLUMNA",
                    "TOKEN",
                    "LEXEMA"
            );

            salida.println("-".repeat(60));


            /* ----------------------------------------------------
                        OBTENER TODOS LOS TOKENS
               ---------------------------------------------------- */

            /*
             * Descripción:
             * Solicita tokens al lexer hasta encontrar EOF.
             *
             * Entrada:
             * Archivo fuente procesado por JFlex.
             *
             * Salida:
             * Cada token se almacena en tokens.txt.
             */

            Symbol token = lexer.next_token();


            while (token.sym != sym.EOF) {


                /*
                 * Busca el nombre del token a partir del número
                 * generado por CUP.
                 */

                String nombre = nombres.getOrDefault(
                        token.sym,
                        "DESCONOCIDO"
                );


                /*
                 * Escribe el token reconocido en tokens.txt.
                 */

                salida.printf(
                        "%-8d %-8d %-18s %s%n",
                        token.left,
                        token.right,
                        nombre,
                        token.value
                );

                totalTokens++;

                /*
                 * Solicita el siguiente token al lexer.
                 */

                token = lexer.next_token();
            }


            /* ----------------------------------------------------
                    GUARDAR ERRORES LÉXICOS
               ---------------------------------------------------- */

            /*
             * Descripción:
             * Obtiene los errores encontrados durante el análisis
             * léxico y los escribe al final de tokens.txt.
             *
             * Entrada:
             * Lista de errores almacenada por el Lexer.
             *
             * Salida:
             * Errores léxicos dentro de tokens.txt.
             */

            List<String> errores = lexer.getErrores();


            if (!errores.isEmpty()) {

                salida.println();

                salida.println(
                        "ERRORES LEXICOS (" +
                                errores.size() +
                                ")"
                );

                salida.println("-".repeat(60));


                for (String error : errores) {

                    salida.println(error);
                }
            }
        }


        /* ========================================================
                    RESUMEN DEL ANÁLISIS LÉXICO
           ======================================================== */

        System.out.println(
                "Tokens reconocidos : " + totalTokens
        );

        System.out.println(
                "Errores léxicos    : " +
                        lexer.getErrores().size()
        );

        System.out.println(
                "Archivo de tokens  : " + rutaTokens
        );


        if (lexer.getErrores().isEmpty()) {

            System.out.println(
                    "Resultado léxico   : sin errores"
            );

        } else {

            System.out.println(
                    "Resultado léxico   : el archivo tiene errores léxicos"
            );
        }


        /* ========================================================
                    FASE 2: ANÁLISIS SINTÁCTICO
           ======================================================== */

        /*
         * Descripción:
         * Esta fase analiza la estructura del programa utilizando
         * el parser generado por Java CUP.
         *
         * Entrada:
         * Archivo fuente .cmm.
         *
         * Salida:
         * Errores sintácticos encontrados.
         *
         * Objetivo:
         * Verificar que los tokens estén organizados de acuerdo
         * con las reglas definidas en Parser.cup.
         */

        System.out.println();

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "                   ANALISIS SINTACTICO"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println();


        /*
         * --------------------------------------------------------
         * CREAR NUEVO LEXER
         * --------------------------------------------------------
         *
         * El primer Lexer ya llegó hasta EOF durante el análisis
         * léxico.
         *
         * Por eso se vuelve a abrir el archivo para que el parser
         * pueda recibir nuevamente los tokens desde el principio.
         */

        Lexer lexerParser;


        try (
                Reader lectorParser = new InputStreamReader(
                        new FileInputStream(rutaFuente),
                        StandardCharsets.UTF_8
                )
        ) {


            lexerParser = new Lexer(lectorParser);


            /* ----------------------------------------------------
                            CREAR PARSER
               ---------------------------------------------------- */

            /*
             * Descripción:
             * Crea una instancia del parser generado por CUP.
             *
             * Entrada:
             * Lexer encargado de proporcionar los tokens.
             *
             * Salida:
             * Parser listo para realizar el análisis sintáctico.
             */

            ParserConErrores parser =
                    new ParserConErrores(lexerParser);


            /* ----------------------------------------------------
                    EJECUTAR ANÁLISIS SINTÁCTICO
               ---------------------------------------------------- */

            try {

                parser.parse();

            } catch (Exception e) {

                /*
                 * Algunos errores pueden provocar una excepción
                 * dependiendo de cómo se comporte el parser.
                 *
                 * No se muestra un stack trace completo para
                 * mantener limpia la salida del compilador.
                 */

                System.out.println();

                System.out.println(
                        "El análisis sintáctico terminó debido a un error."
                );
            }


            /* ----------------------------------------------------
                        RESULTADO DEL PARSER
               ---------------------------------------------------- */

            System.out.println();

            System.out.println(
                    "Errores sintácticos : " +
                            parser.getErroresSintacticos()
            );


            if (parser.getErroresSintacticos() == 0) {

                System.out.println(
                        "Resultado sintáctico: sin errores"
                );

            } else {

                System.out.println(
                        "Resultado sintáctico: el archivo tiene errores sintácticos"
                );
            }
        }


        /* ========================================================
                        RESULTADO FINAL
           ======================================================== */

        /*
         * Muestra un resumen general del análisis realizado.
         */

        System.out.println();

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "                    RESULTADO FINAL"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println();


        System.out.println(
                "Errores léxicos    : " +
                        lexer.getErrores().size()
        );

        System.out.println();

        System.out.println(
                "Análisis terminado."
        );
    }


    /* ============================================================
                    PARSER CON CONTROL DE ERRORES
       ============================================================ */

    /*
     * Descripción:
     * Esta clase extiende el parser generado automáticamente
     * por Java CUP.
     *
     * Su objetivo es controlar y contar los errores sintácticos
     * encontrados durante el análisis.
     *
     * La recuperación sintáctica se realiza mediante la producción:
     *
     * error fin_sentencia
     *
     * definida en Parser.cup.
     *
     * Cuando CUP encuentra un error, utiliza dicha producción
     * para intentar continuar el análisis.
     */

    private static class ParserConErrores extends parser {


        private int erroresSintacticos = 0;


        /* --------------------------------------------------------
                            CONSTRUCTOR
           -------------------------------------------------------- */

        /*
         * Entrada:
         * Lexer que proporciona los tokens al parser.
         *
         * Objetivo:
         * Inicializar el parser generado por CUP.
         */

        public ParserConErrores(Lexer lexer) {

            super(lexer);
        }


        /* --------------------------------------------------------
                        ERROR SINTÁCTICO
           -------------------------------------------------------- */

        /*
         * Descripción:
         * CUP llama este método cuando encuentra un token que
         * no esperaba según la gramática.
         *
         * Entrada:
         * Token que provocó el error.
         *
         * Salida:
         * Información sobre el error sintáctico.
         *
         * La recuperación se realiza mediante la producción
         * "error fin_sentencia" definida en Parser.cup.
         */

        @Override
        public void syntax_error(Symbol cur_token) {


            erroresSintacticos++;


            System.out.println(
                    "Error sintáctico #" +
                            erroresSintacticos
            );


            if (cur_token != null) {


                System.out.println(
                        "  Línea  : " +
                                (cur_token.left + 1)
                );


                System.out.println(
                        "  Columna: " +
                                (cur_token.right + 1)
                );


                String nombreToken =
                        nombreToken(cur_token.sym);


                System.out.println(
                        "  Token  : " +
                                nombreToken
                );


                /*
                 * El lexema solamente se muestra cuando el token
                 * contiene un valor asociado.
                 */

                if (cur_token.value != null) {

                    System.out.println(
                            "  Lexema : " +
                                    cur_token.value
                    );
                }
            }


            System.out.println();
        }


        /* --------------------------------------------------------
                    ERROR REPORTADO POR CUP
           -------------------------------------------------------- */

        /*
         * Descripción:
         * CUP puede utilizar este método para reportar errores.
         *
         * El método syntax_error() ya se encarga de mostrar y
         * contar los errores sintácticos.
         *
         * Por eso no se realiza ninguna acción aquí para evitar
         * mostrar o contar dos veces el mismo error.
         */

        @Override
        public void report_error(
                String message,
                Object info
        ) {

            // El error ya fue mostrado por syntax_error().
        }


        /* --------------------------------------------------------
                            ERROR FATAL
           -------------------------------------------------------- */

        /*
         * Descripción:
         * Un error fatal ocurre cuando CUP no puede recuperarse
         * y continuar con el análisis.
         *
         * Salida:
         * Mensaje indicando que el parser no pudo continuar.
         *
         * Objetivo:
         * Informar al usuario sin mostrar un stack trace completo.
         */

        @Override
        public void report_fatal_error(
                String message,
                Object info
        ) {

            System.out.println(
                    "ERROR SINTACTICO FATAL"
            );

            System.out.println(
                    "  " + message
            );

            System.out.println(
                    "  El parser no pudo continuar."
            );

            System.out.println();
        }


        /* --------------------------------------------------------
                    OBTENER CANTIDAD DE ERRORES
           -------------------------------------------------------- */

        /*
         * Devuelve la cantidad total de errores sintácticos
         * encontrados durante el análisis.
         */

        public int getErroresSintacticos() {

            return erroresSintacticos;
        }


        /* --------------------------------------------------------
                    CONVERTIR TOKEN A NOMBRE
           -------------------------------------------------------- */

        /*
         * Descripción:
         * Convierte el número entero asociado a un token de CUP
         * en el nombre correspondiente definido en la clase sym.
         *
         * Entrada:
         * Número del token.
         *
         * Salida:
         * Nombre del token.
         *
         * Ejemplo:
         *
         * 52 -> ID
         */

        private String nombreToken(int numero) {


            try {

                for (Field campo : sym.class.getFields()) {


                    if (campo.getType() == int.class
                            && Modifier.isStatic(
                                    campo.getModifiers())
                            && campo.getInt(null) == numero) {

                        return campo.getName();
                    }
                }

            } catch (Exception e) {

                return "DESCONOCIDO";
            }


            return "DESCONOCIDO";
        }
    }


    /* ============================================================
                        MAPA DE TOKENS
       ============================================================ */

    /*
     * Descripción:
     * Recorre la clase sym generada por CUP y construye un mapa
     * que relaciona el número de cada token con su nombre.
     *
     * Entrada:
     * Constantes enteras generadas por Java CUP en sym.java.
     *
     * Salida:
     * Mapa con la siguiente estructura:
     *
     * número -> nombre del token
     *
     * Ejemplo:
     *
     * 52 -> ID
     * 53 -> LITERAL_INT
     */

    private static Map<Integer, String> nombresDeTokens()
            throws IllegalAccessException {


        Map<Integer, String> mapa = new HashMap<>();


        for (Field campo : sym.class.getFields()) {


            if (campo.getType() == int.class
                    && Modifier.isStatic(
                            campo.getModifiers())) {


                mapa.put(
                        campo.getInt(null),
                        campo.getName()
                );
            }
        }


        return mapa;
    }
}