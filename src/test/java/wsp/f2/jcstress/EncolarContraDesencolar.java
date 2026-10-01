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
 * Encolar y desencolar a la vez sobre una cola vacía. El consumidor ve el 1 o ve vacío;
 * después el árbitro debe encontrar exactamente lo que el consumidor no se llevó.
 */
@JCStressTest
@Outcome(id = "1, 0", expect = ACCEPTABLE, desc = "el consumidor se lo llevó")
@Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "el consumidor llegó antes; queda para el árbitro")
@Outcome(expect = FORBIDDEN, desc = "perdido o duplicado")
@State
public class EncolarContraDesencolar {
    final ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();

    @Actor
    public void productor() {
        cola.encolar(1);
    }

    @Actor
    public void consumidor(II_Result r) {
        r.r1 = Resultados.comoInt(cola.desencolar());
    }

    @Arbiter
    public void arbitro(II_Result r) {
        r.r2 = Resultados.comoInt(cola.desencolar());
    }
}
