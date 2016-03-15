package it.reply.orchestrator.service;

import com.google.common.io.ByteStreams;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.FileUtil;

import it.reply.orchestrator.exception.service.TOSCAException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class ToscaServiceImpl implements ToscaService {

  private static final Logger LOG = LogManager.getLogger(ToscaServiceImpl.class);

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
      ParsingResult<Csar> result = archiveUploadService.upload(zipFile);
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", () -> normativeLocalName,
            () -> Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
    }

    try (InputStream is = cl.getResourceAsStream(basePath + "/" + indigoLocalName)) {
      Path zipFile = File.createTempFile(indigoLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService.upload(zipFile);
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", () -> indigoLocalName,
            () -> Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
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
  public ParsingResult<ArchiveRoot> getArchiveRootFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException {
    Path zipPath = Files.createTempFile("csar", ".zip");
    try (InputStream is = new ByteArrayInputStream(toscaTemplate.getBytes());) {
      zip(is, zipPath);
    }
    return parser.parse(zipPath);
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
    String template = writer.toString();
    LOG.debug(template);
    return template;
  }

  /**
   * Customize the template with INDIGO requirements, for example it adds the deploymentId.
   * 
   * @param toscaTemplate
   *          the TOSCA template
   * @param deploymentId
   *          the deploymentId
   * @return the customized template
   * 
   * @throws ParsingException
   *           if the template is not valid
   * @throws IOException
   *           if there is an IO error
   */
  @Override
  public String customizeTemplate(@Nonnull String toscaTemplate, @Nonnull String deploymentId)
      throws IOException {

    ParsingResult<ArchiveRoot> result = null;
    try {
      result = getArchiveRootFromTemplate(toscaTemplate);
    } catch (ParsingException e) {
      checkParsingErrors(e.getParsingErrors());
    }
    checkParsingErrors(result.getContext().getParsingErrors());

    addDeploymentId(result, deploymentId);

    return getTemplateFromTopology(result.getResult());

  }

  @Override
  public String updateTemplate(String template) throws IOException {
    ParsingResult<ArchiveRoot> result = null;
    try {
      result = getArchiveRootFromTemplate(template);
    } catch (ParsingException e) {
      checkParsingErrors(e.getParsingErrors());
    }
    checkParsingErrors(result.getContext().getParsingErrors());
    removeRemovalList(result);
    return getTemplateFromTopology(result.getResult());
  }

  private void checkParsingErrors(List<ParsingError> errorList) throws TOSCAException {
    String errorMessage = "";
    if (!errorList.isEmpty()) {
      for (ParsingError error : errorList) {
        if (!error.getErrorLevel().equals(ParsingErrorLevel.INFO)) {
          errorMessage = errorMessage + error.getErrorCode() + ": " + error.getNote() + "; ";
        }
      }
      throw new TOSCAException(errorMessage);
    }

  }

  private void addDeploymentId(ParsingResult<ArchiveRoot> parsingResult, String deploymentId) {
    Map<String, NodeTemplate> nodes = parsingResult.getResult().getTopology().getNodeTemplates();
    for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
      if (entry.getValue().getType().equals("tosca.nodes.indigo.ElasticCluster")) {
        // Create new property with the deploymentId and set as printable
        ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(deploymentId);
        scalarPropertyValue.setPrintable(true);
        entry.getValue().getProperties().put("deployment_id", scalarPropertyValue);
      }
    }
  }

  private void removeRemovalList(ParsingResult<ArchiveRoot> parsingResult) {
    Map<String, NodeTemplate> nodes = parsingResult.getResult().getTopology().getNodeTemplates();
    for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
      Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
      if (scalable != null && scalable.getProperties().containsKey("removal_list")) {
        scalable.getProperties().remove("removal_list");
      }
    }
  }

  private static void setAutentication() {
    Authentication auth = new PreAuthenticatedAuthenticationToken(Role.ADMIN.name().toLowerCase(),
        "", AuthorityUtils.createAuthorityList(Role.ADMIN.name()));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Override
  public Capability getNodeCapabilityByName(NodeTemplate node, String propertyName) {
    for (Entry<String, Capability> entry : node.getCapabilities().entrySet()) {
      if (entry.getKey().equals(propertyName))
        return entry.getValue();
    }
    return null;
  }

  // TODO merge with getCount
  public Map<String, NodeTemplate> getCountNodes(ArchiveRoot archiveRoot) {
    Map<String, NodeTemplate> nodes = new HashMap<>();

    for (Map.Entry<String, NodeTemplate> entry : archiveRoot.getTopology().getNodeTemplates()
        .entrySet()) {
      Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
      if (scalable != null) {
        ScalarPropertyValue scalarPropertyValue = (ScalarPropertyValue) scalable.getProperties()
            .get("count");
        // Check if this value is read from the template and is not a default value
        if (scalarPropertyValue.isPrintable()) {
          nodes.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return nodes;
  }

  @Override
  public int getCount(NodeTemplate nodeTemplate) {

    Capability scalable = getNodeCapabilityByName(nodeTemplate, "scalable");
    if (scalable != null) {
      ScalarPropertyValue scalarPropertyValue = (ScalarPropertyValue) scalable.getProperties()
          .get("count");
      // Check if this value is read from the template and is not a default value
      if (scalarPropertyValue.isPrintable()) {
        return Integer.parseInt(scalarPropertyValue.getValue());
      }
    }
    return -1;
  }

  @Override
  public List<String> getRemovalList(NodeTemplate nodeTemplate) {
    Capability scalable = getNodeCapabilityByName(nodeTemplate, "scalable");
    if (scalable != null) {
      ListPropertyValue listPropertyValue = (ListPropertyValue) scalable.getProperties()
          .get("removal_list");
      if (listPropertyValue != null) {
        return (List<String>) (List<?>) listPropertyValue.getValue();
      }
    }
    return new ArrayList<String>();
  }

  @Override
  public String updateCount(ArchiveRoot archiveRoot, int count) throws IOException {
    for (Map.Entry<String, NodeTemplate> entry : archiveRoot.getTopology().getNodeTemplates()
        .entrySet()) {
      Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
      if (scalable != null) {
        ScalarPropertyValue scalarPropertyValue = (ScalarPropertyValue) scalable.getProperties()
            .get("count");
        if (scalarPropertyValue.isPrintable()) {
          scalarPropertyValue.setValue(String.valueOf(count));
          scalable.getProperties().put("count", scalarPropertyValue);
        }
      }
    }
    return getTemplateFromTopology(archiveRoot);
  }

}
