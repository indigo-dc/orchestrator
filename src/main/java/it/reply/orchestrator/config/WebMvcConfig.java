package it.reply.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
@ComponentScan(basePackages = { "it.reply.orchestrator", "it.reply.workflowmanager" })
public class WebMvcConfig extends WebMvcConfigurationSupport {

  public static final long MAX_UPLOAD_SIZE = 1024000;

  private static final String RESOURCES_LOCATION = "/resources/";
  private static final String RESOURCES_HANDLER = RESOURCES_LOCATION + "**";

  @Bean
  public static RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Override
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    RequestMappingHandlerMapping requestMappingHandlerMapping =
        super.requestMappingHandlerMapping();
    requestMappingHandlerMapping.setUseSuffixPatternMatch(false);
    requestMappingHandlerMapping.setUseTrailingSlashMatch(false);
    return requestMappingHandlerMapping;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler(RESOURCES_HANDLER).addResourceLocations(RESOURCES_LOCATION);
  }

  @Override
  public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    configurer.enable();
  }

  /**
   * Common MultipartResolver.
   * 
   * @return CommonsMultipartResolver
   */
  @Bean(name = "multipartResolver")
  public CommonsMultipartResolver resolver() {
    CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
    commonsMultipartResolver.setMaxUploadSize(MAX_UPLOAD_SIZE);
    return new CommonsMultipartResolver();
  }
}
