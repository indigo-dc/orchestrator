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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.mitre.oauth2.model.RegisteredClient;

import java.net.URL;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IamService {

  private ObjectMapper objectMapper;
  private static final String WELL_KNOWN_ENDPOINT = ".well-known/openid-configuration";

  public IamService (){
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
    String jsonRequestBody = "{\n" +
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
    "}";

    //String jsonOutput = objectMapper.writeValueAsString(jsonRequestBody);

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
    return clientId;
  }
    
  public boolean deleteClient(String clientId, String iamUrl, String token){
    // Crea un oggetto HttpHeaders e aggiungi il token come autorizzazione
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);

    // Crea l'oggetto HttpEntity che contiene l'intestazione con il token
    HttpEntity<?> requestEntity = new HttpEntity<>(headers);

    // URL del servizio REST su cui eseguire la richiesta DELETE
    String deleteUrl = iamUrl + "iam/api/clients/" + clientId; // Sostituisci con l'URL corretto

    // Crea un oggetto RestTemplate
    RestTemplate restTemplate = new RestTemplate();

    // Effettua la richiesta DELETE
    ResponseEntity<String> responseEntity = restTemplate.exchange(
        deleteUrl,
        HttpMethod.DELETE,
        requestEntity,
        String.class
    );

    // Verifica la risposta
    return HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode());
    /*if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
        System.out.println("La richiesta DELETE ha avuto successo.");
        return true;
    } else {
        System.err.println("La richiesta DELETE non ha avuto successo. Status code: " + responseEntity.getStatusCode());
        return false;
    }*/
  }

  public boolean checkIam(String idpUrl) {
    try {

      // Effettua la richiesta HTTP GET all'endpoint
      HttpURLConnection connection = (HttpURLConnection) new URL(idpUrl + "actuator/info").openConnection();
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
    test("https://iotwins-iam.cloud.cnaf.infn.it/");
  }

  public static boolean test(String idpUrl) {
    // Crea un oggetto RestTemplate
    RestTemplate restTemplate = new RestTemplate();

    // URL dell'endpoint da contattare
    String endpointURL = idpUrl + "actuator/info";

    // Crea le intestazioni HTTP con un'intestazione "Accept" per accettare JSON
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Crea una richiesta HTTP con le intestazioni
    HttpEntity<String> requestEntity = new HttpEntity<>(headers);

    // Esegui la richiesta HTTP e ottieni la risposta come ResponseEntity<String>
    ResponseEntity<String> responseEntity = restTemplate.exchange(
        endpointURL,
        HttpMethod.GET,
        requestEntity,
        String.class
    );

    // Ottieni il corpo della risposta come stringa JSON
    String responseBody = responseEntity.getBody();

    // Utilizza Jackson per analizzare la risposta JSON
    try {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // Accedi ai campi JSON come desiderato, ad esempio:
        String buildName = jsonNode
                .path("build")
                .path("name")
                .asText();

        // Verifica se il valore build:name contiene "iam" (ignorando maiuscole e minuscole)
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

}