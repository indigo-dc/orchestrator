/*
 * Copyright © 2019 I.N.F.N.
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

package it.reply.orchestrator.dto.qcg;

import it.reply.orchestrator.utils.ToscaConstants;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class QcgJob {

  public final String getToscaNodeName() {
    return ToscaConstants.Nodes.Types.QCG;
  }

  /**
   * Creates the QcgJob object.
   */
  public QcgJob() {
    attributes = null;
    args = null;
    environment = null;
    cpus = null;
    exitcode = null;
    resubmit = null;
    createdworkdir = null;
  }

  private String id;
  private Map<String, String> attributes; // ??
  private String user;
  private String state;
  private String operation;
  private String note;

  private String directory;
  private String executable;
  private List<String> args;
  private Map<String, String> environment;
  private String schema;

  private String operationstart;
  private String resource;
  private String queue;
  private String localuser;
  private String localgroup;
  private String localid;
  private String submittime;
  private String starttime;
  private String finishtime;
  private String updatedtime;
  private String eta;
  private String nodes;
  private Integer cpus;
  private Integer exitcode;
  private String errors;
  private Integer resubmit;
  private String workdir;
  private Boolean createdworkdir;
  private String lastseen;

  // orchestrator fields
  private String taskId;

}
