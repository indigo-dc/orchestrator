package it.reply.orchestrator.service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.FileUtil;

@Service
public class ToscaServiceImpl implements ToscaService {

  @Resource
  private ArchiveParser parser;

  @Autowired
  private ArchiveUploadService archiveUploadService;

  @Value("${directories.alien}/${directories.csar_repository}")
  private String alienRepoDir;

  @Value("${tosca.definitions.basepath}")
  private String basePath;
  @Value("${tosca.definitions.normative}")
  private String normativeLocalName;
  @Value("${tosca.definitions.indigo}")
  private String indigoLocalName;

  @PostConstruct
  public void init() throws IOException, CSARVersionAlreadyExistsException, ParsingException {
    if (Files.exists(Paths.get(alienRepoDir))) {
      FileUtil.delete(Paths.get(alienRepoDir));
    }

    setAutentication();

    ClassLoader cl = this.getClass().getClassLoader();

    try (InputStream is = cl.getResourceAsStream(basePath + "/" + normativeLocalName)) {
      Path zipFile = File.createTempFile(normativeLocalName, ".zip").toPath();
      zip(is, zipFile);
      archiveUploadService.upload(zipFile);
    }

    try (InputStream is = cl.getResourceAsStream(basePath + "/" + indigoLocalName)) {
      Path zipFile = File.createTempFile(indigoLocalName, ".zip").toPath();
      zip(is, zipFile);
      archiveUploadService.upload(zipFile);
    }

  }

  public static void zip(@Nonnull InputStream fileStream, @Nonnull Path outputPath)
      throws IOException {
    FileUtil.touch(outputPath);
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(
        new BufferedOutputStream(Files.newOutputStream(outputPath)))) {
      zipOutputStream.putNextEntry(new ZipEntry("definition.yml"));
      ByteStreams.copy(fileStream, zipOutputStream);
      zipOutputStream.closeEntry();
      zipOutputStream.flush();
    }
  }

  @Override
  @Nonnull
  public ArchiveRoot getArchiveRootFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException {
    Path zipPath = Files.createTempFile("csar", ".zip");
    try (InputStream is = new ByteArrayInputStream(toscaTemplate.getBytes());) {
      zip(is, zipPath);
    }
    ParsingResult<ArchiveRoot> parsingResult = parser.parse(zipPath);
    ArchiveRoot archiveRoot = parsingResult.getResult();
    // topology.setId(UUID.randomUUID().toString());
    return archiveRoot;
  }

  @Override
  @Nonnull
  public String getTemplateFromTopology(@Nonnull ArchiveRoot archiveRoot) throws IOException {
    Map<String, Object> velocityCtx = new HashMap<>();
    velocityCtx.put("tosca_definitions_version",
        archiveRoot.getArchive().getToscaDefinitionsVersion());
    velocityCtx.put("description", archiveRoot.getArchive().getDescription());
    velocityCtx.put("template_name", archiveRoot.getArchive().getName());
    velocityCtx.put("template_version", archiveRoot.getArchive().getVersion());
    velocityCtx.put("template_author", archiveRoot.getArchive().getTemplateAuthor());
    velocityCtx.put("repositories", archiveRoot.getArchive().getRepositories());
    velocityCtx.put("topology", archiveRoot.getTopology());
    StringWriter writer = new StringWriter();
    VelocityUtil.generate("templates/topology-1_0_0_INDIGO.yml.vm", writer, velocityCtx);
    return writer.toString();
  }

  private static void setAutentication() {
    Authentication auth = new PreAuthenticatedAuthenticationToken(Role.ADMIN.name().toLowerCase(),
        "", AuthorityUtils.createAuthorityList(Role.ADMIN.name()));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
