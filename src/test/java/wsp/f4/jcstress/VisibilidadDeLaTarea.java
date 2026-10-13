package wsp.f4.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import wsp.f3.DequeChaseLev;

/**
 * I5: el dueño crea una {@link Tarea}, escribe sus dos campos (planos, no {@code final}) y la empuja;
 * el ladrón que la roba ve los dos campos. r1 = a, r2 = b; (-1, -1) si el ladrón no vio nada.
 *
 * <p>El happens-before lo pone el deque: el {@code setRelease} de {@code bottom} en {@code push} y el
 * {@code getAcquire} del ladrón. Un resultado con un campo en 0 sería una tarea vista a medias.
 *
 * <p>El deque se crea en el constructor del {@code @State}; solo el actor dueño llama a
 * {@code push}, y no hay árbitro porque lo único que se mide es lo que ve el ladrón.
 */
@JCStressTest
@Outcome(id = "-1, -1", expect = ACCEPTABLE, desc = "el ladrón no la vio")
@Outcome(id = "1, 2", expect = ACCEPTABLE, desc = "tarea completa")
@Outcome(expect = FORBIDDEN, desc = "tarea a medias: falta happens-before")
@State
public class VisibilidadDeLaTarea {
    final DequeChaseLev<Tarea> deque = new DequeChaseLev<>(2);

    @Actor
    public void duenio() {
        Tarea t = new Tarea();
        t.a = 1;
        t.b = 2;
        deque.push(t);
    }

    @Actor
    public void ladron(II_Result r) {
        Tarea t = deque.steal();
        if (t == null) {
            r.r1 = -1;
            r.r2 = -1;
        } else {
            r.r1 = t.a;
            r.r2 = t.b;
        }
    }
}
