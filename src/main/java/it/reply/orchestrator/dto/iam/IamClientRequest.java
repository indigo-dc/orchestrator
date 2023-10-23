package it.reply.orchestrator.dto.iam;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IamClientRequest {
    private List<String> redirectUris;
    private String clientName;
    private List<String> contacts;
    private String tokenEndpointAuthMethod;
    private String scope;
    private List<String> grantTypes;
    private List<String> responseTypes;


    public IamClientRequest(List<String> redirectUris, String clientName, List<String> contacts,
            String tokenEndpointAuthMethod, String scope, List<String> grantTypes, List<String> responseTypes) {
        this.redirectUris = redirectUris;
        this.clientName = clientName;
        this.contacts = contacts;
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
        this.scope = scope;
        this.grantTypes = grantTypes;
        this.responseTypes = responseTypes;
    }
    
    // Getter and setter for redirectUris

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    // Getter and setter for clientName

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    // Getter and setter for contacts

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    // Getter and setter for tokenEndpointAuthMethod

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    // Getter and setter for scope

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    // Getter and setter for grantTypes

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    // Getter and setter for responseTypes

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }
}
