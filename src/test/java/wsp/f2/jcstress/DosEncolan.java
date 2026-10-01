package wsp.f2.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import wsp.f2.ColaMichaelScott;

/**
 * Dos hilos encolan a la vez. Después un árbitro vacía la cola. Aceptable: salen el 1 y
 * el 2, en cualquier orden, ninguno perdido ni duplicado. Prohibido todo lo demás.
 */
@JCStressTest
@Outcome(id = "1, 2", expect = ACCEPTABLE, desc = "1 primero")
@Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "2 primero")
@Outcome(expect = FORBIDDEN, desc = "elemento perdido, duplicado o inventado")
@State
public class DosEncolan {
    final ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();

    @Actor
    public void a() {
        cola.encolar(1);
    }

    @Actor
    public void b() {
        cola.encolar(2);
    }

    @Arbiter
    public void arbitro(II_Result r) {
        r.r1 = Resultados.comoInt(cola.desencolar());
        r.r2 = Resultados.comoInt(cola.desencolar());
    }
}
