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
 * I8: crecer cuando el contenido da la vuelta al arreglo. En el constructor: capacidad 2,
 * {@code push(1)}, {@code push(2)}, {@code steal} y {@code push(4)}; queda {@code top} = 1, el 2 en
 * la casilla 1 y el 4 en la casilla 0 (por la vuelta). Con la máscara vieja y la nueva distintas, una
 * copia que use la máscara equivocada coloca mal el 4.
 *
 * <p>El dueño empuja un 8 (crece, copiando con vuelta) y hace {@code pop} (r1); un ladrón roba dos
 * veces (r2, r3); el árbitro {@code drenar}la lo que queda (r4). Nadie compite por la misma tarea: el
 * dueño saca el 8 y el ladrón se lleva el 2 y el 4 en ese orden.
 *
 * <p>Una sola salida aceptable es a propósito: lo que se mide es que no aparezca otra.
 *
 * <p>El deque se crea y se llena en el constructor del {@code @State}; jcstress da happens-before
 * entre el constructor, los actores y el árbitro, y nunca hay dos hilos haciendo de dueño a la vez.
 */
@JCStressTest
@Outcome(id = "8, 2, 4, 0", expect = ACCEPTABLE, desc = "el dueño sacó el 8, el ladrón el 2 y el 4")
@Outcome(expect = FORBIDDEN, desc = "tarea perdida, duplicada o mal colocada al crecer con vuelta")
@State
public class CrecerConVuelta {
    final DequeChaseLev<Integer> deque = new DequeChaseLev<>(2);

    public CrecerConVuelta() {
        deque.push(1);
        deque.push(2);
        deque.steal(); // top = 1
        deque.push(4); // casilla 0, con vuelta
    }

    @Actor
    public void duenio(IIII_Result r) {
        deque.push(8); // arreglo lleno: crece copiando con vuelta
        r.r1 = Resultados.comoInt(deque.pop());
    }

    @Actor
    public void ladron(IIII_Result r) {
        r.r2 = Resultados.comoInt(deque.steal());
        r.r3 = Resultados.comoInt(deque.steal());
    }

    @Arbiter
    public void arbitro(IIII_Result r) {
        r.r4 = Resultados.drenar(deque);
    }
}
