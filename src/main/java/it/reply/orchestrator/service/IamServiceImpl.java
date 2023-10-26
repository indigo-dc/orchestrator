package it.reply.orchestrator.service;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.iam.IamClientRequest;
import it.reply.orchestrator.dto.iam.WellKnownResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IamServiceImpl implements IamService {

  private ObjectMapper objectMapper;
  public static final String IAM_TOSCA_NODE_TYPE = "tosca.nodes.indigo.iam.client";
  private static final String WELL_KNOWN_ENDPOINT = ".well-known/openid-configuration";
  private static final List<String> REDIRECT_URIS = Lists.newArrayList("https://another.client.example/oidc");
  private static final String TOKEN_ENDPOINT_AUTH_METHOD = "client_secret_basic";
  private static final List<String> GRANT_TYPES = Lists.newArrayList("refresh_token", "authorization_code");
  private static final List<String> RESPONSE_TYPES = Lists.newArrayList("code");
  private static final String ORCHESTRATOR_SCOPES = "openid profile email offline_access iam:admin.write iam:admin.read";
   
  public IamServiceImpl (){
    objectMapper = new ObjectMapper();
  }

  public String getOrchestratorScopes() {
    return ORCHESTRATOR_SCOPES;
}

  public WellKnownResponse getWellKnown(RestTemplate restTemplate, String issuer){
    ResponseEntity<String> responseEntity;
    WellKnownResponse wellKnownResponse = new WellKnownResponse();
    try{
      responseEntity = restTemplate.getForEntity(issuer + WELL_KNOWN_ENDPOINT, String.class);
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("The %s endpoint cannot be contacted. Status code: %s",
          WELL_KNOWN_ENDPOINT, e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("The %s endpoint cannot be contacted. %s",
          WELL_KNOWN_ENDPOINT, e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The %s endpoint cannot be contacted. Status code: %s",
          WELL_KNOWN_ENDPOINT, responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    JsonNode responseJson = null;
    try {
      responseJson = objectMapper.readTree(responseEntity.getBody());
    } catch (IOException e) {
      String errorMessage = String.format("Error in contacting %s. %s",
          issuer + WELL_KNOWN_ENDPOINT, e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } 

    try {
      wellKnownResponse.setRegistrationEndpoint(responseJson.get("registration_endpoint").asText());
      wellKnownResponse.setTokenEndpoint(responseJson.get("token_endpoint").asText());
      List<String> listOfScopes = new ArrayList<>();
      for (JsonNode scope : responseJson.get("scopes_supported")){
        listOfScopes.add(scope.asText());
      }
      wellKnownResponse.setScopesSupported(listOfScopes);
    } catch (NullPointerException e){
      String errorMessage = String.format("Necessary enpoint/s not found in configuration");
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }
    return wellKnownResponse;
  }

  public String getEndpoint(RestTemplate restTemplate, String url, String endpointName){
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.getForEntity(url + WELL_KNOWN_ENDPOINT, String.class);
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("The %s endpoint cannot be contacted. Status code: %s",
          WELL_KNOWN_ENDPOINT, e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("The %s endpoint cannot be contacted. %s",
          WELL_KNOWN_ENDPOINT, e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The %s endpoint cannot be contacted. Status code: %s",
          WELL_KNOWN_ENDPOINT, responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String responseBody = responseEntity.getBody();
    String urlEndpoint = null;
    try {
      // Extract "endpointName" from Json
      urlEndpoint = objectMapper.readTree(responseBody).get(endpointName).asText();
    } catch (IOException e) {
      String errorMessage = String.format("The endpoint %s cannot be obtained. %s",
          endpointName, e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (NullPointerException e){
      String errorMessage = String.format("%s endpoint not found", endpointName);
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }
    return urlEndpoint;
  }
  

  public String getTokenClientCredentials(RestTemplate restTemplate, String iamClientId,
      String iamClientSecret, String iamClientScopes, String iamTokenEndpoint){
    // Set basic authentication in the "Authorization" header
    HttpHeaders headers = new HttpHeaders();
    String auth = String.format("%s:%s", iamClientId, iamClientSecret);
    byte[] authBytes = auth.getBytes();
    byte[] base64CredsBytes = Base64.getEncoder().encode(authBytes);
    String base64Creds = new String(base64CredsBytes);
    headers.add("Authorization", "Basic " + base64Creds);

    // Create a MultiValueMap object for the body data
    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("grant_type", "client_credentials");
    requestBody.add("scope", iamClientScopes);

    // Create an HttpEntity object that contains the headers and body
    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

    // Do the HTTP POST request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          iamTokenEndpoint,
          HttpMethod.POST,
          requestEntity,
          String.class
    );
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("Impossible to create a token with client credentials as grant type. " +
          "Status code: %s", e.getStatusCode());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("Impossible to create a token with client credentials as grant type. %s",
          e.getMessage());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    // Verify the response
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("Impossible to create a token with client credentials as grant type. " +
          "Status code: %s", responseEntity.getStatusCode());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String responseBody = responseEntity.getBody();
    String access_token = null;
    try {
      // Extract "access_token" from Json
      access_token = objectMapper.readTree(responseBody).get("access_token").asText();
    } catch (IOException e) {
      String errorMessage = String.format("Impossible to create a token with client credentials as grant type. %s",
          e.getMessage());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (NullPointerException e){
      String errorMessage = "Impossible to create a token with client credentials as grant type: " +
          "access_token endpoint not found";
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    LOG.debug("Access token with client credentials as grant type successfully created");
    return access_token;
  }

  public Map<String,String> createClient(RestTemplate restTemplate, String iamRegistration,
      String uuid, String userEmail, String scopes) {
    String clientName = "paas:" + uuid;
    List<String> contacts = Arrays.asList(userEmail);
    String jsonRequestBody = "";

    IamClientRequest iamClientRequest = new IamClientRequest(REDIRECT_URIS, clientName, contacts,
        TOKEN_ENDPOINT_AUTH_METHOD, scopes, GRANT_TYPES, RESPONSE_TYPES);
    try {
        jsonRequestBody = objectMapper.writeValueAsString(iamClientRequest);
    }
    catch(JsonProcessingException e) {
        String errorMessage = String.format("No IAM client created. %s", e.getMessage());
        throw new IamServiceException(errorMessage, e);
    }

    // Create an HttpHeaders object to specify the JSON content type
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Create the HttpEntity object that contains the request body and headers
    HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, headers);

    // Do the POST request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          iamRegistration,
          HttpMethod.POST,
          requestEntity,
          String.class
    );
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("No IAM client created. Status code: %s", e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("No IAM client created. %s", e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    // Verify the response
    if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
      String errorMessage = String.format("No IAM client created. Status code: %s",
          responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String responseBody = responseEntity.getBody();
    String clientId = null;
    String registrationAccessToken = null;
    Map<String, String> clientCreated = new HashMap<>();
    try {
      // Extract "client_id", and "registration_access_token" from Json
      clientId = objectMapper.readTree(responseBody).get("client_id").asText();
      registrationAccessToken = objectMapper.readTree(responseBody)
          .get("registration_access_token").asText();
    } catch (IOException e) {
      String errorMessage = String.format("No IAM client created. %s", e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (NullPointerException e){
      String errorMessage = String.format(
          "No IAM client created: client_id and/or registration_access_token not found");
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    clientCreated.put("client_id", clientId);
    clientCreated.put("registration_access_token", registrationAccessToken);
    LOG.debug("The client with client_id {} and registration_access_token {} has been successfully created",
        clientId, registrationAccessToken);
    return clientCreated;
  }
    
  public boolean deleteClient(String clientId, String iamUrl, String token){
    // Create an HttpHeaders object and add the token as authorization
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);

    // Create the HttpEntity object that contains the header with the token
    HttpEntity<?> requestEntity = new HttpEntity<>(headers);

    // URL of the REST service to contact to perform the DELETE request
    String deleteUrl = iamUrl + "/" + clientId;

    // Create a RestTemplate object
    RestTemplate restTemplate = new RestTemplate();

    // Do the DELETE request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          deleteUrl,
          HttpMethod.DELETE,
          requestEntity,
          String.class
      );
    } catch (HttpClientErrorException e){
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())){
        LOG.warn("The client with client_id {} was not found", clientId);
        return false;
      }
      String errorMessage = String.format("The delete of the client with client_id %s was unsuccessful. " + 
        "Status code: %s", clientId, e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("The delete of the client with client_id %s was unsuccessful. %s",
          e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } 

    // Check the response
    if (!HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The delete of the client with client_id %s was unsuccessful. " + 
        "Status code: %s", clientId, responseEntity.getStatusCode());
      LOG.debug(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    LOG.info("The client with client_id {} has been successfully deleted", clientId);
    return true;
  }

  public void deleteAllClients(RestTemplate restTemplate, Map<Boolean, Set<Resource>> resources){
    for (Resource resource : resources.get(false)) {
      if (resource.getToscaNodeType().equals(IAM_TOSCA_NODE_TYPE)){
        Map<String,String> resourceMetadata = resource.getMetadata();
        if (resourceMetadata != null && resourceMetadata.containsKey("client_id") && 
            resourceMetadata.containsKey("registration_access_token")){
          WellKnownResponse wellKnownResponse = getWellKnown(restTemplate, resourceMetadata.get("issuer"));
          LOG.info("Deleting client with client_id {} and issuer {}",
              resourceMetadata.get("client_id"), resourceMetadata.get("issuer"));
          deleteClient(resourceMetadata.get("client_id"), wellKnownResponse.getRegistrationEndpoint(),
              resourceMetadata.get("registration_access_token"));
        }
        else {
          LOG.info("Found node of type {} but no client is registered in metadata", IAM_TOSCA_NODE_TYPE);
        }
      }
    }
  }

  public void assignOwnership(String clientId, String iamUrl, String owner, String token){
    // Create an HttpHeaders object and add the token as authorization
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);

    // Create the HttpEntity object that contains the header with the token
    HttpEntity<?> requestEntity = new HttpEntity<>(headers);

    // URL of the REST service to contact to assign the ownership of a client
    String assignOwnershipUrl = iamUrl + "iam/api/clients/" + clientId + "/owners/" + owner;

    // Create a RestTemplate object
    RestTemplate restTemplate = new RestTemplate();

    // Do the POST request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          assignOwnershipUrl,
          HttpMethod.POST,
          requestEntity,
          String.class
      );
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("The owner of the client with client_id %s cannot be set to " +
      "the user with Id %s (issuer %s). Status code: %s", clientId, owner, iamUrl, e.getStatusCode());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("The owner of the client with client_id %s cannot be set to " +
      "the user with Id %s (issuer %s). %s", clientId, owner, iamUrl, e.getMessage());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } 

    // Check the response
    if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The owner of the client with client_id %s cannot be set to " +
      "the user with Id %s (issuer %s). Status code: %s", clientId, owner, iamUrl, responseEntity.getStatusCode());
      LOG.warn(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    LOG.info("The owner of the client with client_id {} has been successfully set to " +
    "the user with Id {}", clientId, owner);
  }

  public boolean checkIam(RestTemplate restTemplate, String idpUrl) {
    // URL of the endpoint to be contacted
    String endpointURL = idpUrl + "actuator/info";

    // Create HTTP headers to accept JSON
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Create an HTTP request with headers
    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

    // Do the HTTP request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          endpointURL,
          HttpMethod.GET,
          requestEntity,
          String.class
      );
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("Cannot say if %s is an url related to an IAM or not. Status code: %s",
          endpointURL, e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("Cannot say if %s is an url related to an IAM or not. %s",
          endpointURL, e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    // Check the response
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("Cannot say if %s is an url related to an IAM or not. Status code: %s",
          endpointURL, responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    // Get the response body as a JSON string
    String responseBody = responseEntity.getBody();

    // Analyze the JSON response
    try {
        // Access the build:name field
        String buildName = objectMapper.readTree(responseBody)
            .get("build").get("name").asText();

        // Check if the value contains "iam" (ignoring case)
        if (buildName.toLowerCase().contains("iam")) {
            return true;
        } else {
            return false;
        }
    } catch (IOException e) {
      String errorMessage = String.format("Cannot say if %s is an url related to an IAM or not. %s",
          endpointURL, e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (NullPointerException e) {
      String errorMessage = String.format("Cannot say if %s is an url related to an IAM or not." +
          "The build:name field does not exist", endpointURL);
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }
  }

  public String getInfoIamClient(String clientId, String iamUrl, String token){
    // Create an HttpHeaders object and add the token as authorization
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);

    // Create the HttpEntity object that contains the header with the token
    HttpEntity<?> requestEntity = new HttpEntity<>(headers);

    // URL of the REST service to contact to perform the request
    String getUrl = iamUrl + clientId;

    // Create a RestTemplate object
    RestTemplate restTemplate = new RestTemplate();

    // Do the GET request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          getUrl,
          HttpMethod.GET,
          requestEntity,
          String.class
      );
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("Obtaining of information about the client with client_id %s was unsuccessful. " + 
        "Status code: %s", clientId, e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("Obtaining of information about the client with client_id %s " +
          "was unsuccessful. %s", e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } 
    System.out.println("Corpo della risposta:\n" + responseEntity.getBody());
    // Check the response
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("Obtaining of information about the client with client_id %s was unsuccessful. " + 
        "Status code: %s", clientId, responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    LOG.info("Information about the client with client_id {} has been successfully obtained", clientId);
    return responseEntity.getBody();
  }
  
  public String updateClient(String clientId, String iamUrl, String token, String jsonUpdated){
    // Create an HttpHeaders object and add the token as authorization
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);

    // Create the HttpEntity object that contains the header with the token
    HttpEntity<?> requestEntity = new HttpEntity<>(jsonUpdated, headers);

    // URL of the REST service to contact to perform the update request
    String updateUrl = iamUrl + clientId;

    // Create a RestTemplate object
    RestTemplate restTemplate = new RestTemplate();

    // Do the GET request
    ResponseEntity<String> responseEntity;
    try{
      responseEntity = restTemplate.exchange(
          updateUrl,
          HttpMethod.PUT,
          requestEntity,
          String.class
      );
    } catch (HttpClientErrorException e){
      String errorMessage = String.format("The update of the client with client_id %s was unsuccessful. " + 
        "Status code: %s", clientId, e.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } catch (RestClientException e){
      String errorMessage = String.format("The update of the client with client_id %s was unsuccessful. %s",
          e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    } 
    System.out.println("Corpo della risposta:\n" + responseEntity.getBody());
    // Check the response
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The update of the client with client_id %s was unsuccessful. " + 
        "Status code: %s", clientId, responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    LOG.info("The info of the client with client_id {} has been successfully updated", clientId);
    return responseEntity.getBody();
  }


}