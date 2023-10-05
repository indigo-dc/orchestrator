package it.reply.orchestrator.service;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import it.reply.orchestrator.IamClientRequest;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IamServiceImpl implements IamService {

  private ObjectMapper objectMapper;
  private static final String WELL_KNOWN_ENDPOINT = ".well-known/openid-configuration";
  private static final List<String> REDIRECT_URIS = Lists.newArrayList("https://another.client.example/oidc");
  private static final String TOKEN_ENDPOINT_AUTH_METHOD = "client_secret_basic";
  private static final String SCOPE = "openid email profile offline_access";
  private static final List<String> GRANT_TYPES = Lists.newArrayList("refresh_token", "authorization_code");
  private static final List<String> RESPONSE_TYPES = Lists.newArrayList("code");
  private static final String ORCHESTRATOR_SCOPES = "openid profile email offline_access iam:admin.write iam:admin.read";
   
  public IamServiceImpl (){
    objectMapper = new ObjectMapper();
  }

  public String getOrchestratorScopes() {
    return ORCHESTRATOR_SCOPES;
}

  public String getEndpoint(RestTemplate restTemplate, String url, String endpointName){
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(url + WELL_KNOWN_ENDPOINT, String.class);
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The request was unsuccessful. Status code: %s", responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }
    String responseBody = responseEntity.getBody();
    LOG.debug("Body of the request: {}", responseBody);
    JsonNode jsonNode = null;
    try {
      // Extract "endpointName" from Json
      jsonNode = objectMapper.readTree(responseBody).get(endpointName);
    } catch (IOException e) {
      LOG.error(e.getMessage());
      throw new IamServiceException(e.getMessage(), e);
    } catch (NullPointerException e){
      String errorMessage = String.format("%s endpoint not found", endpointName);
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String urlEndpoint = jsonNode.asText();
    return urlEndpoint;
  }
  

  public String getTokenClientCredentials(RestTemplate restTemplate, String iamClientId, String iamClientSecret, String iamClientScopes, String iamTokenEndpoint){

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
    ResponseEntity<String> responseEntity = restTemplate.exchange(
        iamTokenEndpoint,
        HttpMethod.POST,
        requestEntity,
        String.class
    );

    // Verify the response
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("Impossible to create a token with client credentials as grant type." +
          "The request was unsuccessful. Status code: %s", responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String responseBody = responseEntity.getBody();
    LOG.debug("Body of the request: {}", responseBody);
    JsonNode jsonNode = null;
    try {
      // Extract "access_token" from Json
      jsonNode = objectMapper.readTree(responseBody).get("access_token");
    } catch (IOException e) {
      LOG.error("Impossible to create a token with client credentials as grant type: " + e.getMessage());
      throw new IamServiceException(e.getMessage(), e);
    } catch (NullPointerException e){
      String errorMessage ="access_token endpoint not found";
      LOG.error("Impossible to create a token with client credentials as grant type: " + errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String access_token = jsonNode.asText();
    LOG.debug("access token with client credentials as grant type successfully created");
    return access_token;
  }

  public String createClient(RestTemplate restTemplate, String iamRegistration, String uuid, String userEmail) {
    /*String jsonRequestBody = "{\n" +
    "  \"redirect_uris\": [\n" +
    "    \"https://another.client.example/oidc\"\n" +
    "  ],\n" +
    "  \"client_name\": \"paas:" + uuid + "\",\n" +
    "  \"contacts\": [\n" +
    "    \"" + userEmail + "\"\n" + 
    "  ],\n" +
    "  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
    "  \"scope\": \"openid email profile offline_access\",\n" +
    "  \"grant_types\": [\n" +
    "    \"refresh_token\",\n" +
    "    \"authorization_code\"\n" +
    "  ],\n" +
    "  \"response_types\": [\n" +
    "    \"code\"\n" +
    "  ]\n" +
    "}";*/

    String clientName = "paas:" + uuid;
    List<String> contacts = Arrays.asList(userEmail);
    String jsonRequestBody = "";

    IamClientRequest iamClientRequest = new IamClientRequest(REDIRECT_URIS, clientName, contacts, TOKEN_ENDPOINT_AUTH_METHOD, SCOPE, GRANT_TYPES, RESPONSE_TYPES);
    try {
        jsonRequestBody = objectMapper.writeValueAsString(iamClientRequest);
        LOG.debug("{}", jsonRequestBody);
    }
    catch(JsonProcessingException e) {
        throw new IamServiceException(e.getMessage(), e);
    }

    // Create an HttpHeaders object to specify the JSON content type
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Create the HttpEntity object that contains the request body and headers
    HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, headers);

    // Do the POST request
    ResponseEntity<String> responseEntity = restTemplate.exchange(
        iamRegistration,
        HttpMethod.POST,
        requestEntity,
        String.class
    );

    // Verify the response
    if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
      String errorMessage = String.format("The request was unsuccessful. Status code: %s", responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String responseBody = responseEntity.getBody();
    LOG.debug("Body of the request: {}", responseBody);
    JsonNode jsonNode = null;
    try {
      // Extract "client_id" from Json
      jsonNode = objectMapper.readTree(responseBody).get("client_id");
    } catch (IOException e) {
      LOG.error("No IAM client created: " + e.getMessage());
      throw new IamServiceException(e.getMessage(), e);
    } catch (NullPointerException e){
      String errorMessage = "client_id not found";
      LOG.error(errorMessage);
      LOG.error("No IAM client created: " + errorMessage);
      throw new IamServiceException(errorMessage);
    }

    String clientId = jsonNode.asText();
    LOG.debug("The client with client_id {} has been successfully created", clientId);
    return clientId;
  }
    
  public boolean deleteClient(String clientId, String iamUrl, String token){
    // Create an HttpHeaders object and add the token as authorization
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);

    // Create the HttpEntity object that contains the header with the token
    HttpEntity<?> requestEntity = new HttpEntity<>(headers);

    // URL of the REST service to contact to perform the DELETE request
    String deleteUrl = iamUrl + "iam/api/clients/" + clientId;

    // Create a RestTemplate object
    RestTemplate restTemplate = new RestTemplate();

    // Do the DELETE request
    ResponseEntity<String> responseEntity = restTemplate.exchange(
        deleteUrl,
        HttpMethod.DELETE,
        requestEntity,
        String.class
    );

    // Check the response
    if (!HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The delete was unsuccessful. Status code: %s", responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    LOG.info("The client with client_id {} has been successfully deleted", clientId);
    return true;
  }

   public static void main(String args[]){
    IamService iamService = new IamServiceImpl();
    RestTemplate restTemplate = new RestTemplate();
    iamService.checkIam(restTemplate, "https://iotwins-iam.cloud.cnaf.infn.it/");
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
    ResponseEntity<String> responseEntity = restTemplate.exchange(
        endpointURL,
        HttpMethod.GET,
        requestEntity,
        String.class
    );

    // Check the response
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      String errorMessage = String.format("The request to %s was unsuccessful. Status code: %s", endpointURL, responseEntity.getStatusCode());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage);
    }

    // Get the response body as a JSON string
    String responseBody = responseEntity.getBody();

    // Analyze the JSON response
    try {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // Access the build:name field
        String buildName = jsonNode
                .path("build")
                .path("name")
                .asText();

        // Check if build:name value contains "iam" (ignoring case)
        if (buildName.toLowerCase().contains("iam")) {
          LOG.debug("{} is an IAM", idpUrl);
            return true;
        } else {
            LOG.debug("{} is not an IAM", idpUrl);
            return false;
        }
    } catch (IOException e) {
      LOG.error(e.getMessage());
      throw new IamServiceException(e.getMessage(), e);
    }
  }
}