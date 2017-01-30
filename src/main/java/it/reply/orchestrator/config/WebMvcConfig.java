package it.reply.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

  public static final long MAX_UPLOAD_SIZE = 1 * 1024 * 1024; // 1 MByte

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.setUseSuffixPatternMatch(false);
    configurer.setUseTrailingSlashMatch(true);
    super.configurePathMatch(configurer);
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
