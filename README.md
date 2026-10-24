# planificador-work-stealing

[Español](README.es.md) · **English**

A lock-free Chase-Lev deque in Java (`push`, `pop`, `steal`, with array growth), tested with
JUnit and jcstress. Design and phase plan in `DESIGN.md`.

## Where it is and how to run it

The deque is `src/main/java/wsp/f3/DequeChaseLev.java`. Packages `wsp.f0` to `wsp.f2` are the
earlier exercises (basic concurrency, memory model, Michael-Scott queue). Requires Java 21 or
later and Maven.

- `mvn test`: sequential and concurrent JUnit tests of the deque (and of the earlier phases).
- `.\scripts\jcstress.ps1 -Tests "wsp.f4.*"`: the jcstress scenarios, in quick mode. jcstress
  does not run under `mvn test`.

All commands, scenarios and mutations are in `TESTING.md`.

**Scope cut (2026-10-08):** the project started out to demonstrate correctness against the Java
memory model and to reach a work-stealing pool. It was reduced to implementing and testing the
deque. There is no pool and no JMH measurement. The texts about the memory model
(`F1-explicacion.md` and the justifications of access modes) were written by Claude on request,
and **do not show that the author can explain them**; the correctness claimed here rests on the
tests and their limits (see below), not on a proof of the author's own.

> The documentation files (`DESIGN.md`, `F0.md` to `F4.md`, `TESTING.md`) are in Spanish; this
> README is also available in Spanish as [`README.es.md`](README.es.md).

**Final run** (2026-10-15, `default` mode, `wsp.f4.*`): AMD Ryzen 5 7520U (4 cores / 8 threads),
Windows 11, Java 23.0.1, jcstress 0.16. **482 configurations, 482 passed, 0 failed, 0 errors;
zero forbidden outcomes** (x86 only, see the known limit). Every acceptable outcome of each
scenario was observed, except that I4 produced `0, 1, 0` only 293,598 times out of
2,056,972,934 (the thief rarely wins).

| # | Scenario | Acceptable outcomes observed | Samples | Forbidden |
|---|---|---|---|---|
| I1 | `UltimaTareaPopContraSteal` | `1, 0, 4`; `0, 1, 4` | 1,549,314,694 | 0 |
| I2 | `UltimaTareaDosLadrones` | `1, 0, 0`; `0, 1, 0` | 1,651,939,974 | 0 |
| I3 | `UltimaTareaTresActores` | `1, 0, 0, 0`; `0, 1, 0, 0`; `0, 0, 1, 0` | 4,556,081,422 | 0 |
| I4 | `PushContraSteal` | `1, 0, 0`; `0, 1, 0` | 2,056,972,934 | 0 |
| I5 | `VisibilidadDeLaTarea` | `-1, -1`; `1, 2` | 2,738,188,934 | 0 |
| I6 | `PopVacioLuegoPush` | `0, 1, 0`; `0, 0, 1` | 2,088,368,774 | 0 |
| I7 | `CrecerConLadron` | `4, 1, 2` | 1,632,135,814 | 0 |
| I8 | `CrecerConVuelta` | `8, 2, 4, 0` | 1,014,561,414 | 0 |
| I10 | `PopContraDosRobos` | `2, 1, 0, 0`; `2, 0, 1, 0`; `0, 1, 2, 0`; `0, 2, 1, 0` | 4,937,152,782 | 0 |

I9 has no scenario (see below).

## Known limit: the tests were run on x86 only

Every jcstress result in this repository comes from one x86 machine (Windows, Java 23). x86 has
a strong memory model (TSO): it does not reorder stores with each other, nor loads that depend
on one another. That is why many ordering bugs **cannot show up** there, and a green test does
not prove the code is correct on ARM or other weakly ordered architectures.

Evidence (F2, 2026-10-02): on the Michael-Scott queue, three increasingly aggressive mutations
were tried (dropping the `volatile` on `siguiente`; publishing with a plain write instead of a
CAS; and in addition making `item` non-`final` and reading it with a plain read).
`VisibilidadDelContenido` stayed green (14 of 14 configurations) in all three. We do not know
which of them are real bugs under the memory model (the `final` field might protect the
contents); what we do know is that on this machine the test cannot tell any of them apart from
the original code.

Consequences:

- "Zero forbidden outcomes" means **zero on x86**, not zero in general.
- The correctness of the access modes rests on `happens-before` reasoning (justified in
  `F2.md`), not on the tests.
- Repeating the F4 suite on an ARM machine, where failures could appear, has still not been
  done.

Evidence from F4 (2026-10-15): x86 **does** show one reordering, a store followed by a load of
another variable. Changing `setVolatile` to `setRelease` in `pop` (M8) makes scenario I10
produce `2, 1, 2, 0` and `2, 2, 1, 0` (one task comes out twice) in `quick` mode. It is the
first evidence that jcstress sees an access-mode failure on this machine. The other access-mode
mutations (M9 to M11) did not fail.

**I9 (`top` only grows, ABA)** has no jcstress scenario: it is a property of all executions. It
is covered by the argument in `F3.md` and by the exact count in
`DequeChaseLevConcurrenteTest`.

**Not done:** CI with jcstress (`DESIGN.md` §10), and repeating on ARM.

## Do the tests work? Mutations

For each jcstress scenario the deque was broken on purpose and the scenario was checked to fail.
Details in `TESTING.md` and `F4.md`.

| Mutation | Scenario | Result |
|---|---|---|
| `pop` and `steal` without CAS (M1, M2) | I1, I2, I3 | fails |
| `pop` without restoring `bottom` (M3, M5) | I1, I6 | fails |
| `push` publishes `bottom` before the slot (M4) | I4 | fails |
| `crecer` publishes before copying; copies with the old mask (M6, M7) | I7, I8 | fails |
| `pop` with `setRelease` instead of `setVolatile` (M8) | I10 | **fails** |
| `steal` without `fullFence` (M9) | I10 | does not fail on x86 |
| `bottom` and `casillas` without release/acquire (M10, M11) | I5, I7, I8 | does not fail on x86 |
| Control C1: task fields written after `push` | I5 | fails |

Logic mutations are visible on any machine. Of the access-mode ones, only M8 shows on x86 (it
reorders a store followed by a load); the others do not, and are left for ARM.

## License

[MIT](LICENSE)
