package it.reply.orchestrator.config.security;

import it.reply.orchestrator.config.filters.CustomRequestLoggingFilter;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

  @Override
  protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
    CustomRequestLoggingFilter loggingFilter = new CustomRequestLoggingFilter();
    loggingFilter.setMaxPayloadLength(-1);// Ints.checkedCast(WebMvcConfig.MAX_UPLOAD_SIZE));
    // loggingFilter.setHeadersToOmitt(Lists.newArrayList(HttpHeaders.AUTHORIZATION));
    FilterRegistration.Dynamic loggingFilterRegistration =
        servletContext.addFilter("logger", loggingFilter);
    loggingFilterRegistration.addMappingForUrlPatterns(null, false, "/*");
  }
}
