# planificador-work-stealing

Deque lock-free de Chase-Lev en Java (`push`, `pop`, `steal`, con crecimiento del arreglo),
probado con JUnit y jcstress. Diseño y plan por fases en `DESIGN.md`.

## Dónde está y cómo correrlo

El deque es `src/main/java/wsp/f3/DequeChaseLev.java`. Los paquetes `wsp.f0` a `wsp.f2` son los
ejercicios previos (concurrencia básica, modelo de memoria, cola de Michael-Scott). Requiere Java 21
o superior y Maven.

- `mvn test`: JUnit secuencial y concurrente del deque (y de las fases anteriores).
- `.\scripts\jcstress.ps1 -Tests "wsp.f4.*"`: los escenarios de jcstress, en modo rápido. jcstress no
  corre con `mvn test`.

Todos los comandos, los escenarios y las mutaciones están en `TESTING.md`.

**Alcance recortado (2026-10-08):** el proyecto nació para demostrar la correctitud contra el
modelo de memoria de Java y llegar a un pool con work-stealing. Se redujo a implementar y probar el
deque. No hay pool ni mediciones con JMH. Los textos sobre el modelo de memoria (`F1-explicacion.md`
y las justificaciones de modos de acceso) los redactó Claude a petición, y **no acreditan que el
autor pueda explicarlos**; la corrección que se afirma aquí se apoya en las pruebas y en sus
límites (ver abajo), no en una demostración propia.

**Corrida final** (2026-10-15, modo `default`, `wsp.f4.*`): AMD Ryzen 5 7520U (4 núcleos / 8 hilos), Windows 11, Java 23.0.1, jcstress 0.16. **482 configuraciones, 482 pasadas, 0 fallidas, 0 errores; cero salidas prohibidas** (solo en x86, ver el límite conocido). Todas las salidas aceptables de cada escenario se observaron, salvo que I4 sacó `0, 1, 0` solo 293.598 veces de 2.056.972.934 (el ladrón rara vez gana).

| # | Escenario | Salidas aceptables observadas | Muestras | Prohibidas |
|---|---|---|---|---|
| I1 | `UltimaTareaPopContraSteal` | `1, 0, 4`; `0, 1, 4` | 1.549.314.694 | 0 |
| I2 | `UltimaTareaDosLadrones` | `1, 0, 0`; `0, 1, 0` | 1.651.939.974 | 0 |
| I3 | `UltimaTareaTresActores` | `1, 0, 0, 0`; `0, 1, 0, 0`; `0, 0, 1, 0` | 4.556.081.422 | 0 |
| I4 | `PushContraSteal` | `1, 0, 0`; `0, 1, 0` | 2.056.972.934 | 0 |
| I5 | `VisibilidadDeLaTarea` | `-1, -1`; `1, 2` | 2.738.188.934 | 0 |
| I6 | `PopVacioLuegoPush` | `0, 1, 0`; `0, 0, 1` | 2.088.368.774 | 0 |
| I7 | `CrecerConLadron` | `4, 1, 2` | 1.632.135.814 | 0 |
| I8 | `CrecerConVuelta` | `8, 2, 4, 0` | 1.014.561.414 | 0 |
| I10 | `PopContraDosRobos` | `2, 1, 0, 0`; `2, 0, 1, 0`; `0, 1, 2, 0`; `0, 2, 1, 0` | 4.937.152.782 | 0 |

I9 no tiene escenario (ver abajo).

## Límite conocido: las pruebas se corrieron solo en x86

Todos los resultados de jcstress de este repositorio salen de una máquina x86 (Windows,
Java 23). x86 tiene un modelo de memoria fuerte (TSO): no reordena escrituras entre sí ni
lecturas que dependen unas de otras. Por eso muchos errores de ordenación **no se pueden
manifestar** ahí, y un test en verde no prueba que el código sea correcto en ARM u otras
arquitecturas débiles.

Evidencia (F2, 2026-10-02): en la cola de Michael-Scott se probaron tres mutaciones cada vez más
agresivas (quitar el `volatile` de `siguiente`; publicar con escritura normal en lugar de CAS;
además `item` no `final` y lectura normal). `VisibilidadDelContenido` siguió en verde (14 de 14
configuraciones) en las tres. No sabemos cuáles de ellas son bugs reales según el modelo de
memoria (el campo `final` podría proteger el contenido); lo que sí sabemos es que en esta
máquina el test no distingue ninguna del código original.

Consecuencias:

- "Cero resultados prohibidos" significa **cero en x86**, no cero en general.
- La corrección de los modos de acceso se apoya en el razonamiento con `happens-before`
  (justificado en `F2.md`), no en los tests.
- Pendiente: repetir la suite de la F4 en una máquina ARM, donde sí podrían aparecer fallos.

Evidencia de la F4 (2026-10-15): x86 **sí** deja ver un reordenamiento, el de escritura seguida de lectura de otra variable. Al cambiar `setVolatile` por `setRelease` en `pop` (M8), el escenario I10 saca `2, 1, 2, 0` y `2, 2, 1, 0` (una tarea sale dos veces) en modo `quick`. Es la primera evidencia de que jcstress ve un fallo de modo de acceso en esta máquina. Las otras mutaciones de modo (M9 a M11) no fallaron.

**I9 (`top` solo crece, ABA)** no tiene escenario de jcstress: es una propiedad de todas las ejecuciones. La cubren el argumento de `F3.md` y el conteo exacto de `DequeChaseLevConcurrenteTest`.

**No hecho:** CI con jcstress (`DESIGN.md` §10), y repetir en ARM.

## ¿Sirven las pruebas? Mutaciones

Para cada escenario de jcstress se rompió el deque a propósito y se comprobó que el escenario fallara. Detalle en `TESTING.md` y `F4.md`.

| Mutación | Escenario | Resultado |
|---|---|---|
| `pop` y `steal` sin CAS (M1, M2) | I1, I2, I3 | falla |
| `pop` sin restaurar `bottom` (M3, M5) | I1, I6 | falla |
| `push` publica `bottom` antes de la casilla (M4) | I4 | falla |
| `crecer` publica antes de copiar; copia con la máscara vieja (M6, M7) | I7, I8 | falla |
| `pop` con `setRelease` en vez de `setVolatile` (M8) | I10 | **falla** |
| `steal` sin `fullFence` (M9) | I10 | no falla en x86 |
| `bottom` y `casillas` sin release/acquire (M10, M11) | I5, I7, I8 | no falla en x86 |

Las mutaciones de lógica las ve cualquier máquina. De las de modo de acceso, solo M8 se ve en x86 (reordena escritura seguida de lectura); las otras no, y quedan para ARM.
