package it.reply.orchestrator.dto.security;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.UserInfo;

import java.util.List;

public class IndigoUserInfo extends DefaultUserInfo {

  private static final long serialVersionUID = -6165120150633146681L;

  private static final String GROUPS_KEY = "groups";
  private static final String ORGANIZATION_NAME_KEY = "organisation_name";
  private List<Group> groups;
  private String organizationName;

  public IndigoUserInfo(UserInfo other) {
    this.setSub(other.getSub());
    this.setPreferredUsername(other.getPreferredUsername());
    this.setName(other.getName());
    this.setGivenName(other.getGivenName());
    this.setFamilyName(other.getFamilyName());
    this.setMiddleName(other.getMiddleName());
    this.setNickname(other.getNickname());
    this.setProfile(other.getProfile());
    this.setPicture(other.getPicture());
    this.setWebsite(other.getWebsite());
    this.setEmail(other.getEmail());
    this.setEmailVerified(other.getEmailVerified());
    this.setGender(other.getGender());
    this.setZoneinfo(other.getZoneinfo());
    this.setLocale(other.getLocale());
    this.setPhoneNumber(other.getPhoneNumber());
    this.setPhoneNumberVerified(other.getPhoneNumberVerified());
    this.setAddress(other.getAddress());
    this.setUpdatedTime(other.getUpdatedTime());
    this.setBirthdate(other.getBirthdate());
    this.setSource(other.getSource());
  }

  public List<Group> getGroups() {
    return groups;
  }

  public void setGroups(List<Group> groups) {
    this.groups = groups;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  @Override
  public JsonObject toJson() {
    if (this.getSource() == null) {
      JsonObject result = super.toJson();
      if (groups != null) {
        JsonArray groupsJson = new JsonArray();
        for (Group g : groups) {
          groupsJson.add(g.toJson());
        }
        result.add(GROUPS_KEY, groupsJson);
      }
      if (organizationName != null) {
        result.addProperty(ORGANIZATION_NAME_KEY, organizationName);
      }
      return result;
    } else {
      return this.getSource();
    }
  }

  public static UserInfo fromJson(JsonObject obj) {
    IndigoUserInfo result = new IndigoUserInfo(DefaultUserInfo.fromJson(obj));
    if (obj.has(GROUPS_KEY) && obj.get(GROUPS_KEY).isJsonArray()) {
      List<Group> groups = Lists.newArrayList();
      JsonArray groupsJson = obj.getAsJsonArray(GROUPS_KEY);
      for (JsonElement groupJson : groupsJson) {
        if (groupJson.isJsonObject()) {
          groups.add(Group.fromJson(groupJson.getAsJsonObject()));
        }
      }
      if (!groups.isEmpty()) {
        result.setGroups(groups);
      }
    }
    result.setOrganizationName(
        obj.has(ORGANIZATION_NAME_KEY) && obj.get(ORGANIZATION_NAME_KEY).isJsonPrimitive()
            ? obj.get(ORGANIZATION_NAME_KEY).getAsString() : null);
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    if (groups != null) {
      for (Group g : groups) {
        result = prime * result + ((g == null) ? 0 : g.hashCode());
      }
    }
    result = prime * result + ((organizationName == null) ? 0 : organizationName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof IndigoUserInfo)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    IndigoUserInfo other = (IndigoUserInfo) obj;
    if (groups == null) {
      if (other.groups != null) {
        return false;
      }
    } else {
      if (!groups.equals(other.groups)) {
        return false;
      }
    }
    if (organizationName == null) {
      if (other.organizationName != null) {
        return false;
      }
    } else if (!organizationName.equals(other.organizationName)) {
      return false;
    }
    return true;
  }
}
