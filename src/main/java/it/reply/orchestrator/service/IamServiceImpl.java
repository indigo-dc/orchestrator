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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.mitre.oauth2.model.RegisteredClient;

import java.net.URL;
import lombok.extern.slf4j.Slf4j;

import it.reply.orchestrator.IamClientRequest;

@Slf4j
public class IamServiceImpl implements IamService {

  private ObjectMapper objectMapper;
  private static final String WELL_KNOWN_ENDPOINT = ".well-known/openid-configuration";
  private static final List<String> REDIRECT_URIS = Lists.newArrayList("https://another.client.example/oidc");
  private static final String TOKEN_ENDPOINT_AUTH_METHOD = "client_secret_basic";
  private static final String SCOPE = "openid email profile offline_access";
  private static final List<String> GRANT_TYPES = Lists.newArrayList("refresh_token", "authorization_code");
  private static final List<String> RESPONSE_TYPES = Lists.newArrayList("code");
   
  public IamServiceImpl (){
    objectMapper = new ObjectMapper();
  }

  public String getEndpoint(RestTemplate restTemplate, String url, String endpointName){
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(url + WELL_KNOWN_ENDPOINT, String.class);
    if (!HttpStatus.OK.equals(responseEntity.getStatusCode())){
      LOG.error("The request was unsuccessful. Status code: {}", responseEntity.getStatusCode());
      return "";
    }
    String responseBody = responseEntity.getBody();
    LOG.debug("Body of the request: {}", responseBody);
    JsonNode jsonNode = null;
    try {
      // Extract "endpointName" from Json
      jsonNode = objectMapper.readTree(responseBody).get(endpointName);
    } catch (IOException e) {
      LOG.error(e.getMessage());
      return "";
    } catch (NullPointerException e){
      LOG.error("{} not found", endpointName);
      return "";
    }

    String urlEndpoint = jsonNode.asText();

    // Stampa il valore di registration_endpoint
    LOG.debug("endpoint: {}", urlEndpoint);
    return urlEndpoint;
  }
  

  public String getToken(RestTemplate restTemplate, String iamClientId, String iamClientSecret, String iamClientScopes, String iamTokenEndpoint){

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
      LOG.error("The request was unsuccessful. Status code: {}", responseEntity.getStatusCode());
      return "";
    }

    String responseBody = responseEntity.getBody();
    LOG.debug("Body of the request: {}", responseBody);
    JsonNode jsonNode = null;
    try {
      // Extract "access_token" from Json
      jsonNode = objectMapper.readTree(responseBody).get("access_token");
    } catch (IOException e) {
      LOG.error(e.getMessage());
      return "";
    } catch (NullPointerException e){
      LOG.error("access_token not found");
      return "";
    }

    String access_token = jsonNode.asText();
    return access_token;
  }

  public String createClient(RestTemplate restTemplate, String iamRegistration, String uuid, String userEmail){
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
        e.getMessage();
        return "";
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
    if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())){
      LOG.error("The request was unsuccessful. Status code: {}", responseEntity.getStatusCode());
      return "";
    }

    String responseBody = responseEntity.getBody();
    LOG.debug("Body of the request: {}", responseBody);
    JsonNode jsonNode = null;
    try {
      // Extract "client_id" from Json
      jsonNode = objectMapper.readTree(responseBody).get("client_id");
    } catch (IOException e) {
      LOG.error(e.getMessage());
      return "";
    } catch (NullPointerException e){
      LOG.error("client_id not found");
      return "";
    }

    String clientId = jsonNode.asText();
    LOG.debug("The client {} has been successfully created", clientId);
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
      LOG.error("The delete was unsuccessful. Status code: {}", responseEntity.getStatusCode());
      return false;
    }

    LOG.debug("The client {} has been successfully deleted", clientId);
    return true;
    /*if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
        System.out.println("La richiesta DELETE ha avuto successo.");
        return true;
    } else {
        System.err.println("La richiesta DELETE non ha avuto successo. Status code: " + responseEntity.getStatusCode());
        return false;
    }*/
  }

  public boolean checkIam_old(String idpUrl) {
    try {

      // Effettua la richiesta HTTP GET all'endpoint
      HttpURLConnection connection = (HttpURLConnection) new URL(idpUrl + "actuator/info").openConnection();
      //fai un blocco try catch per il tipo di eccezione IOException
      connection.setRequestMethod("GET");

      // Legge la risposta dal server
      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
          response.append(line);
      }
      reader.close();

        // Analizza il JSON con Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.toString());

        // Estrai il valore di build:name dal JSON
        String buildName = jsonNode
                .path("build")
                .path("name")
                .asText();

        // Verifica se il valore di build:name contiene "iam" (ignorando maiuscole e minuscole)
        if (buildName.toLowerCase().contains("iam")) {
            System.out.println("Il valore build:name contiene 'iam'.");
            return true;
        } else {
            System.out.println("Il valore build:name non contiene 'iam'.");
            return false;
        }
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
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
      LOG.error("The request to {} was unsuccessful. Status code: {}", endpointURL, responseEntity.getStatusCode());
      return false;
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
      return false;
    }
  }
}