package it.reply.orchestrator.dto.deployment;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

public class AwsSlaPlacementPolicy extends CredentialsAwareSlaPlacementPolicy {

  private static final long serialVersionUID = -7011571670045507898L;

  public AwsSlaPlacementPolicy(CredentialsAwareSlaPlacementPolicy policy) {
    super(policy.getNodes(), policy.getSlaId(), policy.getUsername(), policy.getPassword());
  }

  public String getAccessKey() {
    return this.getUsername();
  }

  public String getSecretKey() {
    return this.getPassword();
  }

}
