package it.reply.orchestrator.config;

import it.reply.orchestrator.config.filters.CustomRequestLoggingFilter;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAppInitializer extends SpringBootServletInitializer {
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.applicationClass);
  }

  /**
   * Generate a WebAppInitializer.
   */
  public WebAppInitializer() {
    super();
    // The ErrorPageFilter get stuck on response when no body is returned
    setRegisterErrorPageFilter(false);
  }

  /**
   * Register the CustomRequestLoggingFilter.
   * 
   * @return the FilterRegistrationBean
   */
  @Bean
  public FilterRegistrationBean customRequestLoggingFilterRegistration() {
    FilterRegistrationBean registration = new FilterRegistrationBean();
    registration.setFilter(customRequestLoggingFilter());
    registration.addUrlPatterns("/*");
    registration.setName("customWebRequestLoggingFilter");
    registration.setOrder(FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER - 104);
    return registration;
  }

  /**
   * Create the CustomRequestLoggingFilter.
   * 
   * @return the CustomRequestLoggingFilter
   */
  @Bean
  public CustomRequestLoggingFilter customRequestLoggingFilter() {
    CustomRequestLoggingFilter loggingFilter = new CustomRequestLoggingFilter();
    loggingFilter.setMaxPayloadLength(-1);// Ints.checkedCast(WebMvcConfig.MAX_UPLOAD_SIZE));
    // loggingFilter.setHeadersToOmitt(Lists.newArrayList(HttpHeaders.AUTHORIZATION));
    return loggingFilter;
  }

}