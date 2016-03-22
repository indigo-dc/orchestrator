package it.reply.orchestrator.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.reply.orchestrator.config.Application;
import it.reply.orchestrator.exception.http.OrchestratorApiException;

@RestController
@RequestMapping("/proxy")
@PropertySource("classpath:im-config/im-java-api.properties")
public class ProxyController {

  @Value("${file.directory}")
  private String DIR;

  private static final Logger LOG = LogManager.getLogger(ProxyController.class);

  @RequestMapping(method = RequestMethod.POST)
  public void handleFileUpload(@RequestParam("file") MultipartFile file) {
    if (!file.isEmpty()) {
      try {
        BufferedOutputStream stream = new BufferedOutputStream(
            new FileOutputStream(new File(DIR + File.separator + Application.PROXY)));
        FileCopyUtils.copy(file.getInputStream(), stream);
        stream.close();
      } catch (Exception e) {
        LOG.error(e);
        throw new OrchestratorApiException("Error durung the upload", e);

      }
    } else {
      throw new OrchestratorApiException("Failed to upload the file because the file was empty");
    }
  }

}
