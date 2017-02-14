package it.reply.orchestrator.extra;

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
import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import it.reply.orchestrator.service.ToscaServiceTest;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateGeneratorHelper {
  public static void main(String[] args) throws FileException {
    System.out.println(
        generateChronosTemplate(10000).replace(System.getProperty("line.separator"), "\\n"));
  }

  public static String generateChronosTemplate(int nodeNumber) throws FileException {
    String baseTemplate = getFileContentAsString(
        ToscaServiceTest.TEMPLATES_BASE_DIR + "chronos_tosca_generator.yaml");

    Pattern pat = Pattern.compile("(?s)<pattern>(.*?)<\\/pattern>");
    Matcher matcher = pat.matcher(baseTemplate);

    matcher.find();
    String pattern = (matcher.group(1)).toString();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nodeNumber; i++) {
      sb.append(String.format(pattern, i));
    }

    return matcher.replaceFirst(sb.toString());
  }

  private static String getFileContentAsString(String fileUri) throws FileException {
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }
}
