import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java_cup.runtime.Symbol;

/* ============================================================
 *                    ANALIZADOR DEL LENGUAJE
 * ============================================================
 *
 * Programa principal del compilador.
 *
 * FASE 1: Análisis léxico     (genera tokens.txt).
 * FASE 2: Análisis sintáctico (parser generado con Java CUP).
 *
 * Uso: java Main <archivo_fuente> [archivo_tokens]
 * ============================================================
 */

public class Main {

    /* ========================================================
     *                    CONFIGURACIÓN
     * ========================================================
     */

    private static final int ANCHO = 60;

    private static final String LINEA = "=".repeat(ANCHO);

    private static final String SEPARADOR = "-".repeat(ANCHO);

    /* "Error léxico en línea 59, columna 10: detalle" */
    private static final Pattern PATRON_ERROR = Pattern.compile(
            "Error léxico en línea (\\d+), columna (\\d+): (.*)");

    private static int totalTokens = 0;


    /* ========================================================
     *                 MÉTODOS DE PRESENTACIÓN
     * ========================================================
     */

    private static String centrar(String texto) {
        int espacios = Math.max(0, (ANCHO - texto.length()) / 2);
        return " ".repeat(espacios) + texto;
    }

    /** Título grande centrado entre líneas dobles. */
    private static void titulo(String texto) {
        System.out.println();
        System.out.println(LINEA);
        System.out.println(centrar(texto));
        System.out.println(LINEA);
    }

    /** Encabezado de sección centrado con separador. */
    private static void encabezado(String texto) {
        System.out.println();
        System.out.println(centrar(texto));
        System.out.println(SEPARADOR);
    }

    /** Línea de resumen alineada: "  Etiqueta ........ : valor". */
    private static void resumen(String etiqueta, Object valor) {
        System.out.printf("  %-22s : %s%n", etiqueta, valor);
    }

    /** Muestra una ruta en su propia línea, indentada. */
    private static void ruta(String etiqueta, String valor) {
        System.out.println("  " + etiqueta);
        System.out.println("    " + valor);
    }

    /**
     * Campo alineado con ajuste de línea automático:
     *
     *     Ubicación : Línea 34, Columna 16
     *     Detalle   : Texto largo que se acomoda
     *                 con sangría bajo el valor.
     */
    private static void campo(String etiqueta, String valor) {
        String prefijo = String.format("    %-10s : ", etiqueta);
        String sangria = " ".repeat(prefijo.length());
        int ancho = ANCHO - prefijo.length();

        StringBuilder linea = new StringBuilder();
        boolean primera = true;

        for (String palabra : valor.split(" ")) {
            if (linea.length() > 0
                    && linea.length() + 1 + palabra.length() > ancho) {
                System.out.println((primera ? prefijo : sangria) + linea);
                primera = false;
                linea.setLength(0);
            }
            if (linea.length() > 0) {
                linea.append(' ');
            }
            linea.append(palabra);
        }
        System.out.println((primera ? prefijo : sangria) + linea);
    }

    private static void mostrarEstado(boolean correcto, String mensaje) {
        System.out.println();
        System.out.println("  Estado : " + (correcto ? "✓ " : "✗ ") + mensaje);
    }


    /* ========================================================
     *                         MAIN
     * ========================================================
     */

    public static void main(String[] args) throws Exception {

        /* Salida en UTF-8 (tildes, ✓, ✗). */
        System.setOut(new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, "UTF-8"));

        if (args.length < 1) {
            titulo("USO DEL ANALIZADOR");
            System.out.println();
            System.out.println(
                    "  java Main <archivo_fuente> [archivo_tokens]");
            System.out.println();
            return;
        }

        File archivoFuente = new File(args[0]);
        File archivoTokens = new File(args.length > 1 ? args[1] : "tokens.txt");

        String rutaFuente = archivoFuente.getCanonicalPath();
        String rutaTokens = archivoTokens.getCanonicalPath();

        Map<Integer, String> nombres = nombresDeTokens();


        /* ----------------- ENCABEZADO ----------------- */

        titulo("ANÁLISIS DEL ARCHIVO");
        System.out.println();
        ruta("Archivo fuente:", rutaFuente);
        System.out.println();
        ruta("Archivo tokens:", rutaTokens);


        /* ----------------- FASE 1: LÉXICO ----------------- */

        titulo("ANÁLISIS LÉXICO");

        List<String> erroresLexicos =
                analisisLexico(archivoFuente, archivoTokens, nombres);

        encabezado("RESUMEN LÉXICO");
        resumen("Tokens reconocidos", totalTokens);
        resumen("Errores léxicos", erroresLexicos.size());

        if (erroresLexicos.isEmpty()) {
            mostrarEstado(true, "ANÁLISIS LÉXICO CORRECTO");
        } else {
            mostrarTablaErrores(erroresLexicos);
            mostrarEstado(false, "EL ARCHIVO CONTIENE ERRORES LÉXICOS");
        }


        /* ----------------- FASE 2: SINTÁCTICO ----------------- */

        titulo("ANÁLISIS SINTÁCTICO");

        int erroresSintacticos = analisisSintactico(archivoFuente);

        encabezado("RESUMEN SINTÁCTICO");
        resumen("Errores sintácticos", erroresSintacticos);

        if (erroresSintacticos == 0) {
            mostrarEstado(true, "ANÁLISIS SINTÁCTICO CORRECTO");
        } else {
            mostrarEstado(false, "EL ARCHIVO CONTIENE ERRORES SINTÁCTICOS");
        }


        /* ----------------- RESULTADO FINAL ----------------- */

        titulo("RESULTADO FINAL");
        System.out.println();
        resumen("Errores léxicos", erroresLexicos.size());
        resumen("Errores sintácticos", erroresSintacticos);
        System.out.println();

        if (erroresLexicos.isEmpty() && erroresSintacticos == 0) {
            System.out.println("  ✓ ARCHIVO CORRECTO");
            System.out.println();
            System.out.println("  No se encontraron errores léxicos");
            System.out.println("  ni sintácticos.");
        } else {
            System.out.println("  ✗ ARCHIVO CON ERRORES");
            System.out.println();
            System.out.println("  Corrige los errores indicados antes de");
            System.out.println("  considerar válido el archivo.");
        }

        System.out.println();
        System.out.println(LINEA);
        System.out.println();
    }


    /* ========================================================
     *                    FASE 1: LÉXICO
     * ========================================================
     */

    private static List<String> analisisLexico(
            File fuente,
            File tokens,
            Map<Integer, String> nombres) throws Exception {

        try (
                Reader lector = new InputStreamReader(
                        new FileInputStream(fuente),
                        StandardCharsets.UTF_8);

                PrintWriter salida = new PrintWriter(tokens, "UTF-8")
        ) {

            Lexer lexer = new Lexer(lector);

            salida.printf("%-8s %-8s %-18s %s%n",
                    "LINEA", "COLUMNA", "TOKEN", "LEXEMA");
            salida.println("-".repeat(ANCHO));

            totalTokens = 0;

            Symbol token = lexer.next_token();

            while (token.sym != sym.EOF) {

                salida.printf("%-8d %-8d %-18s %s%n",
                        token.left,
                        token.right,
                        nombres.getOrDefault(token.sym, "DESCONOCIDO"),
                        token.value);

                totalTokens++;
                token = lexer.next_token();
            }

            List<String> errores = lexer.getErrores();

            if (!errores.isEmpty()) {
                salida.println();
                salida.println("ERRORES LEXICOS (" + errores.size() + ")");
                salida.println("-".repeat(ANCHO));

                for (String error : errores) {
                    salida.println(error);
                }
            }

            return errores;
        }
    }


    /**
     * Muestra los errores léxicos en una tabla alineada:
     *
     *    #    LÍNEA   COL   DESCRIPCIÓN
     */
    private static void mostrarTablaErrores(List<String> errores) {

        System.out.println();
        System.out.printf("  %-4s %6s %5s   %s%n",
                "#", "LÍNEA", "COL", "DESCRIPCIÓN");
        System.out.println("  " + "-".repeat(ANCHO - 2));

        int numero = 1;

        for (String error : errores) {

            String texto = error == null
                    ? ""
                    : error.trim().replace("\r", " ").replace("\n", " ");

            Matcher m = PATRON_ERROR.matcher(texto);

            if (m.matches()) {
                System.out.printf("  %-4s %6s %5s   %s%n",
                        String.format("%02d", numero),
                        m.group(1),
                        m.group(2),
                        m.group(3));
            } else {
                System.out.printf("  %-4s %6s %5s   %s%n",
                        String.format("%02d", numero), "-", "-", texto);
            }

            numero++;
        }
    }


    /* ========================================================
     *                  FASE 2: SINTÁCTICO
     * ========================================================
     */

    private static int analisisSintactico(File fuente) throws Exception {

        try (Reader lector = new InputStreamReader(
                new FileInputStream(fuente), StandardCharsets.UTF_8)) {

            ParserConErrores parser =
                    new ParserConErrores(new Lexer(lector));

            try {
                parser.parse();
            } catch (Exception e) {
                if (!parser.errorFatalMostrado) {
                    System.out.println();
                    System.out.println(
                            "  El análisis terminó por un problema");
                    System.out.println("  durante el procesamiento.");
                }
            }

            return parser.erroresSintacticos;
        }
    }


    /* ========================================================
     *              PARSER CON CONTROL DE ERRORES
     * ========================================================
     */

    private static class ParserConErrores extends parser {

        private int erroresSintacticos = 0;

        private Symbol ultimoToken = null;

        private boolean errorFatalMostrado = false;

        public ParserConErrores(Lexer lexer) {
            super(lexer);
        }

        @Override
        public void syntax_error(Symbol cur_token) {

            erroresSintacticos++;
            ultimoToken = cur_token;

            System.out.println();
            System.out.println("  [Error sintáctico #"
                    + String.format("%02d", erroresSintacticos) + "]");

            if (cur_token == null) {
                return;
            }

            /*
             * El Lexer ya entrega línea y columna con base 1,
             * por eso aquí NO se les suma 1.
             */
            String ubicacion = cur_token.left > 0
                    ? "Línea " + cur_token.left
                            + ", Columna " + cur_token.right
                    : "Final del archivo";

            campo("Ubicación", ubicacion);

            if (cur_token.sym == sym.EOF) {

                campo("Detalle",
                        "Se llegó al final del archivo y aún se esperaban "
                        + "más elementos. Puede faltar un cierre o una "
                        + "sentencia.");

            } else {

                campo("Token", nombreToken(cur_token.sym));

                if (cur_token.value != null) {
                    campo("Lexema", String.valueOf(cur_token.value));
                }

                campo("Detalle",
                        "Elemento inesperado en esta posición según "
                        + "las reglas de la gramática.");
            }
        }

        /* El error ya se muestra en syntax_error(). */
        @Override
        public void report_error(String message, Object info) {
        }

        @Override
        public void report_fatal_error(String message, Object info) {

            errorFatalMostrado = true;

            System.out.println();
            System.out.println("  " + SEPARADOR.substring(2));
            System.out.println("  ✗ ERROR FATAL: el análisis se detuvo,");
            System.out.println("    no fue posible recuperarse del error.");
        }

        private static String nombreToken(int numero) {
            try {
                for (Field campo : sym.class.getFields()) {
                    if (campo.getType() == int.class
                            && Modifier.isStatic(campo.getModifiers())
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


    /* ========================================================
     *                    MAPA DE TOKENS
     * ========================================================
     */

    private static Map<Integer, String> nombresDeTokens()
            throws IllegalAccessException {

        Map<Integer, String> mapa = new HashMap<>();

        for (Field campo : sym.class.getFields()) {
            if (campo.getType() == int.class
                    && Modifier.isStatic(campo.getModifiers())) {
                mapa.put(campo.getInt(null), campo.getName());
            }
        }

        return mapa;
    }
}