# planificador-work-stealing

Deque lock-free de Chase-Lev en Java (`push`, `pop`, `steal`, con crecimiento del arreglo),
probado con JUnit y jcstress. Diseño y plan por fases en `DESIGN.md`.

**Alcance recortado (2026-10-08):** el proyecto nació para demostrar la correctitud contra el
modelo de memoria de Java y llegar a un pool con work-stealing. Se redujo a implementar y probar el
deque. No hay pool ni mediciones con JMH. Los textos sobre el modelo de memoria (`F1-explicacion.md`
y las justificaciones de modos de acceso) los redactó Claude a petición, y **no acreditan que el
autor pueda explicarlos**; la corrección que se afirma aquí se apoya en las pruebas y en sus
límites (ver abajo), no en una demostración propia.

> Borrador: la tabla de resultados de jcstress llega en la F4.

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
