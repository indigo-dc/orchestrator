package it.reply.orchestrator.service.security;

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
