# TESTING

Cómo se prueba el deque de Chase-Lev (`wsp.f3.DequeChaseLev`) y qué demuestra cada prueba. Texto de
Claude a petición de Sebastián (2026-10-15); no acredita comprensión del modelo de memoria.

## Cómo correr

| Qué | Comando |
|---|---|
| JUnit (secuencial y concurrente) | `mvn test` |
| JUnit también con el intérprete (sin JIT) | `mvn test -Pxint` |
| jcstress de la F4, rápido | `.\scripts\jcstress.ps1 -Tests "wsp.f4.*"` |
| jcstress de la F4, corrida del README | `.\scripts\jcstress.ps1 -Tests "wsp.f4.*" -Modo default` |
| Un escenario largo | `.\scripts\jcstress.ps1 -Tests "wsp.f4.jcstress.PopContraDosRobos" -Modo tough` |

jcstress no corre con `mvn test`: el script compila con el perfil `jcstress` en `target/jcstress-build`
y deja el informe HTML en `target/jcstress-results/index.html`. Los `jcstress-results-*.bin.gz` de la
raíz son salida del programa y están en `.gitignore`.

## Escenarios (`src/test/java/wsp/f4/jcstress/`)

Uno por archivo. Resultados enteros, `null` = 0; las tareas valen 1, 2, 4 y 8 para que la suma del
árbitro identifique el conjunto que quedó. Las salidas aceptables se listan una por una y todo lo demás
es `FORBIDDEN`. Montajes y salidas, escenario por escenario, en `F4.md` ("Escenarios").

| # | Archivo | Invariante |
|---|---|---|
| I1 | `UltimaTareaPopContraSteal` | última tarea: `pop` contra `steal`, sale una vez |
| I2 | `UltimaTareaDosLadrones` | última tarea: dos `steal` |
| I3 | `UltimaTareaTresActores` | última tarea: `pop` y dos `steal` |
| I4 | `PushContraSteal` | la tarea empujada no se pierde ni se duplica |
| I5 | `VisibilidadDeLaTarea` | quien la obtiene ve los campos escritos antes del `push` |
| I6 | `PopVacioLuegoPush` | `pop` vacío restaura `bottom`; el `push` posterior funciona |
| I7 | `CrecerConLadron` | crecer con un ladrón a mitad de robo |
| I8 | `CrecerConVuelta` | crecer copiando con vuelta del índice |
| I9 | — | `top` solo crece: sin escenario (argumento + conteo exacto de `DequeChaseLevConcurrenteTest`) |
| I10 | `PopContraDosRobos` | `pop` sin CAS contra dos ladrones |

## Mutaciones: ¿sirve cada escenario?

Cada mutación se aplicó en el árbol de trabajo, se corrió su escenario en `quick` y se revirtió
(`git diff src/main` vacío). Nunca se commiteó una mutación. Fechas: M1–M7 el 2026-10-13; M8–M11 y C1 el
2026-10-15. Salidas prohibidas observadas, en `F4.md`.

| # | Mutación | Escenario | Resultado |
|---|---|---|---|
| M1 | `pop`: CAS de la última tarea por `TOP.set(t + 1)` | I1, I3 | **falla** |
| M2 | `steal`: CAS por `TOP.set(t + 1)` | I2, I3 | **falla** |
| M3 | `pop`: sin restaurar `bottom` tras el CAS | I1 | **falla** |
| M4 | `push`: publicar `bottom` antes de escribir la casilla | I4 | **falla** |
| M5 | `pop`: sin restaurar `bottom` con el deque vacío | I6 | **falla** |
| M6 | `crecer`: publicar el arreglo antes de copiar | I7, I8 | **falla** |
| M7 | `crecer`: copiar con la máscara vieja | I8 | **falla** |
| M8 | `pop`: `setRelease` en vez de `setVolatile` sobre `bottom` | I10 | **falla** (`2, 1, 2, 0`, `2, 2, 1, 0`) |
| M9 | `steal`: sin el `fullFence` | I10 | no falla en x86 |
| M10 | `bottom` sin release/acquire (`set`/`get`) | I5 | no falla en x86 |
| M11 | `casillas` sin release/acquire (`set`/`get`) | I7, I8 | no falla en x86 |
| C1 | control del oráculo: campos escritos después de `push` | I5 | **falla** (`0, 0`, `0, 2`, `1, 0`) |

Lectura: M1–M7 son errores de lógica y los escenarios los atrapan. M8 es el único cambio de modo de
acceso que x86 deja ver (el reordenamiento escritura→lectura). M9–M11 no se ven en x86 porque no
reordena lectura con lectura ni escrituras entre sí: es el límite del README, no una prueba de que el
modo sobre. C1 comprueba el oráculo de I5 (detecta una tarea a medias), no la barrera.
