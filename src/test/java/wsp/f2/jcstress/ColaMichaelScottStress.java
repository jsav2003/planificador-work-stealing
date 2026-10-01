package wsp.f2.jcstress;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import wsp.f2.ColaMichaelScott;

/**
 * F2, ejercicio 4: tests de jcstress de {@link ColaMichaelScott}. Cada uno convierte un
 * invariante en un escenario de dos hilos con resultados aceptables y prohibidos
 * (DESIGN §7.1). Se corren con {@code scripts/jcstress.ps1} (ver F2.md), no con
 * {@code mvn test}.
 *
 * <p>Convención: un {@code desencolar} que devuelve {@code null} se anota como 0.
 */
public final class ColaMichaelScottStress {

    private ColaMichaelScottStress() {}

    private static int comoInt(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * Dos hilos encolan a la vez. Después un árbitro vacía la cola. Aceptable: salen el 1 y
     * el 2, en cualquier orden, ninguno perdido ni duplicado. Prohibido todo lo demás.
     */
    @JCStressTest
    @Outcome(id = "1, 2", expect = ACCEPTABLE, desc = "1 primero")
    @Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "2 primero")
    @Outcome(expect = FORBIDDEN, desc = "elemento perdido, duplicado o inventado")
    @State
    public static class DosEncolan {
        final ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();

        @Actor
        public void a() {
            cola.encolar(1);
        }

        @Actor
        public void b() {
            cola.encolar(2);
        }

        @Arbiter
        public void arbitro(II_Result r) {
            r.r1 = comoInt(cola.desencolar());
            r.r2 = comoInt(cola.desencolar());
        }
    }

    /**
     * El caso análogo a DESIGN §4.2 pero en la cola: queda un solo elemento y dos hilos
     * intentan sacarlo. Solo uno puede ganar. (1, 1) sería duplicado; (0, 0) sería perdido.
     */
    @JCStressTest
    @Outcome(id = "1, 0", expect = ACCEPTABLE, desc = "gana el primero")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "gana el segundo")
    @Outcome(id = "1, 1", expect = FORBIDDEN, desc = "elemento duplicado")
    @Outcome(id = "0, 0", expect = FORBIDDEN, desc = "elemento perdido")
    @State
    public static class DosDesencolanUnoSolo {
        final ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();

        public DosDesencolanUnoSolo() {
            cola.encolar(1);
        }

        @Actor
        public void a(II_Result r) {
            r.r1 = comoInt(cola.desencolar());
        }

        @Actor
        public void b(II_Result r) {
            r.r2 = comoInt(cola.desencolar());
        }
    }

    /**
     * Encolar y desencolar a la vez sobre una cola vacía. El consumidor ve el 1 o ve vacío;
     * después el árbitro debe encontrar exactamente lo que el consumidor no se llevó.
     */
    @JCStressTest
    @Outcome(id = "1, 0", expect = ACCEPTABLE, desc = "el consumidor se lo llevó")
    @Outcome(id = "0, 1", expect = ACCEPTABLE, desc = "el consumidor llegó antes; queda para el árbitro")
    @Outcome(expect = FORBIDDEN, desc = "perdido o duplicado")
    @State
    public static class EncolarContraDesencolar {
        final ColaMichaelScott<Integer> cola = new ColaMichaelScott<>();

        @Actor
        public void productor() {
            cola.encolar(1);
        }

        @Actor
        public void consumidor(II_Result r) {
            r.r1 = comoInt(cola.desencolar());
        }

        @Arbiter
        public void arbitro(II_Result r) {
            r.r2 = comoInt(cola.desencolar());
        }
    }

    /** Objeto con dos campos NORMALES que el productor escribe antes de encolarlo. */
    static final class Mensaje {
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
    public static class VisibilidadDelContenido {
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
}
