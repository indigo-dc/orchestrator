package it.reply.orchestrator.service.security;

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
import com.google.gson.JsonObject;

import it.reply.orchestrator.dto.security.IndigoUserInfo;

import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.model.UserInfo;

public class IndigoUserInfoFetcher extends UserInfoFetcher {

  @Override
  protected UserInfo fromJson(JsonObject userInfoJson) {
    return IndigoUserInfo.fromJson(userInfoJson);
  }
}
