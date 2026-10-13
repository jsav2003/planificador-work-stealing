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
 * I6: con el deque vacío, el dueño hace {@code pop} (r1, siempre 0) y luego {@code push(1)} mientras
 * un ladrón hace {@code steal} (r2). La tarea la tiene el ladrón o sigue en el deque para el árbitro
 * ({@code drenar}, r3); nunca se pierde ni se duplica.
 *
 * <p>Ejercita la restauración de {@code bottom} en {@code pop} cuando encuentra el deque vacío: si
 * {@code bottom} quedara en -1, el {@code push} siguiente escribiría fuera del rango {@code [top,
 * bottom)} y la tarea no se vería nunca.
 *
 * <p>El deque se crea en el constructor del {@code @State}; jcstress da happens-before entre el
 * constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "0, 1, 0", expect = ACCEPTABLE, desc = "la robó el ladrón")
@Outcome(id = "0, 0, 1", expect = ACCEPTABLE, desc = "el ladrón llegó antes del push; la tarea quedó para el árbitro")
@Outcome(expect = FORBIDDEN, desc = "tarea duplicada o perdida, o pop con algo")
@State
public class PopVacioLuegoPush {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    @Actor
    public void duenio(III_Result r) {
        r.r1 = Resultados.comoInt(deque.pop());
        deque.push(1);
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
