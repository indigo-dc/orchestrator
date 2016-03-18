package it.reply.orchestrator.service.utils;

import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class MyLinkBuilder extends LinkBuilderSupport<MyLinkBuilder> {

  public MyLinkBuilder(UriComponentsBuilder builder) {
    super(builder);
  }

  @Override
  protected MyLinkBuilder getThis() {
    return this;
  }

  @Override
  protected MyLinkBuilder createNewInstance(UriComponentsBuilder builder) {
    return new MyLinkBuilder(builder);
  }

  public static MyLinkBuilder getNewBuilder(UriComponentsBuilder builder) {
    UriComponentsBuilder uriComponentsBuilder = (UriComponentsBuilder) builder.clone();
    return new MyLinkBuilder(uriComponentsBuilder);
  }

  public static MyLinkBuilder getNewBuilder(URI uri) {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri);
    return new MyLinkBuilder(uriComponentsBuilder);
  }

}
