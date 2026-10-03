# Instituto Tecnológico de Costa Rica

## Ingeniería en Computación

### Compiladores e Intérpretes

**Semestre:** II, 2026

**Profesor:** [Allan Rodriguez Davila](https://tecdigital.tec.ac.cr/dotlrn/community-member?user_id=451189)

**Proyecto:** #1 — Análisis Léxico y Sintáctico

**Fecha de entrega:** 3 de octubre de 2026

### Estudiantes

| Estudiante               | Carné     |
| ------------------------ | ---------- |
| Heldyis Agüero Espinoza | 2023296812 |
| Alice Arias Salazar      | 2023104639 |

---

# Índice

1. [Descripción del problema]
2. [Objetivos]
3. [Herramientas y librerías utilizadas]
4. [Estructura del proyecto]
5. [Requisitos]
6. [Gramática del lenguaje]
7. [Características del lenguaje]
8. [Análisis léxico]
9. [Análisis sintáctico]
10. [Manejo de errores]
11. [Compilación del proyecto]
12. [Ejecución del proyecto]
13. [Archivos de prueba]
14. [Flujo de ejecución]
15. [Resultado esperado]

---

# Descripción del problema

El proyecto consiste en desarrollar los componentes iniciales de un compilador para un lenguaje de programación imperativo ligero, diseñado para realizar configuraciones básicas en dispositivos y sistemas embebidos.

La propuesta surge debido a que los dispositivos electrónicos y chips actuales pueden configurarse mediante lenguajes de programación cada vez más ligeros y potentes. Por esta razón, se plantea el desarrollo de un lenguaje propio que permita representar instrucciones, declaraciones, expresiones y estructuras de control de una manera sencilla.

En este primer proyecto se desarrollan dos etapas fundamentales de un compilador:

* **Análisis léxico:** identifica los diferentes elementos que forman parte del lenguaje y genera los tokens correspondientes.
* **Análisis sintáctico:** verifica que la secuencia de tokens cumpla con las reglas definidas por la gramática del lenguaje.

El sistema recibe un archivo fuente con extensión `.cmm`, analiza su contenido y genera un archivo con los tokens reconocidos. Posteriormente, los tokens son utilizados por el analizador sintáctico para determinar si el programa cumple con la gramática definida.

Además, el sistema debe detectar errores léxicos y sintácticos, indicar la línea donde ocurren y continuar con el análisis utilizando mecanismos de recuperación de errores.

---

# Objetivos

## Objetivo general

Desarrollar un analizador léxico y sintáctico para un lenguaje de programación imperativo definido mediante una gramática BNF, utilizando JFlex y CUP.

## Objetivos específicos

* Implementar un analizador léxico utilizando  **JFlex** .
* Definir los tokens, palabras reservadas, operadores, delimitadores, identificadores y literales del lenguaje.
* Detectar y reportar errores léxicos.
* Generar un archivo con los tokens reconocidos y sus respectivos lexemas.
* Implementar un analizador sintáctico utilizando  **Java CUP** .
* Verificar que los programas cumplan con las reglas de la gramática.
* Implementar recuperación de errores sintácticos mediante  **Panic Mode** .
* Indicar la línea y columna donde se presentan los errores.
* Permitir continuar el análisis después de encontrar errores.
* Probar el funcionamiento del analizador mediante archivos `.cmm`.

---

# Herramientas y librerías utilizadas

## Java JDK 21.0.12

Java es el lenguaje utilizado para desarrollar el proyecto.

En este proyecto se utiliza **JDK 21.0.12** para:

* Compilar los archivos `.java`.
* Ejecutar el programa.
* Procesar archivos de entrada y salida.
* Trabajar con las clases generadas por JFlex y CUP.

---

## JFlex 1.9.1

**JFlex** es utilizado para generar el analizador léxico.

El archivo:

```text
Lexer.flex
```

contiene las reglas que permiten reconocer los diferentes elementos del lenguaje.

A partir de este archivo, JFlex genera:

```text
Lexer.java
```

El analizador léxico reconoce:

* Palabras reservadas.
* Identificadores.
* Números enteros.
* Números decimales.
* Cadenas de texto.
* Caracteres.
* Operadores.
* Delimitadores.
* Comentarios.
* Errores léxicos.

---

## Java CUP 11b

**Java CUP** se utiliza para generar el analizador sintáctico.

El archivo:

```text
Parser.cup
```

contiene la definición de la gramática del lenguaje y las reglas utilizadas por el parser.

A partir de este archivo se generan principalmente:

```text
parser.java
sym.java
```

El archivo `parser.java` contiene el analizador sintáctico generado por CUP, mientras que `sym.java` contiene las constantes asociadas a los símbolos y tokens del lenguaje.

---

## Java CUP Runtime

El archivo:

```text
java-cup-11b.jar
```

también proporciona las clases necesarias para ejecutar el parser generado.

Entre ellas se encuentra:

```java
java_cup.runtime.Symbol
```

Esta clase representa los símbolos utilizados durante el análisis sintáctico y permite transportar información como:

* Tipo de token.
* Lexema.
* Línea.
* Columna.

---

## Librerías estándar de Java

El archivo `Main.java` utiliza diferentes librerías incluidas en Java.

### `java.io`

Se utiliza para trabajar con archivos, entradas, salidas, lectores y escritores.

Entre las clases utilizadas se encuentran:

```java
File
FileInputStream
FileOutputStream
InputStreamReader
OutputStream
PrintStream
PrintWriter
Reader
```

Estas clases permiten leer el archivo fuente, generar el archivo de tokens y manejar la entrada y salida del programa.

### `java.lang.reflect`

Se utiliza para acceder dinámicamente a información de las clases generadas.

En este proyecto permite trabajar con los valores definidos en:

```text
sym.java
```

### `java.nio.charset`

Se utiliza para especificar la codificación de caracteres.

El proyecto utiliza:

```text
UTF-8
```

para permitir el manejo correcto de caracteres especiales utilizados por el lenguaje.

### `java.nio.file`

Se utiliza para trabajar con archivos y rutas del sistema.

Entre las clases utilizadas se encuentran:

```java
Files
Path
Paths
```

### `java.util`

Se utiliza para manejar estructuras de datos como:

```java
ArrayList
HashMap
LinkedHashSet
List
Map
Set
```

Estas estructuras permiten almacenar información relacionada con tokens, errores, símbolos y otros elementos utilizados durante el análisis.

### `java.util.regex`

Se utiliza para trabajar con expresiones regulares mediante:

```java
Matcher
Pattern
```

Estas clases permiten realizar búsquedas y procesamiento de patrones de texto.

---

# Estructura del proyecto

La estructura actual del proyecto es la siguiente:

```text
PY1_CompiladoresEInterpretes/
│
├── .vscode/
│
├── Documentacion/
│
├── Programa/
│   │
│   ├── bin/
│   │
│   ├── librerias/
│   │   ├── java-cup-11b.jar
│   │   └── jflex-full-1.9.1.jar
│   │
│   ├── pruebas/
│   │   ├── prueba_lexica.cmm
│   │   ├── prueba_sintactica.cmm
│   │   └── tokens.txt
│   │
│   ├── src/
│   │   ├── ejecutables/
│   │   │   ├── build.bat
│   │   │   └── run.bat
│   │   │
│   │   └── Main.java
│   │
│   ├── Lexer.flex
│   ├── Parser.cup
│   └── Tokens.xlsx
│
└── README.md
```

## Descripción de los principales archivos

### `Lexer.flex`

Contiene las reglas del analizador léxico.

Es utilizado por JFlex para generar automáticamente `Lexer.java`.

### `Parser.cup`

Contiene la gramática del lenguaje y las reglas del analizador sintáctico.

Es utilizado por CUP para generar `parser.java` y `sym.java`.

### `Main.java`

Es el punto de entrada del programa.

Se encarga de coordinar el análisis léxico y sintáctico, procesar el archivo fuente, generar el archivo de tokens y mostrar los resultados del análisis.

### `build.bat`

Automatiza el proceso de compilación del proyecto.

Se encarga de:

1. Verificar las librerías.
2. Crear la carpeta `bin`.
3. Generar el parser mediante CUP.
4. Generar el lexer mediante JFlex.
5. Compilar los archivos Java.

### `run.bat`

Automatiza la ejecución del proyecto.

Solicita el nombre del archivo `.cmm`, ejecuta el analizador y genera el archivo:

```text
pruebas/tokens.txt
```

### `Tokens.xlsx`

Documento utilizado para registrar y documentar los tokens definidos para el lenguaje.

### `pruebas/`

Contiene los archivos utilizados para comprobar el funcionamiento del analizador.

### `bin/`

Contiene los archivos generados y compilados necesarios para ejecutar el proyecto.

---

# Requisitos

Para ejecutar el proyecto se necesita:

* Windows.
* Java JDK 21.0.12.
* JFlex 1.9.1.
* Java CUP 11b.
* Java CUP Runtime.
* PowerShell o CMD.
* Visual Studio Code u otro editor compatible.

Las librerías de JFlex y CUP utilizadas por el proyecto se encuentran dentro de:

```text
Programa/librerias/
```

---

# Gramática del lenguaje

La gramática del lenguaje está definida en:

```text
Programa/Parser.cup
```

La gramática utiliza una estructura basada en BNF y define las reglas necesarias para reconocer programas escritos en el lenguaje.

El símbolo inicial de la gramática es:

```text
programa
```

La estructura general del programa está compuesta por declaraciones globales y unidades de programa.

De forma general:

```text
programa
    ::= lista_globales unidades_programa
```

El lenguaje permite declarar funciones y contiene un único procedimiento principal:

```text
principal
```

La estructura del método principal es:

```text
void principal()
{
    ...
}
```

Los símbolos utilizados para los diferentes elementos del lenguaje son definidos mediante tokens y posteriormente utilizados por las producciones de la gramática.

---

# Características del lenguaje

La gramática actual permite trabajar con diferentes elementos de un lenguaje imperativo.

## Tipos de datos

Se incluyen los siguientes tipos:

```text
int
float
bool
char
string
void
```

## Valores booleanos

El lenguaje permite utilizar:

```text
true
false
```

## Declaración de variables

Las variables se declaran utilizando la palabra reservada:

```text
val
```

## Operadores aritméticos

El lenguaje incluye operadores para:

* Incremento.
* Suma.
* Decremento.
* Resta.
* Multiplicación.
* División entera.
* División decimal.
* Módulo.
* Potencia.

Entre los símbolos utilizados se encuentran:

```text
++
+
--
-
*
//
/
mod
pot
```

## Operadores relacionales

Se incluyen:

```text
<=
>=
==
!=
<
>
```

## Operadores lógicos

Se incluyen:

```text
λ
θ
Σ
```

correspondientes a las operaciones lógicas definidas por el lenguaje.

También se permite el operador:

```text
not
```

mediante el token correspondiente definido en la gramática.

## Estructuras de control

El lenguaje incluye:

```text
if
elif
else
while
for
```

## Funciones

La gramática permite declarar funciones con y sin valor de retorno.

También permite:

```text
return
break
```

## Entrada y salida

El lenguaje incluye operaciones para:

```text
print
read
write
```

## Arreglos

Se permiten arreglos de tipo:

```text
int
float
```

y el acceso mediante los delimitadores definidos para índices.

## Comentarios

El lenguaje permite comentarios de una línea utilizando:

```text
|
```

y comentarios multilínea utilizando:

```text
¡
...
!
```

---

# Análisis léxico

El análisis léxico es realizado mediante  **JFlex** .

El archivo utilizado para definir las reglas léxicas es:

```text
Lexer.flex
```

JFlex analiza el archivo fuente carácter por carácter y agrupa los caracteres en unidades llamadas  **tokens** .

Cada token contiene información relacionada con:

* Tipo.
* Lexema.
* Línea.
* Columna.

El lexer utiliza los mecanismos de JFlex para mantener la posición actual dentro del archivo fuente.

---

## Tokens reconocidos

Entre los tokens definidos se encuentran:

### Operadores aritméticos

```text
INCREMENT
PLUS
DECREMENT
MINUS
MULTIPLY
ENTIREDIV
FLOATDIV
MOD
POT
```

### Operadores relacionales

```text
LTE
GTE
EQUAL
NEQ
LT
GT
```

### Operadores lógicos

```text
AND
OR
NOT
```

### Delimitadores

```text
OPBLOCK
CLBLOCK
PARENOP
PARENCL
OPBRACKET
CLBRACKET
TERMINATOR
ASIGN
COMA
```

### Tipos y palabras reservadas

```text
VAL
PRINCIPAL
INT
FLOAT
BOOL
CHAR
STRING
VOID
TRUE
FALSE
```

### Estructuras de control

```text
IF
ELIF
ELSE
WHILE
FOR
RETURN
BREAK
```

### Entrada y salida

```text
PRINT
READ
WRITE
```

### Literales e identificadores

```text
LITERAL_INT
LITERAL_FLOAT
LITERAL_STRING
LITERAL_CHAR
ID
```

---

# Reglas léxicas

El analizador también verifica reglas específicas para los diferentes tipos de lexemas.

Entre los errores detectados se encuentran:

* Números enteros con ceros iniciales.
* Números decimales con ceros iniciales.
* Números decimales que terminan de forma incorrecta.
* Números decimales sin los dígitos necesarios después del punto.
* Identificadores que comienzan con un número.
* Identificadores que comienzan con `_`.
* Caracteres vacíos.
* Caracteres con más de un símbolo.
* Caracteres sin cerrar.
* Comillas simples aisladas.
* Cadenas sin cerrar.
* Comentarios multilínea sin cerrar.
* Caracteres desconocidos.

Cuando ocurre un error léxico, el sistema registra información sobre el error y continúa con el análisis cuando es posible.

---

# Análisis sintáctico

El análisis sintáctico es realizado utilizando  **Java CUP** .

El archivo principal de definición de la gramática es:

```text
Parser.cup
```

CUP utiliza los tokens generados por el analizador léxico para verificar si la secuencia recibida cumple con las producciones de la gramática.

Durante esta etapa se verifica, entre otras cosas:

* Declaraciones.
* Expresiones.
* Asignaciones.
* Funciones.
* Parámetros.
* Estructuras condicionales.
* Ciclos.
* Retornos.
* Operaciones de entrada y salida.
* Arreglos.
* Método principal.
* Estructura general del programa.

---

# Precedencia de expresiones

La gramática separa las expresiones en diferentes niveles para establecer el orden de evaluación.

Se utilizan producciones como:

```text
expresion_suma
expresion_multiplicacion
expresion_unaria
expresion_potencia
valor_aritmetico
```

Esta organización permite diferenciar operaciones con distintos niveles de precedencia.

Por ejemplo, las operaciones de multiplicación se encuentran en un nivel diferente de las operaciones de suma, mientras que la potencia tiene su propio nivel dentro de la expresión.

También se contempla el uso de operaciones unarias y valores agrupados.

---

# Manejo de errores

El proyecto contempla errores tanto en la etapa léxica como en la etapa sintáctica.

## Errores léxicos

Los errores léxicos son detectados directamente por las reglas de `Lexer.flex`.

El sistema registra información como:

* Tipo de error.
* Descripción.
* Línea.
* Columna.

Esto permite continuar el análisis y reportar los problemas encontrados.

---

## Errores sintácticos

Los errores sintácticos son detectados por CUP cuando la secuencia de tokens no coincide con las producciones de la gramática.

El proyecto utiliza recuperación de errores mediante  **Panic Mode** .

La recuperación permite que, después de encontrar un error, el parser avance hasta encontrar un punto adecuado para continuar el análisis.

La gramática también incluye producciones de recuperación mediante el símbolo:

```text
error
```

Por ejemplo, se utilizan reglas relacionadas con el final de una sentencia para permitir que el análisis continúe después de ciertos errores.

El objetivo es evitar que el parser termine inmediatamente después del primer error y permitir reportar varios errores presentes en el mismo archivo.

---

# Compilación del proyecto

Para compilar el proyecto se debe abrir una terminal en la carpeta:

```text
Programa
```

Por ejemplo:

```powershell
cd "C:\Users\arias\OneDrive\Desktop\PY1_CompiladoresEInterpretes\Programa"
```

Luego se ejecuta el archivo:

```text
src\ejecutables\build.bat
```

mediante:

```powershell
.\src\ejecutables\build.bat
```

El proceso realiza automáticamente las siguientes acciones:

1. Verifica la existencia de JFlex.
2. Verifica la existencia de CUP.
3. Prepara la carpeta `bin`.
4. Genera `parser.java`.
5. Genera `sym.java`.
6. Genera `Lexer.java`.
7. Compila los archivos Java.
8. Almacena los archivos generados y compilados en `bin`.

---

# Ejecución del proyecto

Una vez compilado correctamente, se puede ejecutar:

```powershell
.\src\ejecutables\run.bat
```

El programa solicita el nombre del archivo fuente.

Los archivos fuente utilizados para las pruebas se encuentran en:

```text
Programa/pruebas/
```

Por ejemplo:

```text
prueba_lexica.cmm
```

o:

```text
prueba_sintactica.cmm
```

El programa agrega automáticamente la extensión `.cmm` cuando es necesario.

---

# Ejecución directa

También es posible ejecutar el programa directamente desde la carpeta `Programa`.

La estructura general del comando es:

```powershell
java -cp "bin;librerias\java-cup-11b.jar" Main pruebas\archivo.cmm pruebas\tokens.txt
```

Por ejemplo:

```powershell
java -cp "bin;librerias\java-cup-11b.jar" Main pruebas\prueba_sintactica.cmm pruebas\tokens.txt
```

El primer argumento corresponde al archivo fuente y el segundo al archivo donde se almacenan los tokens.

---

# Archivos de prueba

Los archivos de prueba se encuentran en:

```text
Programa/pruebas/
```

Actualmente se utilizan archivos como:

```text
prueba_lexica.cmm
prueba_sintactica.cmm
```

Estos archivos permiten comprobar diferentes partes del analizador.

## Prueba léxica

El archivo:

```text
prueba_lexica.cmm
```

se utiliza principalmente para comprobar el reconocimiento de:

* Tokens.
* Palabras reservadas.
* Identificadores.
* Literales.
* Operadores.
* Delimitadores.
* Comentarios.
* Errores léxicos.

## Prueba sintáctica

El archivo:

```text
prueba_sintactica.cmm
```

se utiliza para comprobar que las diferentes construcciones del lenguaje cumplan con las reglas definidas en `Parser.cup`.

También permite comprobar el comportamiento del parser ante errores sintácticos.

---

# Archivo de tokens

Durante la ejecución se genera:

```text
Programa/pruebas/tokens.txt
```

Este archivo contiene los tokens reconocidos durante el análisis léxico.

La información permite observar qué elementos fueron encontrados en el archivo fuente y comprobar el funcionamiento del lexer.

El sistema identifica los tokens utilizando las constantes generadas en:

```text
sym.java
```

---

# Flujo de ejecución

El funcionamiento general del proyecto puede representarse de la siguiente manera:

```text
                 ARCHIVO .CMM
                       │
                       ▼
                ┌─────────────┐
                │    JFlex    │
                │    Lexer    │
                └──────┬──────┘
                       │
                       ▼
                 TOKENS + LEXEMAS
                       │
                       ├──────────────► tokens.txt
                       │
                       ▼
                ┌─────────────┐
                │     CUP     │
                │    Parser   │
                └──────┬──────┘
                       │
                       ▼
              ANÁLISIS SINTÁCTICO
                       │
             ┌─────────┴─────────┐
             │                   │
             ▼                   ▼
       Programa válido      Errores
                             │
                             ▼
                       Recuperación
                        Panic Mode
```

---

# Flujo del programa

El proceso completo se realiza de la siguiente manera:

### 1. Selección del archivo

El usuario proporciona un archivo fuente con extensión:

```text
.cmm
```

### 2. Análisis léxico

JFlex procesa el contenido del archivo y reconoce los diferentes elementos del lenguaje.

### 3. Generación de tokens

Cada elemento reconocido se convierte en un token y se registra su lexema y posición.

### 4. Generación del archivo de tokens

Los tokens reconocidos se almacenan en:

```text
tokens.txt
```

### 5. Análisis sintáctico

Los tokens son procesados por el parser generado mediante CUP.

### 6. Verificación de la gramática

El parser comprueba que la secuencia de tokens cumpla con las producciones definidas en `Parser.cup`.

### 7. Manejo de errores

Si se encuentra un error, se reporta la información correspondiente y se intenta continuar el análisis utilizando los mecanismos de recuperación implementados.

### 8. Resultado

Finalmente se informa si el archivo cumple con las reglas del lenguaje y se muestran los errores encontrados, cuando corresponda.

---

# Resultado esperado

Cuando el archivo fuente es correcto, el programa debe mostrar información relacionada con:

```text
ANÁLISIS LÉXICO
```

indicando los tokens reconocidos y los errores léxicos encontrados.

Posteriormente se realiza:

```text
ANÁLISIS SINTÁCTICO
```

donde se determina si el programa cumple con la gramática.

En caso de existir errores, se muestra información como:

```text
Línea
Columna
Token
Lexema
Problema
```

permitiendo identificar el lugar donde se produjo el error.

Además, el archivo:

```text
tokens.txt
```

contiene el resultado correspondiente al análisis léxico.

---

# Archivos importantes

| Archivo                   | Función                                       |
| ------------------------- | ---------------------------------------------- |
| `Lexer.flex`            | Definición del analizador léxico             |
| `Parser.cup`            | Definición de la gramática y parser          |
| `Main.java`             | Punto de entrada y coordinación del análisis |
| `build.bat`             | Compilación automática                       |
| `run.bat`               | Ejecución automática                         |
| `java-cup-11b.jar`      | CUP y runtime                                  |
| `jflex-full-1.9.1.jar`  | JFlex                                          |
| `Tokens.xlsx`           | Documentación de tokens                       |
| `prueba_lexica.cmm`     | Prueba del análisis léxico                   |
| `prueba_sintactica.cmm` | Prueba del análisis sintáctico               |
| `tokens.txt`            | Archivo generado con los tokens                |


---

# Conclusión

El proyecto implementa las primeras etapas de un compilador para un lenguaje imperativo propio.

El análisis léxico se realiza mediante JFlex y permite identificar los elementos que forman parte del lenguaje, mientras que el análisis sintáctico se realiza mediante CUP y permite comprobar que los programas cumplan con la gramática definida.

La separación entre `Lexer.flex`, `Parser.cup` y `Main.java` permite organizar el proyecto en diferentes componentes, facilitando su compilación, ejecución y mantenimiento.

Además, la implementación de recuperación de errores permite continuar el análisis después de encontrar determinados errores, proporcionando información útil sobre los problemas encontrados en el código fuente.
