package it.reply.orchestrator.dto.security;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.UserInfo;

import java.util.List;

public class IndigoUserInfo extends DefaultUserInfo {

  private static final long serialVersionUID = -6165120150633146681L;

  private static final String GROUPS_KEY = "groups";
  private static final String ORGANIZATION_NAME_KEY = "organisation_name";
  private List<String> groups;
  private String organizationName;

  /**
   * Create an IndigoUserInfo copying the fields from a {@link UserInfo} object.
   * 
   * @param other
   *          the {@link UserInfo} to copy from.
   */
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

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
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
        for (String g : groups) {
          groupsJson.add(new JsonPrimitive(g));
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

  /**
   * Create a {@link UserInfo} from its JSON representation.
   * 
   * @param obj
   *          {@link JsonObject} containing the JSON representation.
   * @return the UserInfo.
   */
  public static UserInfo fromJson(JsonObject obj) {
    IndigoUserInfo result = new IndigoUserInfo(DefaultUserInfo.fromJson(obj));
    if (obj.has(GROUPS_KEY) && obj.get(GROUPS_KEY).isJsonArray()) {
      List<String> groups = Lists.newArrayList();
      JsonArray groupsJson = obj.getAsJsonArray(GROUPS_KEY);
      for (JsonElement groupJson : groupsJson) {
        if (groupJson != null && groupJson.isJsonPrimitive()) {
          groups.add(groupJson.getAsString());
        }
      }
      result.setGroups(groups);
    }
    result.setOrganizationName(
        obj.has(ORGANIZATION_NAME_KEY) && obj.get(ORGANIZATION_NAME_KEY).isJsonPrimitive()
            ? obj.get(ORGANIZATION_NAME_KEY).getAsString() : null);
    return result;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().appendSuper(super.hashCode()).append(this.getGroups())
        .append(this.getOrganizationName()).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof IndigoUserInfo)) {
      return false;
    }
    IndigoUserInfo other = (IndigoUserInfo) obj;
    return new EqualsBuilder().appendSuper(super.equals(other))
        .append(this.getGroups(), other.getGroups())
        .append(this.getOrganizationName(), other.getOrganizationName()).build();
  }
}
