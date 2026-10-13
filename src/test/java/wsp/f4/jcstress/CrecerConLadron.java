package wsp.f4.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;
import wsp.f3.DequeChaseLev;

/**
 * I7: el deque (capacidad 2) está lleno con 1 y 2. El dueño empuja un 4, lo que obliga a crecer, y
 * hace {@code pop} (r1); un ladrón hace {@code steal} (r2); el árbitro {@code drenar}la lo que queda
 * (r3). Nadie compite por la misma tarea: el dueño saca el 4, el ladrón se lleva el 1 y queda el 2.
 * Cualquier otra salida es una tarea perdida, duplicada o leída a medias durante el crecimiento.
 *
 * <p>Una sola salida aceptable es a propósito: lo que se mide es que no aparezca otra.
 *
 * <p>El deque se crea y se llena en el constructor del {@code @State}; jcstress da happens-before
 * entre el constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "4, 1, 2", expect = ACCEPTABLE, desc = "el dueño sacó el 4, el ladrón el 1, quedó el 2")
@Outcome(expect = FORBIDDEN, desc = "tarea perdida, duplicada o leída a medias al crecer")
@State
public class CrecerConLadron {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    public CrecerConLadron() {
        deque.push(1);
        deque.push(2);
    }

    @Actor
    public void duenio(III_Result r) {
        deque.push(4); // arreglo lleno: crece
        r.r1 = Resultados.comoInt(deque.pop());
    }

    @Actor
    public void ladron(III_Result r) {
        r.r2 = Resultados.comoInt(deque.steal());
    }

    @Arbiter
    public void arbitro(III_Result r) {
        r.r3 = Resultados.drenar(deque);
    }
}
