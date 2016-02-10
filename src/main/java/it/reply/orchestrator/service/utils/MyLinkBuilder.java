package it.reply.orchestrator.service.utils;

import java.net.URI;

import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.web.util.UriComponentsBuilder;

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
    UriComponentsBuilder _builder = (UriComponentsBuilder) builder.clone();
    return new MyLinkBuilder(_builder);
  }

  public static MyLinkBuilder getNewBuilder(URI uri) {
    UriComponentsBuilder _builder = UriComponentsBuilder.fromUri(uri);
    return new MyLinkBuilder(_builder);
  }

}
