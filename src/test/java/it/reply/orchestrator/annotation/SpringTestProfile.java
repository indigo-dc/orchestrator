package it.reply.orchestrator.annotation;

import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Profile(SpringTestProfile.PROFILE_QUALIFIER)
public @interface SpringTestProfile {
  public final static String PROFILE_QUALIFIER = "test";
}
