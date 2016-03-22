package it.reply.orchestrator.service.deployment.providers;

@FunctionalInterface
interface Function<A, B, R> {
  public R apply(A a, B b);
}