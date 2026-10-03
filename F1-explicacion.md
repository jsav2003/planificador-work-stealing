# F1 · Por qué el ejemplo de §3.1 produce ceros

Entregable de la F1: la fase termina cuando Sebastián puede explicarlo sin mirar el código.

**Origen del texto:** todas las secciones (1, 2, 2b, 3, 4 y 5) las redactó Claude a petición de
Sebastián (2026-09-29, 2026-10-01 y entrega final el 2026-10-02). Se entregan como están, no
reescritas por Sebastián. Que él pueda explicarlas sin mirarlas es un criterio aparte que no se
ha comprobado.

## 1. Lista de reglas de `happens-before` (ejercicio 3)

Fuente: JLS §17.4.5, más la documentación de `java.util.concurrent` ("Memory Consistency
Properties"). `A → B` significa "A sucede antes que B": todo lo que A escribió es visible en B.

1. **Orden de programa.** Dentro de un hilo, cada acción sucede antes que las siguientes del
   código. Ojo: solo vale para lo que ese hilo mismo puede observar. No impide que otro hilo,
   sin más reglas de por medio, vea las cosas en otro orden (es el caso de §3.1).
2. **Monitor.** Liberar un monitor (`synchronized` sale) → adquirir *el mismo* monitor después.
3. **Volatile.** Escribir una variable `volatile` → toda lectura posterior *de esa misma*
   variable. Se lleva consigo todo lo escrito antes de la escritura.
4. **Inicio de hilo.** `Thread.start()` → toda acción del hilo iniciado.
5. **Fin de hilo.** Última acción de un hilo → quien detecta que terminó (`join()` que
   vuelve, `isAlive()` que da `false`).
6. **Valor por defecto.** Escribir el 0/`null`/`false` inicial de cada variable → primera
   acción de cualquier hilo. Por eso nunca se lee "basura", solo el valor por defecto.
7. **Interrupción.** `interrupt()` sobre un hilo → ese hilo detecta la interrupción.
8. **Transitividad.** Si A → B y B → C, entonces A → C. Es la regla que encadena las otras.
9. **Campos `final`** bien construidos (JLS §17.5): quien ve la referencia al objeto ve los
   `final` ya inicializados, sin más sincronización.
10. **Atómicos y `VarHandle`.** Un `compareAndSet` exitoso o un `getAndAdd` se comportan como
    escritura y lectura volatile (doc de `java.util.concurrent.atomic`). Así `AtomicInteger`
    da happens-before. Esta regla **no** está en el JLS §17.4.5; viene de la documentación de
    la API.
11. **Ejecutores** (doc de `java.util.concurrent`): lo que un hilo hace antes de enviar una
    tarea → la ejecución de la tarea; lo que hace la tarea → `Future.get()` que devuelve su
    resultado. Es lo que hizo válido el ejercicio 4 de la F0.

**Carrera de datos:** dos accesos a la misma variable, al menos uno escritura, *no ordenados*
por happens-before. Con una carrera, el JMM no promete nada sobre qué valor se lee.

**Garantía que compensa todo esto:** un programa sin carreras de datos se comporta como si
los hilos se intercalaran sin reordenar nada (consistencia secuencial). Es lo que compra
sincronizar bien.

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

## 2. Atomicidad, visibilidad y orden (ejercicio 4)

Son tres problemas distintos con arreglos distintos. `volatile` resuelve dos.

**Atomicidad** (`TresFallos.atomicidad`). `contador++` son tres pasos: leer, sumar 1,
escribir. Dos hilos leen el 5, los dos escriben 6, y se perdió un incremento. `volatile` no
lo evita: garantiza que cada lectura ve la última escritura y el orden entre accesos, pero
los tres pasos siguen sin ser una unidad indivisible. Medido: de 4.000.000 esperados, el
campo normal dio 3.052.311 y el `volatile` 1.201.539. Arreglo: una operación atómica
(`getAndAdd`, o el bucle de `compareAndSet` de la F0).

**Visibilidad** (`TresFallos.visibilidad`). Un hilo gira con `while (!bandera) {}` y otro
levanta `bandera`. Sin happens-before entre la escritura y la lectura, el lector no tiene
obligación de ver nunca el cambio. El JIT lo aprovecha: como nada en el bucle es
sincronización, puede leer la bandera una vez y convertirlo en `if (!bandera) while (true) {}`.
Medido: con JIT el lector no terminó nunca; con `-Xint` sí, porque el intérprete relee la
variable cada vuelta. Que "funcione" en intérprete es un accidente de esa implementación,
no una garantía. Arreglo: `volatile`, cuya escritura → lectura obliga a que el lector la vea
y le prohíbe al JIT sacar la lectura del bucle.

**Orden** (`CerosMutuos`). Es el ejemplo de §3.1; se explica entero en 2b.

| Problema | Causa | Lo arregla `volatile`? | Qué lo arregla |
|---|---|---|---|
| Atomicidad | operación de varios pasos | **No** | operación atómica (`getAndAdd`, CAS) |
| Visibilidad | el JIT saca la lectura del bucle | Sí | `volatile` |
| Orden | store buffer del procesador (y el JIT) | Sí | `volatile` en ambas variables |

## 2b. Por qué `volatile` elimina el `(0,0)` (ejercicio 2)

```
// hilo A            // hilo B
x = 1;               y = 1;
r1 = y;              r2 = x;
```

Con `x` e `y` volatile, todos los accesos volatile forman **un único orden total** (el orden
de sincronización) que respeta el orden de programa de cada hilo. Supón `r1 == 0 && r2 == 0`:

- `r1 == 0`: A leyó `y` antes de que B la escribiera, o sea `r1=y` va antes que `y=1`.
- `r2 == 0`: B leyó `x` antes de que A la escribiera, o sea `r2=x` va antes que `x=1`.
- Orden de programa: `x=1` va antes que `r1=y`, y `y=1` va antes que `r2=x`.

Encadenando: `x=1` < `r1=y` < `y=1` < `r2=x` < `x=1`. Un ciclo: `x=1` iría antes que sí
misma. Es imposible, así que `(0,0)` no puede ocurrir. Con campos normales no existe ese
orden total y no hay contradicción: por eso sale. Medido: cero `(0,0)` en millones de
iteraciones con `volatile`.

Cómo lo consigue la JVM: tras cada escritura volatile inserta una barrera que vacía el
buffer de escrituras del núcleo (en x86, una instrucción tipo `lock`), de modo que la
lectura siguiente ya no puede adelantarse a la escritura.

## 3. Dibujo del store buffer (ejercicio 5)

*Redactado por Claude.*

Cada núcleo tiene un **buffer de escrituras** entre él y la memoria compartida (la caché). Una
escritura no va directa a memoria: entra al buffer y se vacía después. El núcleo que escribió
la ve de inmediato (lee de su propio buffer); el otro núcleo no la ve hasta que se vacíe.
Las lecturas no esperan al buffer.

```
   Núcleo A                          Núcleo B
  ┌──────────┐                      ┌──────────┐
  │ buffer A │                      │ buffer B │
  │ (x = 1)  │                      │ (y = 1)  │
  └────┬─────┘                      └────┬─────┘
       │  se vacía "cuando puede"        │
       ▼                                 ▼
  ┌───────────────────────────────────────────┐
  │        memoria compartida:  x = 0, y = 0  │
  └───────────────────────────────────────────┘
```

Traza de una ejecución que da `r1 == 0 && r2 == 0` (inicio: `x = 0`, `y = 0`):

| Paso | Núcleo A | Núcleo B | buffer A | buffer B | Memoria (x, y) |
|---|---|---|---|---|---|
| 1 | ejecuta `x = 1` | ejecuta `y = 1` | `x=1` | `y=1` | (0, 0) |
| 2 | ejecuta `r1 = y`: no hay `y` en su buffer, lee memoria: **0** | ejecuta `r2 = x`: no hay `x` en su buffer, lee memoria: **0** | `x=1` | `y=1` | (0, 0) |
| 3 | | | se vacía | se vacía | (1, 1) |

Resultado: `r1 = 0`, `r2 = 0`. Los dos hilos ejecutaron sus instrucciones **en el orden del
programa**, y aun así el efecto fue como si `r1 = y` hubiera pasado antes que `x = 1`: la
escritura tardó en hacerse visible y la lectura no esperó. Ese es el reordenamiento
**store→load** (escritura seguida de lectura de otra variable). Es el único que hace x86 en
hardware, y por eso aparece también con `-Xint`.

Qué cambia con `volatile`: tras la escritura, la JVM inserta una barrera que **obliga a
vaciar el buffer antes de seguir** (en x86, una instrucción con prefijo `lock` o `mfence`).
En el paso 2 el buffer de A ya está vacío, `x = 1` ya está en memoria, y B lo lee: al menos
uno de los dos ve un 1.

## 4. Volatile, release/acquire, opaque (ejercicio 6)

Medido con `ModosAcceso` sobre el mismo patrón de §3.1 (`(0,0)` en 2.000.000 de iteraciones,
tres corridas): plano 8.522 / 5.045 / 55.289; opaque 4.521 / 6.952 / 6.556;
**release/acquire 3.195 / 4.731 / 5.525**; volatile 0 / 0 / 0; release/acquire + `fullFence`
0 / 0 / 0.

| Modo | Qué garantiza | Qué **no** garantiza |
|---|---|---|
| Plano | nada entre hilos | todo |
| `Opaque` | el acceso ocurre de verdad (el JIT no lo quita ni lo saca del bucle); coherencia sobre *esa* variable | orden respecto a otras variables |
| `Release` / `Acquire` | lo escrito antes de un release es visible para quien lo lee con acquire (mensajería: "publico y aviso") | que una lectura posterior se adelante a una escritura anterior de *otra* variable |
| `Volatile` | orden total de todos los accesos volatile | (es lo más caro) |

Conclusión principal: **release/acquire no evita el `(0,0)`**. Solo ordena "lo de antes se ve
al leer esto", en una dirección; no ordena una escritura seguida de una lectura de otra
variable (store→load). Ese es el único reordenamiento que hace x86 en hardware, y es el
que produce el `(0,0)`. Para prohibirlo hay que pagar `volatile` o poner un `fullFence`
entre la escritura y la lectura.

`-Xint`: con opaque y release/acquire salió 0 en 300.000 iteraciones, pero no lo tomo como
prueba: en intérprete las dos escrituras y lecturas quedan muy separadas en el tiempo y la
carrera casi no se da (incluso plano bajó a 16 y 2). Distinto del caso volátil, que es 0 por
garantía y no por suerte.

**Qué necesitará el deque de Chase-Lev (F3)**, a confirmar cuando se implemente:

- `push`: escribir el elemento y luego publicar `bottom` con **release**; los ladrones leen
  `bottom` con **acquire**. Es el patrón de mensajería, y release/acquire alcanza.
- `pop` (dueño): baja `bottom` y luego lee `top`. Es escritura de una variable seguida de
  lectura de *otra*, exactamente el patrón del `(0,0)`. Si fallan las dos partes, la última
  tarea se ejecuta dos veces o ninguna (DESIGN §4.2). Aquí release/acquire **no** basta: hace
  falta `volatile` o `fullFence` entre las dos operaciones.

## 5. La explicación (ejercicio 7)

*Redactado por Claude.*

**La pregunta.** Dos hilos, `x` e `y` valen 0. A hace `x = 1; r1 = y;` y B hace
`y = 1; r2 = x;`. Leyendo el código parece que alguno de los dos tiene que ver el 1 del
otro, así que `r1 == 0 && r2 == 0` "no puede pasar". Pasa.

**El error está en la suposición.** Se piensa que los hilos se intercalan, un paso cada vez,
en un único orden global. Eso es la *consistencia secuencial*, y ni el procesador ni el
compilador ni el modelo de memoria de Java la prometen. Java solo la promete a los programas
**sin carreras de datos**, y este tiene una: `x` se escribe en un hilo y se lee en otro sin
ninguna relación happens-before entre ambos accesos. Con una carrera, el JMM no dice qué valor
se lee, y `0` es un valor permitido.

**Causa 1: el procesador.** Cada núcleo escribe en un buffer propio antes de llegar a memoria
(§3). La lectura siguiente no espera al buffer, así que A lee `y` y B lee `x` antes de que
la escritura del otro sea visible. Es el reordenamiento store→load. Se comprobó que ocurre
sin compilador de por medio: con `-Xint` el `(0,0)` sigue saliendo (523 y 240 de 300.000).

**Causa 2: el compilador / JIT.** Para un hilo, `x = 1; r1 = y;` son independientes (variables
distintas), y el JIT tiene derecho a intercambiarlos o a mantener valores en registros. Es
la causa de los fallos que *solo* aparecen con JIT, como la bandera de §2 que el lector nunca
ve. En el `(0,0)` concreto de esta máquina la causa medida es la 1; la 2 está permitida por
el JMM y puede aparecer en otra JVM o con otro código.

Ambas están permitidas porque el JMM define **qué puede ver cada hilo**, no un orden de
ejecución real. Las dos optimizaciones (buffer y reordenar) son legales mientras el hilo mismo
no note la diferencia, y eso es justo lo que hace rápido el código.

**La regla que lo arregla.** Declarar `x` e `y` `volatile`. Todos los accesos volatile forman
un único orden total que respeta el orden de programa de cada hilo (§2b). `(0,0)` exigiría
`x=1 < r1=y < y=1 < r2=x < x=1`: un ciclo, imposible. En la práctica la JVM vacía el buffer
tras cada escritura volatile y prohíbe al JIT reordenar. Medido: `(0,0)` ~0,5 % con campos
normales, 0 en millones de iteraciones con `volatile`, y también 0 con `-Xint`.

**Lo que no arregla cualquier cosa.** `Release`/`Acquire` no basta (§4): ordena "lo de antes
se ve al leer esto", pero no una escritura seguida de la lectura de otra variable. Hace falta
`volatile` o un `fullFence` en medio.

**Resumen en una frase.** Sale `(0,0)` porque sin `volatile` no hay orden entre los accesos a
`x` e `y` de hilos distintos, y tanto el procesador (store buffer) como el compilador pueden
retrasar o reordenar una escritura respecto a la lectura siguiente; `volatile` los
ordena.
