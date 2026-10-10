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
 * I1: con una sola tarea, el dueño ({@code pop}) y un ladrón ({@code steal}) compiten por ella; la
 * tiene exactamente uno. Después el árbitro empuja un 4 y lo roba: el deque sigue usable (r3 = 4).
 *
 * <p>El deque se crea y se llena en el constructor del {@code @State}; jcstress da happens-before
 * entre el constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "1, 0, 4", expect = ACCEPTABLE, desc = "la ganó el dueño")
@Outcome(id = "0, 1, 4", expect = ACCEPTABLE, desc = "la ganó el ladrón")
@Outcome(expect = FORBIDDEN, desc = "los dos con la tarea, ninguno, o deque dañado")
@State
public class UltimaTareaPopContraSteal {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    public UltimaTareaPopContraSteal() {
        deque.push(1);
    }

    @Actor
    public void duenio(III_Result r) {
        r.r1 = Resultados.comoInt(deque.pop());
    }

    @Actor
    public void ladron(III_Result r) {
        r.r2 = Resultados.comoInt(deque.steal());
    }

    @Arbiter
    public void arbitro(III_Result r) {
        deque.push(4);
        r.r3 = Resultados.comoInt(deque.steal());
    }
}
