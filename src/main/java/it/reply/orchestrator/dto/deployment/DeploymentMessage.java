package it.reply.orchestrator.dto.deployment;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl.IndigoJob;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A message containing all the information needed during Deployment WF.
 * 
 * @author l.biava
 *
 */
public class DeploymentMessage implements Serializable {

  private static final long serialVersionUID = 8003907220093782923L;

  /**
   * The internal deployment representation (stored in the DB).
   */
  private Deployment deployment;

  private String deploymentId;

  private DeploymentProvider deploymentProvider;

  private TemplateTopologicalOrderIterator templateTopologicalOrderIterator;

  private boolean createComplete;
  private boolean deleteComplete;
  private boolean pollComplete;
  private boolean skipPollInterval;

  private CloudProvider chosenCloudProvider;

  /**
   * TEMPORARY Chronos Job Graph (to avoid regenerating the template representation each time).
   */
  private Map<String, IndigoJob> chronosJobGraph;

  public Deployment getDeployment() {
    return deployment;
  }

  public void setDeployment(Deployment deployment) {
    this.deployment = deployment;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public DeploymentProvider getDeploymentProvider() {
    return deploymentProvider;
  }

  public void setDeploymentProvider(DeploymentProvider deploymentProvider) {
    this.deploymentProvider = deploymentProvider;
  }

  public TemplateTopologicalOrderIterator getTemplateTopologicalOrderIterator() {
    return templateTopologicalOrderIterator;
  }

  public void setTemplateTopologicalOrderIterator(
      TemplateTopologicalOrderIterator templateTopologicalOrderIterator) {
    this.templateTopologicalOrderIterator = templateTopologicalOrderIterator;
  }

  public Map<String, IndigoJob> getChronosJobGraph() {
    return chronosJobGraph;
  }

  public void setChronosJobGraph(Map<String, IndigoJob> chronosJobGraph) {
    this.chronosJobGraph = chronosJobGraph;
  }

  public boolean isCreateComplete() {
    return createComplete;
  }

  public void setCreateComplete(boolean createComplete) {
    this.createComplete = createComplete;
  }

  public boolean isDeleteComplete() {
    return deleteComplete;
  }

  public void setDeleteComplete(boolean deleteComplete) {
    this.deleteComplete = deleteComplete;
  }

  public boolean isPollComplete() {
    return pollComplete;
  }

  public void setPollComplete(boolean pollComplete) {
    this.pollComplete = pollComplete;
  }

  public boolean isSkipPollInterval() {
    return skipPollInterval;
  }

  public void setSkipPollInterval(boolean skipPollInterval) {
    this.skipPollInterval = skipPollInterval;
  }

  public CloudProvider getChosenCloudProvider() {
    return chosenCloudProvider;
  }

  public void setChosenCloudProvider(CloudProvider chosenCloudProvider) {
    this.chosenCloudProvider = chosenCloudProvider;
  }

  @Override
  public String toString() {
    return "DeploymentMessage [deploymentId=" + deploymentId + ", deploymentProvider="
        + deploymentProvider + ", templateTopologicalOrderIterator="
        + templateTopologicalOrderIterator + ", createComplete=" + createComplete
        + ", pollComplete=" + pollComplete + "]";
  }

  /**
   * Class to contain template's nodes in topological order and to allow to iterate on the list.
   * 
   * @author l.biava
   *
   */
  public static class TemplateTopologicalOrderIterator implements Serializable {

    private static final long serialVersionUID = 1557615023166610397L;

    /**
     * Template's nodes, topologically ordered.
     */
    List<Resource> topologicalOrder;

    int position = 0;

    public TemplateTopologicalOrderIterator(List<Resource> topologicalOrder) {
      this.topologicalOrder = topologicalOrder;
    }

    public int getPosition() {
      return position;
    }

    public List<Resource> getTopologicalOrder() {
      return topologicalOrder;
    }

    public int getNodeSize() {
      return topologicalOrder.size();
    }

    public synchronized boolean hasNext() {
      return topologicalOrder.size() - 1 > position;
    }

    /**
     * Get the node in the current position of the iterator. <br/>
     * <b>Note that the first time this method is called it returns the first element of the list,
     * or <tt>null</tt> if the list is empty</b>
     * 
     * @return the current node, or <tt>null</tt> if the list is empty.
     */
    public synchronized Resource getCurrent() {
      if (position >= topologicalOrder.size()) {
        return null;
      }
      return topologicalOrder.get(position);
    }

    /**
     * Get the next element of the collection (after incrementing the position pointer).
     * 
     * @return the next node, or <tt>null</tt> if there aren't any others.
     */
    public synchronized Resource getNext() {
      if (!hasNext()) {
        position++;
        return null;
      }
      position++;
      return topologicalOrder.get(position);
    }

    public synchronized void reset() {
      position = 0;
    }

    @Override
    public String toString() {
      return "TemplateTopologicalOrderStatus [topologicalOrder=" + topologicalOrder + ", position="
          + position + "]";
    }

  }

}
