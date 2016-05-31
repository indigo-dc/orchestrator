package it.reply.orchestrator.utils;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import java.nio.file.Paths;

public class CommonUtils {
  public static String getFileContentAsString(String fileUri) throws FileException {
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }
}
