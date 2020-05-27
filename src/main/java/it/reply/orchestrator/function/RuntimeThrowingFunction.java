package it.reply.orchestrator.function;

@FunctionalInterface
public interface RuntimeThrowingFunction<T, R> extends ThrowingFunction<T, R, RuntimeException> {
  R apply(T param);

}
