/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.policies;

import it.reply.orchestrator.utils.ToscaConstants.Policies.Types;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@Setter
public class CredentialsAwareSlaPlacementPolicy extends SlaPlacementPolicy {

  @NotNull
  @Nonnull
  private String username;

  @NotNull
  @Nonnull
  private String password;

  @Nullable
  private String tenant;

  @Deprecated
  protected CredentialsAwareSlaPlacementPolicy() {
    super(Types.CREDENTIALS_AWARE_SLA_PLACEMENT);
  }

  protected CredentialsAwareSlaPlacementPolicy(
      @NonNull Set<String> targets,
      @NonNull String slaId,
      @NonNull String username,
      @NonNull String password,
      @Nullable String tenant
  ) {
    super(Types.CREDENTIALS_AWARE_SLA_PLACEMENT, targets, slaId);
    this.username = Objects.requireNonNull(username);
    this.password = Objects.requireNonNull(password);
    this.tenant = tenant;
  }

}
