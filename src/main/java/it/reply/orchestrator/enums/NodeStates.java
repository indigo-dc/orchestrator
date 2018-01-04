/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.enums;

import lombok.Getter;

/**
 * Normative nodes states. For more details see @see <a href=
 * "http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/csprd02/TOSCA-Simple-Profile-YAML-v1.0-csprd02.html#_Toc445238244">
 * Tosca Simple Profile</a>
 * 
 */
public enum NodeStates {

  // Node is not yet created. Node only exists as a template definition.
  INITIAL(false),

  // Node is transitioning from initial state to created state.
  CREATING(true),

  // Node software has been installed.
  CREATED(false),

  // Node is transitioning from created state to configured state.
  CONFIGURING(true),

  // Node has been configured prior to being started.
  CONFIGURED(false),

  // Node is transitioning from configured state to started state.
  STARTING(true),

  // Node is started.
  STARTED(false),

  // Node is transitioning from its current state to a configured state.
  STOPPING(true),

  // Node is transitioning from its current state to one where it is deleted and its state is no
  // longer tracked by the instance model.
  DELETING(true),

  // Node is in an error state.
  ERROR(false);

  @Getter
  private final boolean transtional;

  NodeStates(boolean transtional) {
    this.transtional = transtional;
  }
}
