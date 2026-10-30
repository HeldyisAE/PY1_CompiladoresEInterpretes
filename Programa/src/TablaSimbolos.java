import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * ============================================================
 *                    TABLA DE SÍMBOLOS
 * ============================================================
 *
 * Descripcion:
 * Implemento la tabla de símbolos con una tabla por ámbito,
 * encadenada al ámbito que la contiene (Aho et al., sección 2.7).
 * Este archivo contiene tres clases:
 *   TablaSimbolos  -> administra la cadena de ámbitos (variable top)
 *   Simbolo        -> una entrada de la tabla          (clase Symbol)
 *   Ambito         -> una tabla por ámbito              (clase Env)
 *
 * Cada símbolo guarda:
 *   1. Información básica : nombre y categoría.
 *   2. Atributos de tipo  : tipo, tamaño en bytes, dimensiones de
 *                           arreglos y estructura de parámetros
 *                           (cantidad, tipos, retorno y paso).
 *   3. Ubicación          : dirección (desplazamiento) y ámbito.
 *   4. Adicionales        : valor inicial, control de acceso y
 *                           referencia a la tabla hija (funciones).
 *
 * Entrada:
 * Las acciones semánticas de la gramática (parser.cup) llaman a
 * estos métodos mientras el parser reconoce el programa:
 *   ¿:  -> abrirAmbito()      :?  -> cerrarAmbito()
 *   val tipo id               -> declararVariable / declararArreglo
 *   tipo id є: params :э      -> declararFuncion
 *   uso de id                 -> usar / usarArreglo / usarFuncion
 *
 * Salida:
 * Los ámbitos con sus símbolos y los errores semánticos encontrados.
 *
 * Restricciones:
 * Un identificador no puede declararse dos veces en el mismo ámbito
 * y debe estar declarado antes de usarse.
 *
 * Objetivo:
 * Pasar la información de las declaraciones a los usos.
 */
public class TablaSimbolos {

    /*
     * Tamaño en bytes de cada tipo.
     * string se guarda como una referencia a la cadena.
     */
    private static final Map<String, Integer> BYTES = new HashMap<>();

    static {
        BYTES.put("int", 4);
        BYTES.put("float", 4);
        BYTES.put("bool", 1);
        BYTES.put("char", 1);
        BYTES.put("string", 4);
        BYTES.put("void", 0);
    }

    public static int bytesDe(String tipo) {
        return BYTES.getOrDefault(tipo, 0);
    }

    private Ambito top;                                     /* ámbito actual */
    private final List<Ambito> todos = new ArrayList<>();   /* todos los ámbitos creados */
    private final List<String> errores = new ArrayList<>();

    /* Datos del próximo ámbito (los parámetros se leen antes del ¿:) */
    private String nombreSiguiente = "bloque";
    private Simbolo funcionSiguiente = null;
    private final List<Simbolo> pendientes = new ArrayList<>();

    public TablaSimbolos() {
        top = new Ambito(null, "global", true);
        todos.add(top);
    }

    /* ======================== ÁMBITOS ======================== */

    /* Indico el nombre del próximo ámbito (if, while, else...). */
    public void siguienteAmbito(String nombre) {
        nombreSiguiente = nombre;
    }

    /* Abro un ámbito nuevo encadenado al actual (al encontrar ¿:). */
    public void abrirAmbito() {
        abrirAmbito(nombreSiguiente);
    }

    public void abrirAmbito(String nombre) {
        boolean esFuncion = funcionSiguiente != null;

        top = new Ambito(top, nombre, esFuncion);
        todos.add(top);

        if (esFuncion) {
            top.duenio = funcionSiguiente;
            funcionSiguiente.tablaHija = top;
            funcionSiguiente = null;
        }

        nombreSiguiente = "bloque";

        for (Simbolo s : pendientes) {
            declarar(s);
        }
        pendientes.clear();
    }

    /*
     * Regreso al ámbito anterior (al encontrar :?).
     * Si se cierra el ámbito de una función, se guarda en ella
     * el tamaño de su registro de activación.
     */
    public void cerrarAmbito() {
        if (top.anterior == null) {
            return;
        }
        if (top.duenio != null) {
            top.duenio.bytes = top.marco.siguienteOffset;
        }
        top = top.anterior;
    }

    /* ===================== DECLARACIONES ===================== */

    /*
     * Inserto un símbolo en el ámbito actual y le asigno ámbito,
     * acceso y dirección.
     * Si ya existe en ESE ámbito se reporta error.
     */
    public void declarar(Simbolo s) {
        if (s == null || s.nombre == null) {
            return;
        }

        Simbolo previo = top.getLocal(s.nombre);

        if (previo != null) {
            error(s.linea, s.columna, "'" + s.nombre
                    + "' ya fue declarado en este ámbito (línea " + previo.linea + ")");
            return;
        }

        s.ambito = top;
        s.acceso = top.nivel == 0 ? "público" : "privado";

        /* Las funciones viven en el código, no ocupan espacio de datos. */
        if (!s.esFuncion()) {
            s.offset = top.marco.siguienteOffset;
            s.segmento = top.marco.anterior == null ? "global" : "pila";
            if (s.bytes > 0) {
                top.marco.siguienteOffset += s.bytes;
            }
        }

        top.put(s.nombre, s);
    }

    public void declararVariable(String nombre, String tipo, String valorInicial,
                                 int linea, int columna) {
        Simbolo s = new Simbolo(nombre, tipo, Simbolo.VARIABLE, linea, columna);
        s.valorInicial = limpiar(valorInicial);
        declarar(s);
    }

    /*
     * tamanio: texto entre ʃ: y :ʅ (null si no se indicó).
     * elementos: cantidad de valores de la lista ¿: ... :? (0 si no hay lista).
     */
    public void declararArreglo(String nombre, String tipo, String tamanio, int elementos,
                                String valorInicial, int linea, int columna) {

        Simbolo s = new Simbolo(nombre, tipo, Simbolo.ARREGLO, linea, columna);

        s.valorInicial = limpiar(valorInicial);
        s.elementos = -1;

        if (tamanio != null) {
            try {
                s.elementos = Integer.parseInt(tamanio.trim());
            } catch (NumberFormatException e) {
                s.elementos = -1;      /* tamaño dado por una expresión */
            }
        } else if (elementos > 0) {
            s.elementos = elementos;   /* sin tamaño: lo define la lista */
        }

        s.bytes = s.elementos > 0 ? s.elementos * bytesDe(tipo) : 0;
        declarar(s);
    }

    /*
     * Declaro una función (o procedimiento si es void) en el ámbito
     * actual y dejo sus parámetros listos para el ámbito de su cuerpo.
     */
    public void declararFuncion(String nombre, String tipo, List<Simbolo> parametros,
                                int linea, int columna) {

        String categoria = "void".equals(tipo) ? Simbolo.PROCEDIMIENTO : Simbolo.FUNCION;

        Simbolo f = new Simbolo(nombre, tipo, categoria, linea, columna);

        if (parametros != null) {
            f.parametros.addAll(parametros);
        }

        declarar(f);

        siguienteAmbito((Simbolo.PROCEDIMIENTO.equals(categoria) ? "procedimiento " : "funcion ") + nombre);
        funcionSiguiente = f;

        if (parametros != null) {
            pendientes.addAll(parametros);
        }
    }

    /*
     * Une el signo negativo con su número: "- 0.01" -> "-0.01",
     * "x - - 14" -> "x - -14".
     */
    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("(^|[-+*/,Ͱ]\\s|mod\\s|pot\\s|є:\\s|ʃ:\\s|¿:\\s)-\\s(?=\\d)", "$1-");
    }

    /* ========================== USOS ========================== */

    /* Busco un identificador usado como variable (factor -> id). */
    public Simbolo usar(String nombre, int linea, int columna) {
        if (nombre == null) {
            return null;
        }
        Simbolo s = top.get(nombre);
        if (s == null) {
            error(linea, columna, "'" + nombre + "' no ha sido declarado");
        } else if (s.esFuncion()) {
            error(linea, columna, "'" + nombre + "' es una función y se usa como variable");
        }
        return s;
    }

    public Simbolo usarArreglo(String nombre, int linea, int columna) {
        if (nombre == null) {
            return null;
        }
        Simbolo s = top.get(nombre);
        if (s == null) {
            error(linea, columna, "el arreglo '" + nombre + "' no ha sido declarado");
        } else if (!Simbolo.ARREGLO.equals(s.categoria)) {
            error(linea, columna, "'" + nombre + "' no es un arreglo");
        }
        return s;
    }

    public Simbolo usarFuncion(String nombre, int linea, int columna) {
        if (nombre == null) {
            return null;
        }
        Simbolo s = top.get(nombre);
        if (s == null) {
            error(linea, columna, "la función '" + nombre + "' no ha sido declarada");
        } else if (!s.esFuncion()) {
            error(linea, columna, "'" + nombre + "' no es una función");
        }
        return s;
    }

    /* ========================= ERRORES ========================= */

    /* Mismo formato que los errores léxicos, para que Main los muestre igual. */
    private void error(int linea, int columna, String mensaje) {
        errores.add("Error semántico en línea " + linea + ", columna " + columna + ": " + mensaje);
    }

    public List<String> getErrores() {
        return errores;
    }

    /* ========================= REPORTE ========================= */

    /* Nombre de la carpeta donde se guarda el reporte completo */
    public static final String CARPETA_REPORTE = "tabla_simbolos";

    /*
     * Descripcion:
     * Ubico la carpeta tabla_simbolos dentro de la carpeta Programa:
     *   PY1_CompiladoresEInterpretes\Programa\bin             <- aquí están los .class
     *   PY1_CompiladoresEInterpretes\Programa\tabla_simbolos  <- aquí se guarda el reporte
     *
     * Restricciones:
     * Se calcula a partir de la ubicación de los .class, así funciona
     * sin importar desde qué carpeta se ejecute el programa.
     */
    public static java.io.File carpetaReporte() {
        try {
            java.io.File bin = new java.io.File(TablaSimbolos.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());

            java.io.File programa = bin.getParentFile();          /* ...\Programa */

            return new java.io.File(programa, CARPETA_REPORTE);

        } catch (Exception e) {
            /* Si no se puede ubicar, se usa la carpeta actual. */
            return new java.io.File(CARPETA_REPORTE);
        }
    }

    /*
     * Descripcion:
     * Genero un resumen corto para la consola: una tabla por ámbito
     * con las columnas principales (cabe en 76 caracteres).
     */
    public String generarResumen() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("  %-24s : %5d%n", "Ámbitos", todos.size()));
        sb.append(String.format("  %-24s : %5d%n", "Símbolos", totalSimbolos()));
        sb.append("\n");

        for (Ambito a : todos) {
            sb.append("  ▌ ").append(a.ruta()).append("\n");

            if (a.cantidad() == 0) {
                sb.append("    (sin declaraciones)\n\n");
                continue;
            }

            sb.append(String.format("    %-15s %-13s %-7s %5s  %-11s %5s%n",
                    "NOMBRE", "CATEGORÍA", "TIPO", "BYTES", "DIRECCIÓN", "LÍNEA"));
            sb.append("    ").append("─".repeat(15)).append(" ").append("─".repeat(13)).append(" ")
              .append("─".repeat(7)).append(" ").append("─".repeat(5)).append("  ")
              .append("─".repeat(11)).append(" ").append("─".repeat(5)).append("\n");

            for (Simbolo s : a.simbolos()) {
                sb.append(String.format("    %-15s %-13s %-7s %5s  %-11s %5d%n",
                        corto(s.nombre, 15), s.categoria, s.tipo,
                        s.esFuncion() ? "—" : (s.bytes > 0 ? String.valueOf(s.bytes) : "?"),
                        s.direccion(), s.linea));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /*
     * Descripcion:
     * Genero el reporte completo con todos los atributos de cada
     * símbolo, el árbol de ámbitos, las funciones y los errores.
     */
    public String generarReporte(String nombreFuente) {
        StringBuilder sb = new StringBuilder();
        String linea = "═".repeat(140);

        sb.append(linea).append("\n");
        sb.append("  TABLA DE SÍMBOLOS\n");
        sb.append(linea).append("\n\n");

        sb.append("  Archivo fuente : ").append(nombreFuente).append("\n");
        sb.append("  Generado       : ")
          .append(java.time.LocalDateTime.now().format(
                  java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
          .append("\n");
        sb.append("  Ámbitos        : ").append(todos.size()).append("\n");
        sb.append("  Símbolos       : ").append(totalSimbolos()).append("\n");
        sb.append("  Errores        : ").append(errores.size()).append("\n\n");

        sb.append("  CONVENCIONES\n");
        sb.append("  ─────────────\n");
        sb.append("  Tamaños    : int 4 B · float 4 B · bool 1 B · char 1 B · string 4 B (referencia a la cadena)\n");
        sb.append("  Dirección  : global+n = desplazamiento en la zona estática · pila+n = desplazamiento en el registro de activación\n");
        sb.append("  Acceso     : público = declarado en el ámbito global · privado = declarado dentro de una función o bloque\n");
        sb.append("  Parámetros : el lenguaje solo tiene paso por valor\n\n");

        /* ---------- 1. Árbol de ámbitos ---------- */
        sb.append(titulo("1. ÁRBOL DE ÁMBITOS (tablas encadenadas)"));
        arbol(sb, todos.get(0), "  ", "", true);
        sb.append("\n");

        /* ---------- 2. Una tabla por ámbito ---------- */
        sb.append(titulo("2. SÍMBOLOS POR ÁMBITO"));

        for (Ambito a : todos) {
            sb.append("  ÁMBITO: ").append(a.ruta()).append("\n");
            sb.append("  Nivel ").append(a.nivel)
              .append(a.nivel == 0 ? " (global)" : " (local)")
              .append(" · padre: ").append(a.anterior == null ? "—" : a.anterior.nombre)
              .append(" · tablas hijas: ").append(a.nombresHijas()).append("\n");

            if (a.cantidad() == 0) {
                sb.append("  (sin declaraciones)\n\n");
                continue;
            }

            CuadroTexto c = new CuadroTexto(
                    new String[]{"Nº", "NOMBRE", "CATEGORÍA", "TIPO", "BYTES", "DIMENSIÓN", "DIRECCIÓN", "VALOR INICIAL", "ACCESO", "LÍN:COL"},
                    new int[]{3, 16, 13, 7, 6, 12, 11, 30, 8, 8});

            int n = 1;
            for (Simbolo s : a.simbolos()) {
                c.fila(String.valueOf(n++),
                        s.nombre,
                        s.categoria,
                        s.tipo,
                        s.esFuncion() ? "—" : (s.bytes > 0 ? String.valueOf(s.bytes) : "?"),
                        s.textoDimensionCorta(),
                        s.direccion(),
                        s.esFuncion() || s.valorInicial == null ? "—" : s.valorInicial,
                        s.acceso,
                        s.linea + ":" + s.columna);
            }
            sb.append(c.dibujar("  ")).append("\n");
        }

        /* ---------- 3. Funciones y procedimientos ---------- */
        sb.append(titulo("3. FUNCIONES Y PROCEDIMIENTOS (estructura de parámetros)"));

        CuadroTexto f = new CuadroTexto(
                new String[]{"NOMBRE", "CATEGORÍA", "RETORNO", "Nº PARÁM.", "PARÁMETROS FORMALES", "PASO", "REG. ACTIV.", "TABLA HIJA", "LÍN:COL"},
                new int[]{14, 13, 8, 9, 30, 9, 11, 24, 8});

        boolean hay = false;
        for (Simbolo s : todos.get(0).simbolos()) {
            if (!s.esFuncion()) {
                continue;
            }
            hay = true;
            f.fila(s.nombre,
                    s.categoria,
                    s.tipo,
                    String.valueOf(s.parametros.size()),
                    s.parametros.isEmpty() ? "(ninguno)" : s.listaParametros(),
                    s.parametros.isEmpty() ? "—" : "por valor",
                    s.bytes + " B",
                    s.tablaHija == null ? "—" : s.tablaHija.nombre,
                    s.linea + ":" + s.columna);
        }
        sb.append(hay ? f.dibujar("  ") : "  (no hay funciones)\n").append("\n");

        /* ---------- 4. Errores ---------- */
        sb.append(titulo("4. ERRORES SEMÁNTICOS DETECTADOS CON LA TABLA"));

        if (errores.isEmpty()) {
            sb.append("  No se encontraron errores.\n");
        } else {
            CuadroTexto e = new CuadroTexto(
                    new String[]{"Nº", "LÍNEA", "COL", "DESCRIPCIÓN"},
                    new int[]{3, 6, 5, 80});
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "Error \\S+ en línea (\\d+), columna (\\d+): (.*)");
            int n = 1;
            for (String err : errores) {
                java.util.regex.Matcher m = p.matcher(err);
                if (m.matches()) {
                    e.fila(String.valueOf(n++), m.group(1), m.group(2), m.group(3));
                } else {
                    e.fila(String.valueOf(n++), "-", "-", err);
                }
            }
            sb.append(e.dibujar("  "));
        }

        sb.append("\n").append(linea).append("\n");
        return sb.toString();
    }

    /*
     * Descripcion:
     * Guardo el reporte completo en la carpeta tabla_simbolos.
     *
     * Entrada: el archivo fuente analizado.
     * Salida : el archivo .txt creado.
     */
    public java.io.File guardarReporte(java.io.File fuente) throws java.io.IOException {
        java.io.File carpeta = carpetaReporte();
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        String base = fuente.getName();
        int punto = base.lastIndexOf('.');
        if (punto > 0) {
            base = base.substring(0, punto);
        }

        java.io.File salida = new java.io.File(carpeta, base + "_tabla_simbolos.txt");

        java.nio.file.Files.writeString(salida.toPath(),
                generarReporte(fuente.getName()),
                java.nio.charset.StandardCharsets.UTF_8);

        return salida;
    }

    /* ------------- datos para mostrar la tabla en Main ------------- */

    /* Todos los ámbitos, en el orden en que se crearon */
    public List<Ambito> getAmbitos() {
        return todos;
    }

    public int cantidadSimbolos() {
        return totalSimbolos();
    }

    /* Árbol de ámbitos dibujado con ├── y └── */
    public String arbolDeAmbitos() {
        StringBuilder sb = new StringBuilder();
        arbol(sb, todos.get(0), "  ", "", true);
        return sb.toString();
    }

    /* ---------------- auxiliares del reporte ---------------- */

    private int totalSimbolos() {
        int total = 0;
        for (Ambito a : todos) {
            total += a.cantidad();
        }
        return total;
    }

    private static String titulo(String texto) {
        return "  " + texto + "\n  " + "─".repeat(texto.length()) + "\n\n";
    }

    private static String corto(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    /* Dibuja el árbol de ámbitos con ├── y └── */
    private static void arbol(StringBuilder sb, Ambito a, String margen, String prefijo, boolean ultimo) {
        if (a.anterior == null) {
            sb.append(margen).append(a.nombre);
        } else {
            sb.append(margen).append(prefijo).append(ultimo ? "└── " : "├── ").append(a.nombre);
        }
        sb.append("  (").append(a.cantidad()).append(a.cantidad() == 1 ? " símbolo)" : " símbolos)").append("\n");

        String siguiente = a.anterior == null ? "" : prefijo + (ultimo ? "    " : "│   ");
        for (int i = 0; i < a.hijas.size(); i++) {
            arbol(sb, a.hijas.get(i), margen, siguiente, i == a.hijas.size() - 1);
        }
    }

}


/*
 * ============================================================
 *                         SIMBOLO
 * ============================================================
 * Descripcion: Una entrada de la tabla con todos sus atributos.
 */
class Simbolo {

    /* Categorías (el lenguaje no tiene constantes, clases ni tipos de usuario) */
    public static final String VARIABLE      = "variable";
    public static final String ARREGLO       = "arreglo";
    public static final String FUNCION       = "función";
    public static final String PROCEDIMIENTO = "procedimiento";
    public static final String PARAMETRO     = "parámetro";

    /* 1. Información básica */
    public String nombre;
    public String categoria;

    /* 2. Atributos semánticos y de tipo */
    public String tipo;                 /* tipo de dato, o tipo de retorno en funciones */
    public int bytes;                   /* tamaño en memoria */
    public int elementos = 0;           /* arreglos: cantidad de elementos (-1 = desconocido) */
    public List<Simbolo> parametros = new ArrayList<>();

    /* 3. Ubicación */
    public Ambito ambito;
    public String segmento = "—";       /* global o pila */
    public int offset = 0;              /* desplazamiento dentro del segmento */
    public int linea;
    public int columna;

    /* 4. Adicionales */
    public String valorInicial;
    public String acceso;               /* público (global) o privado (local) */
    public Ambito tablaHija;            /* funciones: tabla de su cuerpo */

    public Simbolo(String nombre, String tipo, String categoria, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linea = linea;
        this.columna = columna;
        this.bytes = (FUNCION.equals(categoria) || PROCEDIMIENTO.equals(categoria))
                ? 0 : TablaSimbolos.bytesDe(tipo);
    }

    public boolean esFuncion() {
        return FUNCION.equals(categoria) || PROCEDIMIENTO.equals(categoria);
    }

    /* Tipos de los parámetros formales: є: int a, char b :э */
    public String firma() {
        StringBuilder sb = new StringBuilder("є: ");
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parametros.get(i).tipo).append(" ").append(parametros.get(i).nombre);
        }
        return sb.append(parametros.isEmpty() ? ":э" : " :э").toString();
    }

    public String textoTamanio() {
        if (!ARREGLO.equals(categoria)) {
            return bytes + " B";
        }
        if (elementos < 0) {
            return "? B (tamaño por expresión)";
        }
        return bytes + " B (" + elementos + " × " + TablaSimbolos.bytesDe(tipo) + " B)";
    }

    /* Dirección legible: global+n, pila+n o código */
    public String direccion() {
        return esFuncion() ? "código" : segmento + "+" + offset;
    }

    /* Dimensión corta para la tabla: [0..n-1] */
    public String textoDimensionCorta() {
        if (!ARREGLO.equals(categoria)) {
            return "—";
        }
        return elementos < 0 ? "[0..?]" : "[0.." + (elementos - 1) + "]";
    }

    /* Parámetros formales: int a, char b */
    public String listaParametros() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parametros.get(i).tipo).append(" ").append(parametros.get(i).nombre);
        }
        return sb.toString();
    }

    public String textoDimension() {
        if (elementos < 0) {
            return "1 dimensión, índices [0..?]";
        }
        return "1 dimensión, índices [0.." + (elementos - 1) + "], " + elementos + " elementos";
    }
}


/*
 * ============================================================
 *                          AMBITO
 * ============================================================
 * Descripcion: Una tabla por ámbito, encadenada al ámbito que la
 * contiene (clase Env de la lectura). Guarda también sus tablas
 * hijas y el marco donde se reservan las direcciones.
 */
class Ambito {

    private final Map<String, Simbolo> tabla = new LinkedHashMap<>();
    protected final Ambito anterior;
    public final String nombre;
    public final int nivel;

    public final List<Ambito> hijas = new ArrayList<>();

    /*
     * Marco: ámbito que reserva las direcciones. El global y cada
     * función tienen el suyo; los bloques internos (if, while, for)
     * usan el de la función que los contiene.
     */
    public final Ambito marco;
    public int siguienteOffset = 0;
    public Simbolo duenio;              /* función dueña de este ámbito */

    public Ambito(Ambito anterior, String nombre, boolean esMarco) {
        this.anterior = anterior;
        this.nombre = nombre;
        this.nivel = (anterior == null) ? 0 : anterior.nivel + 1;
        this.marco = (esMarco || anterior == null) ? this : anterior.marco;
        if (anterior != null) {
            anterior.hijas.add(this);
        }
    }

    /* put de la lectura: agrega al ámbito actual */
    public void put(String s, Simbolo sym) {
        tabla.put(s, sym);
    }

    /* get de la lectura: busca del ámbito actual hacia afuera */
    public Simbolo get(String s) {
        for (Ambito e = this; e != null; e = e.anterior) {
            Simbolo encontrado = e.tabla.get(s);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    /* Solo en este ámbito, para detectar redeclaraciones */
    public Simbolo getLocal(String s) {
        return tabla.get(s);
    }

    public Iterable<Simbolo> simbolos() {
        return tabla.values();
    }

    public int cantidad() {
        return tabla.size();
    }

    /* Ruta desde el global: global › funcion f › if */
    public String ruta() {
        return anterior == null ? nombre : anterior.ruta() + " › " + nombre;
    }

    public String nombresHijas() {
        if (hijas.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hijas.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(hijas.get(i).nombre);
        }
        return sb.toString();
    }
}


/*
 * ============================================================
 *                       CUADRO DE TEXTO
 * ============================================================
 * Descripcion: Dibuja una tabla con bordes. Si un texto no cabe
 * en su columna, continúa en la línea siguiente de la misma fila.
 */
class CuadroTexto {

    private final String[] titulos;
    private final int[] anchos;
    private final List<String[]> filas = new ArrayList<>();

    public CuadroTexto(String[] titulos, int[] anchos) {
        this.titulos = titulos;
        this.anchos = anchos;
    }

    public void fila(String... celdas) {
        filas.add(celdas);
    }

    public String dibujar(String margen) {
        StringBuilder sb = new StringBuilder();

        sb.append(margen).append(borde('┌', '┬', '┐')).append("\n");
        sb.append(lineasDeFila(margen, titulos));
        sb.append(margen).append(borde('╞', '╪', '╡').replace('─', '═')).append("\n");

        for (int i = 0; i < filas.size(); i++) {
            sb.append(lineasDeFila(margen, filas.get(i)));
            if (i < filas.size() - 1) {
                sb.append(margen).append(borde('├', '┼', '┤')).append("\n");
            }
        }

        sb.append(margen).append(borde('└', '┴', '┘')).append("\n");
        return sb.toString();
    }

    private String borde(char izq, char medio, char der) {
        StringBuilder sb = new StringBuilder().append(izq);
        for (int i = 0; i < anchos.length; i++) {
            sb.append("─".repeat(anchos[i] + 2));
            sb.append(i < anchos.length - 1 ? medio : der);
        }
        return sb.toString();
    }

    private String lineasDeFila(String margen, String[] celdas) {
        List<List<String>> partes = new ArrayList<>();
        int alto = 1;

        for (int i = 0; i < anchos.length; i++) {
            String texto = i < celdas.length && celdas[i] != null ? celdas[i] : "";
            List<String> l = ajustar(texto, anchos[i]);
            partes.add(l);
            alto = Math.max(alto, l.size());
        }

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < alto; r++) {
            sb.append(margen).append("│");
            for (int i = 0; i < anchos.length; i++) {
                String t = r < partes.get(i).size() ? partes.get(i).get(r) : "";
                sb.append(" ").append(String.format("%-" + anchos[i] + "s", t)).append(" │");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /* Corta el texto en líneas de máximo "ancho" caracteres */
    private static List<String> ajustar(String texto, int ancho) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (String palabra : texto.split(" ")) {
            while (palabra.length() > ancho) {
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                lineas.add(palabra.substring(0, ancho));
                palabra = palabra.substring(ancho);
            }
            if (actual.length() > 0 && actual.length() + 1 + palabra.length() > ancho) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }
            if (actual.length() > 0) {
                actual.append(' ');
            }
            actual.append(palabra);
        }
        if (actual.length() > 0 || lineas.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }
}