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
 * I10: con dos tareas (1 y 2), {@code pop} toma la de {@code bottom} sin CAS si lee {@code t < b}.
 * Si la escritura de {@code bottom} se retrasara respecto de la lectura de {@code top}, {@code pop}
 * leería un {@code top} viejo mientras los dos ladrones, que aún ven el {@code bottom} viejo, se
 * llevan las dos tareas: la 2 saldría dos veces. Cada tarea debe salir exactamente una vez y el
 * árbitro no debe encontrar nada (r4 = 0).
 *
 * <p>El deque se crea y se llena en el constructor del {@code @State}; jcstress da happens-before
 * entre el constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "2, 1, 0, 0", expect = ACCEPTABLE, desc = "pop toma la 2; un ladrón la 1")
@Outcome(id = "2, 0, 1, 0", expect = ACCEPTABLE, desc = "pop toma la 2; el otro ladrón la 1")
@Outcome(id = "0, 1, 2, 0", expect = ACCEPTABLE, desc = "pop llega tarde; los ladrones se llevan las dos")
@Outcome(id = "0, 2, 1, 0", expect = ACCEPTABLE, desc = "pop llega tarde; los ladrones se llevan las dos")
@Outcome(expect = FORBIDDEN, desc = "alguna tarea dos veces o perdida")
@State
public class PopContraDosRobos {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(4);

    public PopContraDosRobos() {
        deque.push(1);
        deque.push(2);
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
