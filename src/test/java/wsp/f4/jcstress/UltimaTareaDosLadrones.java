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
 * I2: con una sola tarea, dos ladrones compiten por ella con {@code steal}; la tiene exactamente
 * uno y el árbitro la ve ya robada (r3 = 0).
 *
 * <p>El deque se crea y se llena en el constructor del {@code @State}; jcstress da happens-before
 * entre el constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "1, 0, 0", expect = ACCEPTABLE, desc = "la ganó el primer ladrón")
@Outcome(id = "0, 1, 0", expect = ACCEPTABLE, desc = "la ganó el segundo ladrón")
@Outcome(expect = FORBIDDEN, desc = "robada dos veces, o perdida")
@State
public class UltimaTareaDosLadrones {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    public UltimaTareaDosLadrones() {
        deque.push(1);
    }

    @Actor
    public void ladron1(III_Result r) {
        r.r1 = Resultados.comoInt(deque.steal());
    }

    @Actor
    public void ladron2(III_Result r) {
        r.r2 = Resultados.comoInt(deque.steal());
    }

    @Arbiter
    public void arbitro(III_Result r) {
        r.r3 = Resultados.drenar(deque);
    }
}
