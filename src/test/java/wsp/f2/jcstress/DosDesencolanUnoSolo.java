package wsp.f2.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import wsp.f2.ColaMichaelScott;

/**
 * El caso análogo a DESIGN §4.2 pero en la cola: queda un solo elemento y dos hilos
 * intentan sacarlo. Solo uno puede ganar. (1, 1) sería duplicado; (0, 0) sería perdido.
 */
@JCStressTest
@Outcome(id = "1, 0", expect = ACCEPTABLE, desc = "gana el primero")
@Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "gana el segundo")
@Outcome(id = "1, 1", expect = FORBIDDEN, desc = "elemento duplicado")
@Outcome(id = "0, 0", expect = FORBIDDEN, desc = "elemento perdido")
@State
public class DosDesencolanUnoSolo {
    final ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();

    public DosDesencolanUnoSolo() {
        cola.encolar(1);
    }

    @Actor
    public void a(II_Result r) {
        r.r1 = Resultados.comoInt(cola.desencolar());
    }

    @Actor
    public void b(II_Result r) {
        r.r2 = Resultados.comoInt(cola.desencolar());
    }
}
