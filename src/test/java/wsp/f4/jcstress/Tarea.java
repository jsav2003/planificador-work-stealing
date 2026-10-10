package wsp.f4.jcstress;

/**
 * Tarea con dos campos {@code int} a propósito <b>no {@code final}</b>: con {@code final} la
 * publicación segura de campos finales taparía una barrera que falte (la F2 dejó esa sospecha).
 */
final class Tarea {
    int a;
    int b;
}
