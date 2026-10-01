package wsp.f2.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import wsp.f2.ColaMichaelScott;

/** Objeto con dos campos NORMALES que el productor escribe antes de encolarlo. */
final class Mensaje {
    int a;
    int b;
}

/**
 * Visibilidad del contenido (DESIGN §7.1, tercer ejemplo): quien desencola el mensaje ve
 * los dos campos que el productor escribió antes de encolarlo. Los campos no son
 * volatile: el happens-before lo pone la cola (escritura volatile de {@code siguiente}
 * → lectura volatile). Si una versión debilitada de la cola (ejercicio 5) pierde esa
 * garantía, este es el test que debe fallar; en un test normal pasaría igual.
 *
 * <p>r1 = a, r2 = b; (-1, -1) si el consumidor no vio nada.
 */
@JCStressTest
@Outcome(id = "-1, -1", expect = ACCEPTABLE, desc = "aún no estaba encolado")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "mensaje completo")
@Outcome(expect = FORBIDDEN, desc = "mensaje a medias: falta happens-before")
@State
public class VisibilidadDelContenido {
    final ColaMichaelScott<Mensaje> cola = new ColaMichaelScott<>();

    @Actor
    public void productor() {
        Mensaje m = new Mensaje();
        m.a = 1;
        m.b = 1;
        cola.encolar(m);
    }

    @Actor
    public void consumidor(II_Result r) {
        Mensaje m = cola.desencolar();
        if (m == null) {
            r.r1 = -1;
            r.r2 = -1;
        } else {
            r.r1 = m.a;
            r.r2 = m.b;
        }
    }
}
