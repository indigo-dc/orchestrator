package it.reply.orchestrator.config.security;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import it.reply.orchestrator.config.WebMvcConfig;
import it.reply.orchestrator.config.filters.CustomRequestLoggingFilter;

import org.springframework.http.HttpHeaders;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

  @Override
  protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
    CustomRequestLoggingFilter loggingFilter = new CustomRequestLoggingFilter();
    loggingFilter.setIncludeQueryString(true);
    loggingFilter.setIncludeHeaders(true);
    loggingFilter.setIncludeClientInfo(true);
    loggingFilter.setIncludeResponseStatus(true);
    loggingFilter.setIncludePayload(true);
    loggingFilter.setMaxPayloadLength(Ints.checkedCast(WebMvcConfig.MAX_UPLOAD_SIZE));
    loggingFilter.setIncludeResponseTime(true);
    loggingFilter.setHeadersToOmitt(Lists.newArrayList(HttpHeaders.AUTHORIZATION));
    FilterRegistration.Dynamic loggingFilterRegistration =
        servletContext.addFilter("logger", loggingFilter);
    loggingFilterRegistration.addMappingForUrlPatterns(null, false, "/*");
  }
}
