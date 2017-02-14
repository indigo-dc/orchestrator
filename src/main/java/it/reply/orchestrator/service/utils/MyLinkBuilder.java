package it.reply.orchestrator.service.utils;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
