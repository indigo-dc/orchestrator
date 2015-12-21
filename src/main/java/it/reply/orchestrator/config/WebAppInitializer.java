package it.reply.orchestrator.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.ServletRegistration;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/*" };
  }

  @Override
  protected Class<?>[] getRootConfigClasses() {

    return new Class<?>[] { ApplicationConfig.class };
  }

  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class<?>[] { WebMvcConfig.class };
  }

  @Override
  protected Filter[] getServletFilters() {
    CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
    characterEncodingFilter.setEncoding("UTF-8");
    characterEncodingFilter.setForceEncoding(true);

    return new Filter[] { characterEncodingFilter };
  }

  /**
   * Set to throw a NoHandlerFoundException when no Handler was found.
   */
  @Override
  protected void customizeRegistration(ServletRegistration.Dynamic registration) {
    // registration.setInitParameter("defaultHtmlEscape", "true");
    // registration.setInitParameter("spring.profiles.active", "default");

    registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");

  }
}