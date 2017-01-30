package it.reply.orchestrator.annotation;

import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Profile(SpringDefaultProfile.PROFILE_QUALIFIER)
public @interface SpringDefaultProfile {
  public static final String PROFILE_QUALIFIER = "default";
}
