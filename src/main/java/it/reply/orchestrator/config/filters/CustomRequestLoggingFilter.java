package it.reply.orchestrator.config.filters;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.utils.json.JsonUtility;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.MDC;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CustomRequestLoggingFilter extends OncePerRequestFilter {

  public static final String X_REQUEST_ID = "X-Request-ID";
  public static final String REQUEST_ID_MDC_KEY = "request_id";

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class RequestWrapper extends AbstractWrapper {

    private String type = "request";

    public RequestWrapper(HttpServletRequest request, int maxPayloadLength) {
      super(request, maxPayloadLength);
    }

    @Override
    public String getType() {
      return type;
    }

  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ResponseWrapper extends AbstractWrapper {

    private String type = "response";

    @JsonProperty("response_status")
    private int responseStatus;
    @JsonProperty("response_time")
    private double responseTime;

    /**
     * Generate a ResponseWrapper.
     * 
     * @param request
     *          the request
     * @param response
     *          the response
     * @param maxPayloadLength
     *          the max payload request to parse
     * @param responseTime
     *          the response time
     */
    public ResponseWrapper(HttpServletRequest request, HttpServletResponse response,
        int maxPayloadLength, double responseTime) {
      super(request, maxPayloadLength);
      this.responseStatus = response.getStatus();
      this.responseTime = responseTime;
    }

    @Override
    public String getType() {
      return type;
    }

    public int getResponseStatus() {
      return responseStatus;
    }

    public double getResponseTime() {
      return responseTime;
    }

    public void setResponseStatus(int responseStatus) {
      this.responseStatus = responseStatus;
    }

    public void setResponseTime(double responseTime) {
      this.responseTime = responseTime;
    }

  }

  public abstract static class AbstractWrapper {
    private Map<String, String> headers;
    @JsonProperty("http_method")
    private String httpMethod;
    private String uri;
    @JsonProperty("client_ip")
    private String clientIp;
    private String session;
    private String user;
    private String payload;

    public Map<String, String> getHeaders() {
      return headers;
    }

    public String getHttpMethod() {
      return httpMethod;
    }

    public String getUri() {
      return uri;
    }

    public String getClientIp() {
      return clientIp;
    }

    public String getSession() {
      return session;
    }

    public String getUser() {
      return user;
    }

    public String getPayload() {
      return payload;
    }

    public abstract String getType();

    public void setHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    /**
     * Set the uri from a base uri and a query string.
     * 
     * @param uri
     *          the base uri without query param
     * @param queryString
     *          the string containing the query param
     */
    public void setUri(String uri, String queryString) {
      String parsedQueryString = safeTrimmedString(queryString);
      if (parsedQueryString.isEmpty()) {
        this.uri = uri;
      } else {
        this.uri = String.format("%s?%s", uri, parsedQueryString);
      }
    }

    public void setClientIp(String clientIp) {
      this.clientIp = clientIp;
    }

    /**
     * Set the client ip from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setClientIp(HttpServletRequest request) {
      String clientIp = safeTrimmedString(getRemoteAddr(request));
      if (!clientIp.isEmpty()) {
        setClientIp(clientIp);
      }
    }

    public void setSession(String session) {
      this.session = session;
    }

    /**
     * Set the session from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setSession(HttpServletRequest request) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        setSession(session.getId());
      }
    }

    public void setUser(String user) {
      this.user = user;
    }

    /**
     * Set the user from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setUser(HttpServletRequest request) {
      String user = safeTrimmedString(request.getRemoteUser());
      if (!user.isEmpty()) {
        setUser(user);
      }
    }

    public void setHeaders(Map<String, String> headers) {
      this.headers = headers;
    }

    /**
     * Set the headers from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setHeaders(HttpServletRequest request) {
      Map<String, String> headers =
          new ServletServerHttpRequest(request).getHeaders().toSingleValueMap();
      setHeaders(headers);
      // for (String headerKey : getHeadersToOmitt()) {
      // if (headers.containsKey(headerKey)) {
      // headers.put(headerKey, "<omitted>");
      // }
      // }
    }

    public void setPayload(String payload) {
      this.payload = payload;
    }

    /**
     * Set the payload from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     * @param maxPayloadLength
     *          the max payload length to parse
     */
    public void setPayload(HttpServletRequest request, int maxPayloadLength) {
      if (isIncludePayload(maxPayloadLength)) {
        ContentCachingRequestWrapper wrapper =
            WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
          byte[] buf = wrapper.getContentAsByteArray();
          if (buf.length > 0) {
            int length = Math.min(buf.length, maxPayloadLength);
            String payload;
            try {
              payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
            } catch (UnsupportedEncodingException ex) {
              payload = "[unknown]";
            }
            setPayload(payload);
          }
        }
      }
    }

    /**
     * Generate an AbstractWrapper.
     * 
     * @param request
     *          the request
     * @param maxPayloadLength
     *          the max payload request to parse
     */
    public AbstractWrapper(HttpServletRequest request, int maxPayloadLength) {
      setHttpMethod(request.getMethod());

      setUri(request.getRequestURI(), request.getQueryString());
      setClientIp(request);
      setSession(request);
      setUser(request);
      setHeaders(request);
      setPayload(request, maxPayloadLength);
    }
  }

  private int maxPayloadLength = -1;

  public int getMaxPayloadLength() {
    return maxPayloadLength;
  }

  public void setMaxPayloadLength(int maxPayloadLength) {
    this.maxPayloadLength = maxPayloadLength;
  }

  private List<String> headersToOmitt = Lists.newArrayList();

  public List<String> getHeadersToOmitt() {
    return headersToOmitt;
  }

  public boolean isIncludePayload() {
    return isIncludePayload(getMaxPayloadLength());
  }

  public static boolean isIncludePayload(int maxPayloadLength) {
    return maxPayloadLength > 0;
  }

  public void setHeadersToOmitt(List<String> headersToOmitt) {
    Objects.requireNonNull(headersToOmitt, "headersToOmitt list must not be null");
    this.headersToOmitt = headersToOmitt;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    long startTime = System.nanoTime();
    boolean isFirstRequest = !isAsyncDispatch(request);
    HttpServletRequest requestToUse = request;

    String requestId = "";// safeTrimmedString(request.getHeader(X_REQUEST_ID));

    if (requestId.isEmpty()) {
      requestId = "req-" + UUID.randomUUID().toString();
    }
    try {
      MDC.put(REQUEST_ID_MDC_KEY, requestId);
      response.addHeader(X_REQUEST_ID, requestId);

      if (isIncludePayload() && isFirstRequest
          && !(request instanceof ContentCachingRequestWrapper)) {
        requestToUse = new ContentCachingRequestWrapper(request);
      }

      boolean shouldLog = shouldLog(requestToUse);
      if (shouldLog && isFirstRequest) {
        beforeRequest(requestToUse);
      }
      try {
        filterChain.doFilter(requestToUse, response);
      } finally {
        if (shouldLog && !isAsyncStarted(requestToUse)) {
          afterRequest(requestToUse, response, getElapsedMillisec(startTime));
        }
      }
    } finally {
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }

  public static String safeTrimmedString(String input) {
    return Strings.nullToEmpty(input).trim();
  }

  protected boolean shouldLog(HttpServletRequest request) {
    return logger.isDebugEnabled();
  }

  /**
   * Writes a log message before the request is processed.
   */
  protected void beforeRequest(HttpServletRequest request) {
    try {
      logger.debug(JsonUtility.serializeJson(new RequestWrapper(request, getMaxPayloadLength())));
    } catch (IOException ex) {
      logger.error("Error loggin request", ex);
    }
  }

  /**
   * Writes a log message after the request is processed.
   */
  protected void afterRequest(HttpServletRequest request, HttpServletResponse response,
      double responseTime) {
    try {
      logger.debug(JsonUtility.serializeJson(
          new ResponseWrapper(request, response, getMaxPayloadLength(), responseTime)));
    } catch (IOException ex) {
      logger.error("Error loggin response", ex);
    }
  }

  private double getElapsedMillisec(double startTimeNanoSec) {
    return (System.nanoTime() - startTimeNanoSec) / 1e6;
  }

  private static String getRemoteAddr(HttpServletRequest request) {
    String remoteAddress = null;
    String[] forwardedAddress =
        Strings.nullToEmpty(request.getHeader(HttpHeaders.X_FORWARDED_FOR)).split(", ?");
    if (ArrayUtils.isEmpty(forwardedAddress)
        || Strings.nullToEmpty(forwardedAddress[0]).trim().isEmpty()) {
      remoteAddress = request.getRemoteAddr();
    } else {
      remoteAddress = forwardedAddress[0];
    }
    return remoteAddress;
  }

}
