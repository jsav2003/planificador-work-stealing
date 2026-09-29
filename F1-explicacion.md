# F1 · Por qué el ejemplo de §3.1 produce ceros

Entregable de la F1. **Lo escribe Sebastián**: la fase termina cuando puede explicarlo sin
mirar el código. Las secciones marcadas *(tuyo)* están vacías a propósito; lo demás es
material de apoyo para no empezar de cero.

## 1. Lista de reglas de `happens-before` *(tuyo, ejercicio 3)*

Escríbela con tus palabras leyendo JLS §17.4.5, una regla por línea y qué garantiza.

-
-

### Para contrastar (guía de lectura, no para copiar)

Lo que dice el JLS §17.4.5 contra lo que cubre la tabla de `DESIGN.md` §3.2:

| Regla del JLS | ¿En la tabla de DESIGN §3.2? |
|---|---|
| Orden de programa: dentro de un hilo, cada acción sucede antes que las siguientes | **No** |
| Liberar un monitor → adquirir el mismo monitor | Sí |
| Escritura `volatile` → lectura `volatile` de la misma variable | Sí |
| `Thread.start()` → toda acción del hilo iniciado | Sí |
| Última acción de un hilo → quien detecta su fin (`join()`, `isAlive()`) | Sí (`join`) |
| Escribir el valor por defecto (0/null) → primera acción de cualquier hilo | **No** |
| `interrupt()` → el hilo interrumpido lo detecta | **No** |
| Transitividad: A → B y B → C implica A → C | **No** |
| Campos `final` bien construidos (JLS §17.5) | **No** |

Preguntas para pensar mientras lees:

- La tabla añade `compareAndSet` exitoso como escritura volatile. ¿De dónde sale eso, si
  no aparece en el JLS §17.4.5? (pista: documentación de `java.util.concurrent.atomic` y
  `VarHandle`).
- `happens-before` **no** es orden en el tiempo. ¿Puede A suceder antes que B sin haberse
  ejecutado antes en el reloj? ¿Y al revés?
- ¿Qué define exactamente una *carrera de datos* en el JLS? (§17.4.5: dos accesos en
  conflicto, no ordenados por happens-before).

## 2. Atomicidad, visibilidad y orden *(tuyo, ejercicio 4)*

Un ejemplo mínimo de cada uno que falle, y qué construcción lo arregla.

## 3. Dibujo del store buffer *(tuyo, ejercicio 5)*

Dos núcleos, cada uno con su buffer de escrituras, y la memoria. Traza paso a paso una
ejecución que da `r1 == 0 && r2 == 0`.

## 4. Volatile, release/acquire, opaque *(tuyo, ejercicio 6)*

Qué ordena cada modo y cuál necesitará el deque.

## 5. La explicación *(tuyo, ejercicio 7)*

¿Por qué, si el código "parece imposible", `r1 == 0 && r2 == 0` ocurre? Las dos causas
(procesador y compilador/JIT), y qué regla del JMM lo arregla.

Datos medidos en esta máquina (F1 ejercicios 1 y 2, ver `F1.md`): con campos normales sale
`(0,0)` ~0,5 % de las veces, también con `-Xint`; con `volatile`, nunca.
