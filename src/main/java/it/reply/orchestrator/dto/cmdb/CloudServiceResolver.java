/*
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

package it.reply.orchestrator.dto.cmdb;

import static it.reply.orchestrator.dto.cmdb.CloudService.AWS_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.AZURE_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.CDMI_STORAGE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.CHRONOS_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.MARATHON_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.OCCI_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.ONEPROVIDER_STORAGE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.OPENNEBULA_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.OPENNEBULA_TOSCA_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.OPENSTACK_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.OTC_COMPUTE_SERVICE;
import static it.reply.orchestrator.dto.cmdb.CloudService.QCG_COMPUTE_SERVICE;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

public class CloudServiceResolver extends TypeIdResolverBase {

  private JavaType superType;

  @Override
  public void init(JavaType baseType) {
    superType = baseType;
  }

  @Override
  public String idFromValue(Object value) {
    return ((CloudService) value).getServiceType();
  }

  @Override
  public String idFromValueAndType(Object value, Class<?> suggestedType) {
    return idFromValue(value);
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) {
    final Class<?> subType;
    switch (id) {
      case MARATHON_COMPUTE_SERVICE:
        subType = MarathonService.class;
        break;
      case CHRONOS_COMPUTE_SERVICE:
        subType = ChronosService.class;
        break;
      case QCG_COMPUTE_SERVICE:
        subType = QcgService.class;
        break;
      case OCCI_COMPUTE_SERVICE:
      case OPENNEBULA_COMPUTE_SERVICE:
      case OPENNEBULA_TOSCA_SERVICE:
      case OPENSTACK_COMPUTE_SERVICE:
      case AWS_COMPUTE_SERVICE:
      case AZURE_COMPUTE_SERVICE:
      case OTC_COMPUTE_SERVICE:
        subType = ComputeService.class;
        break;
      case CDMI_STORAGE_SERVICE:
      case ONEPROVIDER_STORAGE_SERVICE:
        subType = StorageService.class;
        break;
      default:
        subType = CloudService.class;
    }
    return context.constructSpecializedType(superType, subType);
  }

  @Override
  public Id getMechanism() {
    return Id.NAME;
  }
}
