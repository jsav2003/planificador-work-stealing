package wsp.f4.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;
import wsp.f3.DequeChaseLev;

/**
 * I3: con una sola tarea, el dueño ({@code pop}) y dos ladrones ({@code steal}) compiten por ella;
 * la tiene exactamente uno de los tres (r1–r3) y el árbitro no encuentra nada (r4 = 0).
 *
 * <p>El deque se crea y se llena en el constructor del {@code @State}; jcstress da happens-before
 * entre el constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "1, 0, 0, 0", expect = ACCEPTABLE, desc = "la ganó el dueño")
@Outcome(id = "0, 1, 0, 0", expect = ACCEPTABLE, desc = "la ganó el primer ladrón")
@Outcome(id = "0, 0, 1, 0", expect = ACCEPTABLE, desc = "la ganó el segundo ladrón")
@Outcome(expect = FORBIDDEN, desc = "más de uno con la tarea, o ninguno")
@State
public class UltimaTareaTresActores {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    public UltimaTareaTresActores() {
        deque.push(1);
    }

    @Actor
    public void duenio(IIII_Result r) {
        r.r1 = Resultados.comoInt(deque.pop());
    }

    @Actor
    public void ladron1(IIII_Result r) {
        r.r2 = Resultados.comoInt(deque.steal());
    }

    @Actor
    public void ladron2(IIII_Result r) {
        r.r3 = Resultados.comoInt(deque.steal());
    }

    @Arbiter
    public void arbitro(IIII_Result r) {
        r.r4 = Resultados.drenar(deque);
    }
}
