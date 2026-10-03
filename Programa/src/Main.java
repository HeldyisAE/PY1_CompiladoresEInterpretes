import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java_cup.runtime.Symbol;

/* ============================================================
 *                 ANALIZADOR DEL LENGUAJE .cmm
 * ============================================================
 *
 * FASE 1: Análisis léxico     (genera tokens.txt).
 * FASE 2: Análisis sintáctico (parser generado con Java CUP).
 *
 * Uso: java Main <archivo_fuente> [archivo_tokens]
 *
 * Nota: durante el análisis se silencia la salida del Lexer y
 * del Parser; todos los resultados se muestran aquí, una sola
 * vez y con un formato uniforme.
 * ============================================================
 */

public class Main {

    /* ========================================================
     *                    CONFIGURACIÓN
     * ========================================================
     */

    private static final int ANCHO = 76;

    private static final Pattern PATRON_ERROR = Pattern.compile(
            "Error léxico en línea (\\d+), columna (\\d+): (.*)");

    /* Nombre de token -> cómo se escribe en el lenguaje. */
    private static final Map<String, String> VISIBLE = new HashMap<>();

    static {
        VISIBLE.put("PLUS", "+");
        VISIBLE.put("MINUS", "-");
        VISIBLE.put("MULTIPLY", "*");
        VISIBLE.put("ENTIREDIV", "//");
        VISIBLE.put("FLOATDIV", "/");
        VISIBLE.put("MOD", "mod");
        VISIBLE.put("POT", "pot");
        VISIBLE.put("INCREMENT", "++");
        VISIBLE.put("DECREMENT", "--");
        VISIBLE.put("LTE", "<=");
        VISIBLE.put("GTE", ">=");
        VISIBLE.put("EQUAL", "==");
        VISIBLE.put("NEQ", "!=");
        VISIBLE.put("LT", "<");
        VISIBLE.put("GT", ">");
        VISIBLE.put("AND", "λ");
        VISIBLE.put("OR", "θ");
        VISIBLE.put("NOT", "Σ");
        VISIBLE.put("OPBLOCK", "¿:");
        VISIBLE.put("CLBLOCK", ":?");
        VISIBLE.put("PARENOP", "є:");
        VISIBLE.put("PARENCL", ":э");
        VISIBLE.put("OPBRACKET", "ʃ:");
        VISIBLE.put("CLBRACKET", ":ʅ");
        VISIBLE.put("TERMINATOR", "»");
        VISIBLE.put("ASIGN", "Ͱ");
        VISIBLE.put("COMA", ",");
        VISIBLE.put("ID", "identificador");
        VISIBLE.put("LITERAL_INT", "número entero");
        VISIBLE.put("LITERAL_FLOAT", "número decimal");
        VISIBLE.put("LITERAL_CHAR", "carácter");
        VISIBLE.put("LITERAL_STRING", "cadena de texto");
        VISIBLE.put("EOF", "fin de archivo");
    }

    private static PrintStream consola;

    private static int totalTokens = 0;

    /* Colores ANSI: solo en terminales que los soportan. */
    private static final boolean COLOR = detectarColor();

    /*
     * Los colores están activados por defecto.
     * Para desactivarlos: definir la variable de entorno NO_COLOR
     * (por ejemplo, "set NO_COLOR=1" antes de ejecutar).
     */
    private static boolean detectarColor() {

        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        String so = System.getProperty("os.name", "").toLowerCase();

        if (so.contains("win")) {
            habilitarAnsiWindows();
            return true;
        }

        String term = System.getenv("TERM");

        return term != null && !term.equals("dumb");
    }

    /*
     * Java no activa por sí solo los códigos ANSI en la consola de
     * Windows. Al lanzar cmd.exe compartiendo la misma consola,
     * Windows 10/11 activa el modo de secuencias de escape para ella.
     */
    private static void habilitarAnsiWindows() {
        try {
            new ProcessBuilder("cmd", "/c", "rem")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (Exception e) {
            /* Si falla, simplemente no se verán colores. */
        }
    }

    private static String col(String codigo, String texto) {
        return COLOR ? "\u001B[" + codigo + "m" + texto + "\u001B[0m" : texto;
    }


    /* ========================================================
     *                 MÉTODOS DE PRESENTACIÓN
     * ========================================================
     */

    /** Cuadro doble centrado, para el título del programa. */
    private static void banner(String texto) {
        int libre = ANCHO - 2 - texto.length();
        int izq = Math.max(0, libre / 2);
        int der = Math.max(0, libre - izq);

        System.out.println();
        System.out.println(col("1;36", "╔" + "═".repeat(ANCHO - 2) + "╗"));
        System.out.println(col("1;36", "║" + " ".repeat(izq) + texto
                + " ".repeat(der) + "║"));
        System.out.println(col("1;36", "╚" + "═".repeat(ANCHO - 2) + "╝"));
    }

    /** Encabezado de sección: ━━ TÍTULO ━━━━━━━━━━━━━━━━━━━━━ */
    private static void seccion(String texto) {
        String cabecera = "━━ " + texto + " ";

        System.out.println();
        System.out.println(col("1;36", cabecera
                + "━".repeat(Math.max(3, ANCHO - cabecera.length()))));
        System.out.println();
    }

    /** Cuadro simple con una o varias líneas de texto. */
    private static void caja(boolean correcto, String... lineas) {

        String c = correcto ? "1;32" : "1;31";

        System.out.println(col(c, "┌" + "─".repeat(ANCHO - 2) + "┐"));
        for (String l : lineas) {
            System.out.println(col(c,
                    String.format("│ %-" + (ANCHO - 4) + "s │", l)));
        }
        System.out.println(col(c, "└" + "─".repeat(ANCHO - 2) + "┘"));
    }

    /** Par "etiqueta : valor" alineado. */
    private static void kv(String etiqueta, Object valor) {
        System.out.printf("  %-24s : %s%n", etiqueta, valor);
    }

    /** Contador alineado a la derecha. */
    private static void contador(String etiqueta, int valor) {

        String numero = String.format("%5d", valor);

        String color = etiqueta.startsWith("Errores")
                ? (valor > 0 ? "1;31" : "1;32")
                : "1;36";

        System.out.println(String.format("  %-24s : ", etiqueta)
                + col(color, numero));
    }

    private static void estado(boolean correcto, String mensaje) {
        System.out.println();
        System.out.println(col(correcto ? "1;32" : "1;31",
                "  " + (correcto ? " " : " ") + mensaje));
    }

    /** Ajusta un texto al ancho de la consola con sangría fija. */
    private static void envolver(String primero, String resto, String texto) {

        int ancho = ANCHO - primero.length();
        StringBuilder linea = new StringBuilder();
        boolean esPrimera = true;

        for (String palabra : texto.split(" ")) {

            if (linea.length() > 0
                    && linea.length() + 1 + palabra.length() > ancho) {

                System.out.println((esPrimera ? primero : resto) + linea);
                esPrimera = false;
                linea.setLength(0);
            }

            if (linea.length() > 0) {
                linea.append(' ');
            }

            linea.append(palabra);
        }

        System.out.println((esPrimera ? primero : resto) + linea);
    }

    /** Divide un texto en líneas de un ancho máximo. */
    private static List<String> ajustar(String texto, int ancho) {

        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (String palabra : texto.split(" ")) {

            if (actual.length() > 0
                    && actual.length() + 1 + palabra.length() > ancho) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }

            if (actual.length() > 0) {
                actual.append(' ');
            }

            actual.append(palabra);
        }

        lineas.add(actual.toString());

        return lineas;
    }

    /** Nota informativa con icono. */
    private static void nota(String texto) {

        List<String> partes = ajustar(texto, ANCHO - 4);

        for (int i = 0; i < partes.size(); i++) {
            System.out.println(
                    (i == 0 ? col("1;36", "  ℹ ") : "    ") + partes.get(i));
        }
    }

    /**
     * Fila dentro de un cuadro de error. Recibe el texto sin color
     * (para calcular el relleno) y el texto con color (para imprimir).
     */
    private static void fila(String plano, String coloreado) {

        int relleno = Math.max(0, ANCHO - 4 - plano.length());
        String borde = col("31", "│");

        System.out.println(borde + " " + coloreado
                + " ".repeat(relleno) + " " + borde);
    }

    /** Fila "Etiqueta : valor" dentro del cuadro, con ajuste de línea. */
    private static void detalle(String etiqueta, String valor, String color) {

        String prefijo = String.format("%-11s : ", etiqueta);
        List<String> partes = ajustar(valor, ANCHO - 4 - prefijo.length());

        for (int i = 0; i < partes.size(); i++) {

            String p = i == 0 ? prefijo : " ".repeat(prefijo.length());

            fila(p + partes.get(i),
                    (i == 0 ? col("1", prefijo) : p)
                            + col(color, partes.get(i)));
        }
    }

    /** Campo del detalle de un error sintáctico. */
    private static void campo(String etiqueta, String valor) {
        envolver(
                String.format("     %-11s : ", etiqueta),
                " ".repeat(19),
                valor);
    }

    /** Ruta relativa a la carpeta actual, para no mostrar rutas enormes. */
    private static String rutaCorta(File f) {
        try {
            Path base = Paths.get("").toAbsolutePath().normalize();
            Path ruta = f.getCanonicalFile().toPath();

            if (ruta.startsWith(base)) {
                return base.relativize(ruta).toString();
            }

            return ruta.toString();

        } catch (Exception e) {
            return f.getName();
        }
    }


    /* ========================================================
     *                         MAIN
     * ========================================================
     */

    public static void main(String[] args) throws Exception {

        long inicio = System.nanoTime();

        consola = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, "UTF-8");

        System.setOut(consola);

        if (args.length < 1) {
            banner("ANALIZADOR · USO");
            System.out.println();
            System.out.println(
                    "  java Main <archivo_fuente> [archivo_tokens]");
            System.out.println();
            return;
        }

        File archivoFuente = new File(args[0]);
        File archivoTokens = new File(args.length > 1 ? args[1] : "tokens.txt");

        Map<Integer, String> nombres = nombresDeTokens();


        /* ----------------- ENCABEZADO ----------------- */

        banner("ANALIZADOR LÉXICO Y SINTÁCTICO ");
        System.out.println();
        kv("Archivo fuente", rutaCorta(archivoFuente));
        kv("Archivo de tokens", rutaCorta(archivoTokens));


        /* ----------------- FASE 1: LÉXICO ----------------- */

        seccion("FASE 1 · ANÁLISIS LÉXICO");

        List<String> erroresLexicos =
                analisisLexico(archivoFuente, archivoTokens, nombres);

        contador("Tokens reconocidos", totalTokens);
        contador("Errores léxicos", erroresLexicos.size());

        if (erroresLexicos.isEmpty()) {
            estado(true, "Análisis léxico correcto.");
        } else {
            mostrarTablaLexica(erroresLexicos);
            estado(false, "El archivo contiene errores léxicos.");
        }


        /* ----------------- FASE 2: SINTÁCTICO ----------------- */

        seccion("FASE 2 · ANÁLISIS SINTÁCTICO");

        ParserConErrores analizador = analisisSintactico(archivoFuente);

        List<String> lineasFuente = leerLineas(archivoFuente);

        int numero = 1;

        for (ErrorSintactico e : analizador.errores) {
            mostrarErrorSintactico(numero++, e, lineasFuente);
        }

        if (analizador.errores.isEmpty()) {

            if (analizador.abortado) {
                System.out.println("  ✗ El análisis se interrumpió de forma");
                System.out.println("    inesperada antes de terminar.");
            } else {
                estado(true, "Análisis sintáctico correcto.");
            }

        } else {

            estado(false, "El archivo contiene errores sintácticos.");
        }


        /* ----------------- RESULTADO FINAL ----------------- */

        seccion("RESULTADO FINAL");

        contador("Tokens reconocidos", totalTokens);
        contador("Errores léxicos", erroresLexicos.size());
        contador("Errores sintácticos", analizador.errores.size());

        int erroresTotales =
                erroresLexicos.size() + analizador.errores.size();

        System.out.println("  " + "─".repeat(24) + " : " + "─".repeat(5));
        contador("Errores totales", erroresTotales);

        long ms = (System.nanoTime() - inicio) / 1_000_000;

        String tiempo = ms < 1000
                ? ms + " ms"
                : (ms / 1000) + "." + String.format("%03d", ms % 1000) + " s";

        System.out.println(String.format("  %-24s : ", "Tiempo de ejecución")
                + col("1;36", String.format("%5s", tiempo)));
        System.out.println();

        if (erroresLexicos.isEmpty() && analizador.errores.isEmpty()) {
            caja(true, "✓  ARCHIVO CORRECTO",
                 "No se encontraron errores léxicos ni sintácticos.");
        } else {
            caja(false, "✗  ARCHIVO CON ERRORES",
                 "Corrige los errores indicados y vuelve a ejecutar.");
        }

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

        /* Se silencia lo que el Lexer imprima por su cuenta. */
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        try (
                Reader lector = new InputStreamReader(
                        new FileInputStream(fuente),
                        StandardCharsets.UTF_8);

                PrintWriter salida = new PrintWriter(tokens, "UTF-8")
        ) {

            Lexer lexer = new Lexer(lector);

            salida.printf("%-8s %-8s %-18s %s%n",
                    "LINEA", "COLUMNA", "TOKEN", "LEXEMA");
            salida.println("-".repeat(60));

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
                salida.println("-".repeat(60));

                for (String error : errores) {
                    salida.println(error);
                }
            }

            return errores;

        } finally {
            System.setOut(original);
        }
    }

    /**
     * Tabla de errores léxicos:
     *
     *   Nº    LÍNEA   COL   DESCRIPCIÓN
     */
    private static void mostrarTablaLexica(List<String> errores) {

        System.out.println();
        System.out.println(col("1;36", String.format("  %-4s %6s %5s   %s",
                "Nº", "LÍNEA", "COL", "DESCRIPCIÓN")));
        System.out.println(col("90", "  " + "─".repeat(4) + " "
                + "─".repeat(6) + " " + "─".repeat(5) + "   "
                + "─".repeat(ANCHO - 24)));

        int numero = 1;

        for (String error : errores) {

            String texto = error == null
                    ? ""
                    : error.trim().replace("\r", " ").replace("\n", " ");

            Matcher m = PATRON_ERROR.matcher(texto);

            String linea = "-";
            String columna = "-";
            String detalle = texto;

            if (m.matches()) {
                linea = m.group(1);
                columna = m.group(2);
                detalle = m.group(3);
            }

            String num = String.format("%02d", numero);

            String planoPrefijo = String.format("  %-4s %6s %5s   ",
                    num, linea, columna);

            String colorPrefijo = "  "
                    + col("90", String.format("%-4s", num)) + " "
                    + col("33", String.format("%6s", linea)) + " "
                    + col("33", String.format("%5s", columna)) + "   ";

            List<String> partes =
                    ajustar(detalle, ANCHO - planoPrefijo.length());

            for (int i = 0; i < partes.size(); i++) {
                System.out.println(
                        (i == 0
                                ? colorPrefijo
                                : " ".repeat(planoPrefijo.length()))
                        + partes.get(i));
            }

            numero++;
        }
    }


    /* ========================================================
     *                  FASE 2: SINTÁCTICO
     * ========================================================
     */

    private static ParserConErrores analisisSintactico(File fuente)
            throws Exception {

        /* Se silencia lo que el Lexer imprima mientras el parser lee. */
        PrintStream original = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        ParserConErrores analizador;

        try (Reader lector = new InputStreamReader(
                new FileInputStream(fuente), StandardCharsets.UTF_8)) {

            analizador = new ParserConErrores(new Lexer(lector));

            try {
                analizador.parse();
            } catch (Exception e) {
                analizador.abortado = true;
            }

        } finally {
            System.setOut(original);
        }

        return analizador;
    }

    private static List<String> leerLineas(File fuente) {
        try {
            return Files.readAllLines(
                    fuente.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Muestra un error sintáctico dentro de su propio cuadro:
     *
     *   ┌─ ERROR 01 · línea 33, columna 15 ───────────────┐
     *   │                                                 │
     *   │  33 │ val int op9 Ͱ +10»                        │
     *   │     │               ^                           │
     *   │                                                 │
     *   │ Se encontró : '+'  (PLUS)                       │
     *   │ Se esperaba : -, Σ, є:, ...                     │
     *   └─────────────────────────────────────────────────┘
     */
    private static void mostrarErrorSintactico(
            int numero,
            ErrorSintactico e,
            List<String> lineas) {

        final int W = ANCHO - 4;

        String ubicacion = e.linea > 0
                ? "línea " + e.linea + ", columna " + e.columna
                : "final del archivo";

        String titulo = " ERROR " + String.format("%02d", numero)
                + " · " + ubicacion + " ";

        /* ---- Borde superior con título ---- */

        System.out.println(
                col("31", "┌─")
                + col("1;31", titulo)
                + col("31", "─".repeat(
                        Math.max(1, ANCHO - 3 - titulo.length())) + "┐"));

        fila("", "");


        /* ---- Línea de código con indicador ---- */

        if (e.linea >= 1 && e.linea <= lineas.size()) {

            int gw = Math.max(3, String.valueOf(e.linea).length());

            String texto = lineas.get(e.linea - 1)
                    .replace("\t", " ")
                    .replaceAll("\\s+$", "");

            int disp = W - gw - 3;
            int pos = Math.max(0, e.columna - 1);
            int inicio = 0;

            /* Si la línea es muy larga, se muestra la zona del error. */
            if (texto.length() > disp) {
                inicio = Math.max(0,
                        Math.min(pos - disp / 3, texto.length() - disp));
                texto = texto.substring(inicio,
                        Math.min(texto.length(), inicio + disp));
            }

            String num = String.format("%" + gw + "d", e.linea);

            fila(num + " │ " + texto,
                    col("90", num + " │ ") + texto);

            if (!e.fin && e.columna > 0) {

                String lexema = e.lexema == null ? "" : e.lexema;

                int c = Math.max(0, pos - inicio);
                int largo = Math.max(1,
                        Math.min(Math.max(1, lexema.length()), disp - c));

                String guia = " ".repeat(gw) + " │ ";

                fila(guia + " ".repeat(c) + "^".repeat(largo),
                        col("90", guia) + " ".repeat(c)
                                + col("1;31", "^".repeat(largo)));
            }

            fila("", "");
        }


        /* ---- Qué se encontró ---- */

        if (e.fin) {

            detalle("Se encontró", "fin de archivo", "1;33");

        } else {

            String lexema = e.lexema == null ? "" : e.lexema;

            if (lexema.length() > 40) {
                lexema = lexema.substring(0, 39) + "…";
            }

            String hallado = "'" + lexema + "'";
            String token = "(" + e.token + ")";
            String prefijo = String.format("%-11s : ", "Se encontró");

            fila(prefijo + hallado + "  " + token,
                    col("1", prefijo)
                            + col("1;33", hallado)
                            + "  " + col("90", token));
        }


        /* ---- Qué se esperaba (máximo 6 opciones) ---- */

        if (!e.esperados.isEmpty()) {

            List<String> lista = new ArrayList<>(e.esperados);
            boolean hayMas = lista.remove("…");

            if (lista.size() > 6) {
                lista = new ArrayList<>(lista.subList(0, 6));
                hayMas = true;
            }

            detalle("Se esperaba",
                    String.join(", ", lista) + (hayMas ? ", …" : ""),
                    "32");
        }

        if (e.fin) {
            detalle("Sugerencia",
                    "Puede faltar un cierre de bloque o un terminador.",
                    "36");
        }


        /* ---- Borde inferior y espacio entre errores ---- */

        fila("", "");

        System.out.println(
                col("31", "└" + "─".repeat(ANCHO - 2) + "┘"));

        System.out.println();
    }


    /* ========================================================
     *              DATOS DE UN ERROR SINTÁCTICO
     * ========================================================
     */

    private static class ErrorSintactico {
        int linea;
        int columna;
        boolean fin;
        String token;
        String lexema;
        List<String> esperados = new ArrayList<>();
    }


    /* ========================================================
     *              PARSER CON CONTROL DE ERRORES
     * ========================================================
     *
     * No imprime nada: guarda los errores para que Main los
     * muestre con el formato uniforme del programa.
     */

    private static class ParserConErrores extends parser {

        private final List<ErrorSintactico> errores = new ArrayList<>();

        private boolean abortado = false;

        private Symbol ultimoReal = null;

        public ParserConErrores(Lexer lexer) {
            super(lexer);
        }

        /*
         * Tokens que deben analizarse bien después de un error para
         * dar la recuperación por válida (CUP usa 3 por defecto).
         * Con 1 se reanuda de inmediato en el token de sincronización.
         */
        @Override
        protected int error_sync_size() {
            return 1;
        }

        /* Recuerda el último token real, para ubicar errores en EOF. */
        @Override
        public Symbol scan() throws Exception {

            Symbol s = super.scan();

            if (s != null && s.sym != sym.EOF) {
                ultimoReal = s;
            }

            return s;
        }

        @Override
        public void syntax_error(Symbol t) {

            ErrorSintactico e = new ErrorSintactico();

            if (t != null) {

                e.fin = t.sym == sym.EOF;
                e.token = nombreToken(t.sym);
                e.lexema = t.value == null ? null : String.valueOf(t.value);

                /* El Lexer ya entrega línea y columna con base 1. */
                if (t.left > 0) {
                    e.linea = t.left;
                    e.columna = t.right;

                } else if (e.fin && ultimoReal != null) {
                    e.linea = ultimoReal.left;
                    e.columna = 0;
                }
            }

            e.esperados = esperados();

            errores.add(e);
        }

        /* Los errores se guardan en syntax_error(). */
        @Override
        public void report_error(String message, Object info) {
        }

        /* No se imprime: Main explica la interrupción. */
        @Override
        public void report_fatal_error(String message, Object info) {
            abortado = true;
        }

        /** Tokens que la gramática aceptaba en este punto. */
        private List<String> esperados() {

            Set<String> lista = new LinkedHashSet<>();

            try {
                for (int id : expected_token_ids()) {

                    if (id == 1) {
                        continue;   /* símbolo interno "error" */
                    }

                    lista.add(legible(nombreToken(id)));
                }
            } catch (Throwable ex) {
                return new ArrayList<>();
            }

            List<String> resultado = new ArrayList<>(lista);

            if (resultado.size() > 10) {
                resultado = new ArrayList<>(resultado.subList(0, 10));
                resultado.add("…");
            }

            return resultado;
        }
    }


    /* ========================================================
     *                    UTILIDADES DE TOKENS
     * ========================================================
     */

    /** Cómo se escribe un token en el lenguaje (o su nombre). */
    private static String legible(String nombre) {
        return VISIBLE.getOrDefault(nombre, nombre.toLowerCase());
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