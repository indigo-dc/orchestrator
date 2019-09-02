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

import lombok.Data;

import java.util.List;
import java.util.Map;

import it.reply.orchestrator.utils.ToscaConstants;

/**
 * Internal model for Qcg Job object
 * 
 */

@Data
public class QcgJob {

  public final String getToscaNodeName() {
    return ToscaConstants.Nodes.Types.QCG;
  }  
  
  //
  
  public QcgJob() {
	  attributes = null;
	  args = null;
	  environment = null;
	  cpus = null;
	  exit_code = null;
	  resubmit = null;
	  created_work_dir = null;	  
  }
  
  private String id;
  private Map<String,String> attributes; // ??
  private String user;
  private String state;
  private String operation;
  private String note;

  private String directory;
  private String executable;
  private List<String> args;
  private Map<String,String> environment;
  private String schema;

  private String operation_start;
  private String resource;
  private String queue;
  private String local_user;
  private String local_group;
  private String local_id;
  private String submit_time;
  private String start_time;
  private String finish_time;
  private String updated_time;
  private String eta;
  private String nodes;
  private Integer cpus;
  private Integer exit_code;
  private String errors;
  private Integer resubmit;
  private String work_dir;
  private Boolean created_work_dir;
  private String last_seen;
  
  // orchestrator fields
  private String taskId;
  
  
}