/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.dal.repository;

import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface DeploymentScheduleRepository extends JpaRepository<DeploymentSchedule, String> {

  String IN_SAME_ORGANIZATION =
      "(d.owner is null "
          + "or (d.owner.organization = ?#{#requester.organization} "
          + "and d.owner.oidcEntityId.issuer = ?#{#requester.oidcEntityId.issuer}))";

  @Query("select d "
      + "from #{#entityName} d "
      + "where d.owner.oidcEntityId = ?#{#ownerId}") Page<DeploymentSchedule> findAllByOwner(
      @Param("ownerId") OidcEntityId ownerId,
      Pageable pageable);

  @Query("select d "
      + "from #{#entityName} d "
      + "where d.owner.oidcEntityId = ?#{#ownerId} "
      + "and " + IN_SAME_ORGANIZATION) Page<DeploymentSchedule> findAllByOwner(
      @Param("requester") OidcEntity requester,
      @Param("ownerId") OidcEntityId ownerId,
      Pageable pageable);

  @Query("select d "
      + "from #{#entityName} d "
      + "where d.id = ?#{#id} "
      + "and " + IN_SAME_ORGANIZATION) DeploymentSchedule findOne(
      @Param("requester") OidcEntity requester,
      @Param("id") String id);

  @Query("select d "
      + "from #{#entityName} d "
      + "where " + IN_SAME_ORGANIZATION) Page<DeploymentSchedule> findAll(
      @Param("requester") OidcEntity requester,
      Pageable pageable);

}
