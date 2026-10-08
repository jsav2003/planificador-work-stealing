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
