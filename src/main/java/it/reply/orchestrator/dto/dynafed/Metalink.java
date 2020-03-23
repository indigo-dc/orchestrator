/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.dynafed;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@Builder
@JacksonXmlRootElement(localName = "metalink", namespace = "http://www.metalinker.org/")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Metalink {

  @NonNull
  @NotNull
  @JacksonXmlProperty(localName = "files")
  @Size(min = 1)
  @Builder.Default
  private List<File> files = new ArrayList<>();

  @Deprecated
  private Metalink() {
    files = new ArrayList<>();
  }

  @Data
  @Builder
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  public static class File {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    @NonNull
    @NotNull
    private String name;

    @JacksonXmlProperty(localName = "size")
    @NonNull
    @NotNull
    private Long size;

    @JacksonXmlProperty(localName = "url")
    @JacksonXmlElementWrapper(localName = "resources")
    @NonNull
    @NotNull
    @Size(min = 1)
    @Builder.Default
    private List<Url> urls = new ArrayList<>();

    @Deprecated
    private File() {
      urls = new ArrayList<>();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Url {

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    @NonNull
    @NotNull
    private String type;

    @JacksonXmlText
    @NonNull
    @NotNull
    private URI value;
  }
}
