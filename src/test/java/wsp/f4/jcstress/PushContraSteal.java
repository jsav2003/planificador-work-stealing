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
 * I4: el dueño empuja un 1 y luego hace {@code pop} (r1) mientras un ladrón hace {@code steal} (r2).
 * La tarea la tiene exactamente uno de los dos, y el árbitro ({@code drenar}, r3) no encuentra nada:
 * ni duplicada, ni perdida, ni dentro del deque mientras {@code pop} dio vacío.
 *
 * <p>Ejercita el orden casilla → {@code bottom} de {@code push}: si el ladrón viera {@code bottom}
 * publicado antes que la casilla, robaría un {@code null} tras ganar el CAS y la tarea se perdería.
 *
 * <p>El deque se crea en el constructor del {@code @State}; jcstress da happens-before entre el
 * constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "1, 0, 0", expect = ACCEPTABLE, desc = "la sacó el dueño")
@Outcome(id = "0, 1, 0", expect = ACCEPTABLE, desc = "la robó el ladrón")
@Outcome(expect = FORBIDDEN, desc = "tarea duplicada, perdida, o dejada dentro del deque")
@State
public class PushContraSteal {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    @Actor
    public void duenio(III_Result r) {
        deque.push(1);
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
