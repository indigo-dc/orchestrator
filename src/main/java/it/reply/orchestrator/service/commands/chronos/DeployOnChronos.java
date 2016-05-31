package it.reply.orchestrator.service.commands.chronos;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.model.v1.Container;
import it.infn.ba.indigo.chronos.client.model.v1.Job;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.service.WorkflowConstants;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderService;
import it.reply.workflowmanager.spring.orchestrator.bpm.ejbcommands.BaseCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DeployOnChronos extends BaseCommand {

  private static final Logger LOG = LogManager.getLogger(DeployOnChronos.class);

  @Autowired
  @Qualifier("CHRONOS")
  private DeploymentProviderService chronosService;

  @Override
  protected ExecutionResults customExecute(CommandContext ctx) throws Exception {
    String deploymentId =
        (String) getWorkItem(ctx).getParameter(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID);
    boolean result = chronosService.doDeploy(deploymentId);
    return resultOccurred(result);
  }

  @Autowired
  private ChronosServiceImpl chronosServiceImpl;

  public void chronosHelloWorld(String deploymentId, boolean shouldFail) throws ChronosException {

    String jobName = deploymentId + "-test-job";
    Job job = new Job();
    job.setName(jobName);
    // job.setSchedule("R1/2015-12-25T17:22:00Z/PT5M");
    job.setSchedule("R1//PT2S"); // Start immediately
    Container container = new Container();
    container.setImage("libmesos/ubuntu");
    container.setType("DOCKER");
    job.setContainer(container);
    job.setCpus(0.5);
    job.setMem(512d);
    job.setRetries(3);
    job.setEpsilon("PT5S"); // Retry interval
    job.setCommand("i=0; while ((i<3)); do echo hello; sleep 5; ((i++)); done;"
        + (shouldFail ? " unknown_command" : ""));

    Chronos client = chronosServiceImpl.getChronosClient();
    client.createJob(job);
    LOG.info("Job {} created", jobName);

    Job js = null;
    do {
      js = client.getJob(jobName).iterator().next();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        ex = null;
      }
      // State = Fresh (success + error = 0), Success (success > 0), Failure (error > 0 )
      String state = "";
      if (js.getSuccessCount() > 0) {
        state = "SUCCESS";
      } else {
        if (js.getErrorCount() > 0) {
          if (js.getErrorCount() < js.getRetries()) {
            state = "RETRYING";
          } else {
            state = "FAILURE";
          }
        } else {
          state = "FRESH";
        }
      }

      LOG.info("Job {}: status {}, success {}, errors {}, retries {}", jobName,
          getLastState(job) + "(calc=" + state + ")", js.getSuccessCount(), js.getErrorCount(),
          js.getRetries());
    } while (js.getSuccessCount() < 1 && js.getErrorCount() < js.getRetries());

    LOG.info("Job {} result {}", jobName, js.getSuccessCount() > 0 ? "Success" : "Error");

    client.deleteJob(jobName);
    LOG.info("Job {} deleted", jobName);
  }

  public enum JobState {
    FRESH, FAILURE, SUCCESS;
  }

  /**
   * @see https://github.com/mesos/chronos/blob/fc2567dbfb0567fd35a496be09a39474c3d0a7c6/src/main/
   *      scala/org/apache/mesos/chronos/scheduler/jobs/graph/Exporter.scala#L33
   * @param job
   * @return
   */
  public static JobState getLastState(Job job) {
    if (job.getLastSuccess() == null && job.getLastError() == null)
      return JobState.FRESH;
    else if (job.getLastSuccess() == null)
      return JobState.FAILURE;
    else if (job.getLastError() == null)
      return JobState.SUCCESS;
    else {
      DateTimeFormatter dtf = ISODateTimeFormat.dateTimeNoMillis();
      DateTime lastSuccessTime = dtf.parseDateTime(job.getLastSuccess());
      DateTime lastErrorTime = dtf.parseDateTime(job.getLastError());
      if (lastSuccessTime.isAfter(lastErrorTime))
        return JobState.SUCCESS;
      else
        return JobState.FAILURE;
    }
  }
}
