package it.reply.orchestrator.extra;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import it.reply.orchestrator.service.ToscaServiceTest;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateGeneratorHelper {
  public static void main(String args[]) throws FileException {
    System.out.println(generateChronosTemplate(1000));
  }

  public static String generateChronosTemplate(int nodeNumber) throws FileException {
    String baseTemplate = getFileContentAsString(
        ToscaServiceTest.TEMPLATES_BASE_DIR + "chronos_tosca_generator.yaml");

    Pattern p = Pattern.compile("(?s)<pattern>(.*?)<\\/pattern>");
    Matcher m = p.matcher(baseTemplate);

    m.find();
    String pattern = (m.group(1)).toString();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nodeNumber; i++) {
      sb.append(String.format(pattern, i));
    }

    return m.replaceFirst(sb.toString());
  }

  private static String getFileContentAsString(String fileUri) throws FileException {
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }
}
