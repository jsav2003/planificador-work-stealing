# Planificador con work-stealing y deque lock-free

Documento de diseño. Escrito antes del código, a propósito.

---

## 1. Qué es este proyecto

Una librería en Java que reparte tareas entre los núcleos de un procesador sin usar
candados en el camino crítico, y que **demuestra su correctitud contra el modelo de
memoria de Java**, no solo contra unos tests que pasan.

La pieza central es un *work-stealing deque* (algoritmo de Chase-Lev): una cola de
dos extremos donde el hilo dueño empuja y saca por un lado, y otros hilos roban por el
otro, todo con operaciones atómicas y sin bloqueo.

Equivalentes reales: `ForkJoinPool` de Java, el planificador del runtime de Go, Tokio
en Rust, Intel TBB.

## 2. Qué NO es

- No es un framework de concurrencia de uso general.
- No hay tareas asíncronas, ni `CompletableFuture`, ni integración con nada.
- No hay prioridades, cancelación, timeouts ni afinidad de núcleo.
- No busca ganarle en rendimiento a `ForkJoinPool`. Busca estar cerca y explicar la
  diferencia.

El objetivo es demostrar que se entiende el modelo de memoria de Java y que se sabe
verificar código concurrente con las herramientas correctas.

## 3. El problema de fondo

### 3.1 El código no se ejecuta como está escrito

Dos hilos, dos variables que empiezan en cero:

```java
// hilo A            // hilo B
x = 1;               y = 1;
r1 = y;              r2 = x;
```

¿Puede terminar con `r1 == 0 && r2 == 0`? Leyendo el código parece imposible: alguno
de los dos escribió primero. Pero ocurre en hardware real, con frecuencia medible.

Dos causas, independientes entre sí:

- **El procesador** guarda las escrituras en un buffer antes de publicarlas al resto
  de núcleos. La escritura de A puede no ser visible para B cuando B lee.
- **El compilador y el JIT** pueden reordenar instrucciones que no dependen entre sí
  dentro de un mismo hilo, porque para ese hilo aislado el resultado es idéntico.

Consecuencia: **en concurrencia no puedes verificar correctitud leyendo el código de
arriba abajo.** Hace falta un modelo formal.

### 3.2 El Java Memory Model

El JMM define la relación `happens-before`: si la acción A *sucede antes* que B, todo
lo que A escribió es visible para B. Si no existe esa relación entre dos acciones, no
hay ninguna garantía sobre lo que una ve de la otra.

Las fuentes de `happens-before` que usa este proyecto:

| Construcción | Garantía |
|---|---|
| Escritura `volatile` → lectura `volatile` de la misma variable | La lectura ve todo lo escrito antes de la escritura |
| `compareAndSet` exitoso | Se comporta como escritura volatile |
| `Thread.start()` | El hilo nuevo ve todo lo anterior al start |
| `Thread.join()` | El que espera ve todo lo que hizo el hilo terminado |
| Liberar un monitor → adquirirlo | Semántica clásica de `synchronized` |

Todo lo que no esté cubierto por alguna de esas reglas es una carrera de datos, y una
carrera de datos en Java no significa "valor viejo": significa que el comportamiento
no está definido por la especificación.

## 4. Qué se construye, en concreto

### 4.1 El deque de Chase-Lev

Un arreglo circular con dos índices:

```
        steal →  [ top ] ............ [ bottom ]  ← push / pop (dueño)
                   ↑                      ↑
            roban otros hilos       solo el dueño toca este extremo
```

- `bottom`: solo lo modifica el hilo dueño. Empuja y saca por ahí.
- `top`: lo modifican los ladrones, con `compareAndSet`.
- El arreglo crece al doble cuando se llena.

Tres operaciones:

```java
void push(Task t)     // solo el dueño
Task pop()            // solo el dueño, por el extremo bottom
Task steal()          // cualquier hilo, por el extremo top
```

**Por qué extremos opuestos:** en el caso común el dueño y los ladrones ni se cruzan,
así que no hay contención en absoluto. El dueño saca por `bottom` sin ninguna
operación atómica costosa. Además, robar por `top` significa robar la tarea *más
antigua*, que en algoritmos recursivos suele ser la más grande — un solo robo da mucho
trabajo.

### 4.2 La carrera central

Todo el algoritmo se reduce a un caso: **cuando queda exactamente una tarea**, el
dueño intenta sacarla por `bottom` y un ladrón intenta robarla por `top`, a la vez.
Solo uno puede ganar; si ganan los dos, la tarea se ejecuta dos veces; si pierden los
dos, se pierde.

La solución de Chase-Lev: en ese caso concreto el dueño abandona su camino rápido y
compite por la tarea usando el mismo `compareAndSet` sobre `top` que usa el ladrón.
Uno de los dos falla, y el que falla se va con las manos vacías.

Escribir eso son unas diez líneas. Entender por qué son correctas es el proyecto.

### 4.3 El pool

Sobre el deque se monta el planificador:

- N hilos trabajadores, uno por núcleo, cada uno con su deque.
- Cada hilo saca de su propio deque mientras tenga trabajo.
- Cuando se queda vacío, elige una víctima al azar e intenta robar.
- Si varios intentos fallan, cede el procesador antes de reintentar, para no quemar
  CPU girando en vacío.
- Terminación: detectar que **todos** los hilos están sin trabajo es un problema en sí
  mismo, porque un hilo puede estar a punto de generar tareas nuevas. Se resuelve con
  un contador atómico de tareas pendientes.

### 4.4 Las tareas

Modelo fork/join mínimo: una tarea puede crear subtareas y esperar sus resultados. Con
eso basta para los casos de prueba clásicos — Fibonacci recursivo (malo como
algoritmo, perfecto como banco de pruebas porque genera millones de tareas
desbalanceadas), merge sort paralelo, y recorrido de árboles.

## 5. Por qué Java

Esta es la justificación real, y no es "Java es bueno para concurrencia".

**jcstress.** Herramienta oficial de OpenJDK construida exactamente para este
problema. Se describe un escenario de dos hilos y los resultados posibles; la
herramienta lo ejecuta millones de veces retorciendo el timing a propósito (spinning,
inversión de orden, presión de memoria) y reporta **todos los resultados observados**,
clasificados como aceptables o prohibidos por la especificación. No existe equivalente
comparable en otros ecosistemas. Es la diferencia entre "mis tests pasaron" y
"observé el espacio de interleavings y ninguno violó el modelo".

**JMH.** Herramienta oficial de benchmarking. Sin ella, medir Java produce números
falsos: el JIT elimina código cuyo resultado no se usa, el calentamiento distorsiona
las primeras iteraciones, y la asignación de memoria esconde el costo real. JMH
resuelve todo eso y es el estándar con el que se publican mediciones serias en la JVM.

**El recolector de basura, como ventaja de diseño.** En Rust o C++ este mismo proyecto
obliga a resolver además la *recuperación segura de memoria*: no puedes liberar el
arreglo viejo tras crecer el deque, porque un ladrón podría estar leyéndolo. Eso
requiere hazard pointers o reclamación por épocas, que es un segundo problema tan duro
como el primero. El GC lo elimina, y permite concentrar las 86 horas en el modelo de
memoria.

**`java.lang.invoke.VarHandle`.** Da acceso explícito a los modos de acceso —
`getVolatile`, `getAcquire`, `setRelease`, `compareAndSet` — que permiten escribir
exactamente la barrera que hace falta y ninguna más. Escribir `setRelease` en vez de
un volatile completo, y poder justificar por qué basta, es la señal de que se entendió
el tema.

## 6. Interfaz pública

```java
public final class WorkStealingPool implements AutoCloseable {
    public WorkStealingPool(int parallelism);
    public <T> Future<T> submit(Task<T> task);
    public void close();
}

public abstract class Task<T> {
    protected abstract T compute();
    protected <R> Future<R> fork(Task<R> sub);
    protected <R> R join(Future<R> f);
}
```

Garantía que ofrece: **toda tarea enviada se ejecuta exactamente una vez**, y todo lo
que escribió una tarea antes de terminar es visible para quien haga `join` sobre ella.

Esa segunda mitad es una afirmación sobre `happens-before`, y hay que sostenerla con
la especificación, no con pruebas empíricas.

## 7. Verificación

Esta sección es la que da valor al proyecto. Sin ella queda un ejercicio de
estructuras de datos con hilos.

### 7.1 jcstress — el argumento principal

Cada invariante del deque se convierte en un test de jcstress. Ejemplos:

- **Un dueño y un ladrón sobre un deque con una sola tarea.** Resultados aceptables:
  uno obtiene la tarea y el otro nada. Resultado prohibido: ambos la obtienen, o
  ninguno.
- **Push concurrente con steal.** La tarea empujada nunca se pierde ni se duplica.
- **Visibilidad del contenido de la tarea.** El ladrón que obtiene una tarea ve todos
  los campos que el productor escribió antes de empujarla. Este test falla si falta la
  barrera de release, y **pasa igual en tests normales** — es exactamente la clase de
  bug que solo jcstress encuentra.

Salida esperada en el README: la tabla de resultados observados, con la columna de
prohibidos en cero tras millones de ejecuciones.

### 7.2 Modelo de referencia

Miles de operaciones aleatorias sobre el pool comparadas contra ejecución secuencial:
el conjunto de tareas ejecutadas debe ser idéntico, cada una exactamente una vez, y
los resultados agregados iguales.

### 7.3 JMH — escalado

Throughput contra número de hilos (1, 2, 4, 8, 16), comparado con:

- `ForkJoinPool` de la JVM (referencia de la industria).
- Una implementación con una única cola protegida por `synchronized` (la solución
  ingenua, para mostrar el colapso por contención).

Métricas adicionales: número de robos exitosos y fallidos, y desbalance entre hilos.
Un planificador que funciona se reconoce porque la tasa de robos cae cuando la carga
está balanceada.

### 7.4 Detección de carreras

Ejecución de la suite completa bajo herramientas de detección dinámica, y `-Xint`
(intérprete puro) además del modo normal, porque el JIT cambia qué reordenamientos
ocurren en la práctica.

## 8. Plan por fases

Presupuesto: 4-5 horas semanales durante 20-22 semanas ≈ 86 horas.

| Fase | Horas | Terminada cuando |
|---|---|---|
| F0 · Java moderno y `java.util.concurrent` | 12 | Escribes y pruebas código con `AtomicInteger`, `VarHandle` y pools existentes |
| F1 · Modelo de memoria | 12 | Puedes explicar por escrito por qué el ejemplo de la sección 3.1 produce ceros |
| F2 · Cola de Michael-Scott (calentamiento) | 15 | Cola lock-free simple, con su primer test de jcstress en verde |
| F3 · Deque de Chase-Lev | 20 | push/pop/steal correctos, incluido el crecimiento del arreglo |
| F4 · Suite de jcstress | 15 | Todos los invariantes cubiertos, cero resultados prohibidos |
| F5 · Pool, JMH y documentación | 12 | Gráfica de escalado y DESIGN/TESTING escritos |

### Reglas de rescate

- La F2 existe para fallar barato: si la cola de Michael-Scott resulta demasiado dura,
  el proyecto se reevalúa ahí, a las 39 horas, no a las 80.
- Si hay retraso, se recorta el pool (F5) y se entrega **solo el deque verificado**.
  Un deque de Chase-Lev con suite de jcstress completa ya es un repo defendible; un
  pool sin verificación no lo es.
- La F4 no se recorta nunca. Es el proyecto.

## 9. Riesgos

| Riesgo | Mitigación |
|---|---|
| El modelo de memoria no "encaja" conceptualmente | La F1 es evaluable: si tras 12 horas no puedes explicar el ejemplo base, este no es el proyecto |
| Bugs no reproducibles que consumen semanas | jcstress se introduce desde la F2, no al final |
| Medir mal y sacar conclusiones falsas | Todo número publicado sale de JMH, ninguno de `System.nanoTime()` a mano |
| Tentación de agregar features al pool | Lista de no-objetivos fija desde el día uno |

## 10. Qué queda en el repositorio

- El código, sin dependencias fuera de jcstress y JMH (ambas solo para pruebas).
- `DESIGN.md` — este documento.
- `MEMORY-MODEL.md` — la justificación, invariante por invariante, de por qué cada
  barrera es necesaria y suficiente. **Esta es la pieza que comunica competencia**: es
  un argumento sobre la especificación, no una lista de tests.
- `TESTING.md` — la salida de jcstress.
- `BENCHMARKS.md` — la gráfica de escalado y su interpretación.
- `BUGS.md` — los errores encontrados, qué los causaba, y cuál test los detectó.
- CI ejecutando la suite de jcstress.

## 11. Glosario

- **Lock-free:** garantiza que al menos un hilo progresa, sin importar qué hagan o
  cuándo se suspendan los demás. No significa "sin sincronización".
- **CAS (compare-and-set):** instrucción atómica del procesador que cambia un valor
  solo si sigue siendo el que esperabas. La base de todo el algoritmo.
- **`happens-before`:** relación del JMM que determina qué escrituras son visibles
  para qué lecturas. Si no existe entre dos acciones, no hay garantía alguna.
- **Barrera de memoria:** instrucción que limita el reordenamiento. `setRelease`
  impide que lo anterior se mueva después; `getAcquire`, que lo posterior se mueva
  antes.
- **Problema ABA:** un valor cambia de A a B y vuelve a A; un CAS lo ve igual y cree
  que nada pasó. En Chase-Lev se evita porque `top` solo crece.
- **Contención:** varios hilos compitiendo por la misma línea de caché. Es la razón de
  que la cola única con candado no escale.
- **Work-stealing:** estrategia donde cada hilo tiene su cola y los ociosos roban a los
  ocupados.
