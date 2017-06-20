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

package it.reply.orchestrator.config.filters;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.utils.json.JsonUtility;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.MDC;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class CustomRequestLoggingFilter extends OncePerRequestFilter {

  public static final String X_REQUEST_ID = "X-Request-ID";
  public static final String REQUEST_ID_MDC_KEY = "request_id";

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
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
  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
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

  }

  @Data
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

    public abstract String getType();

    /**
     * Set the uri from a base uri and a query string.
     * 
     * @param uri
     *          the base uri without query param
     * @param queryString
     *          the string containing the query param
     */
    public void setUriFromRequest(String uri, String queryString) {
      StringBuilder sb = new StringBuilder(uri);
      safeTrimmedString(queryString)
          .ifPresent(parsedQueryString -> sb.append("?").append(parsedQueryString));
      setUri(sb.toString());
    }

    /**
     * Set the client ip from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setClientIpFromRequest(HttpServletRequest request) {
      safeTrimmedString(getRemoteAddr(request)).ifPresent(this::setClientIp);
    }

    /**
     * Set the session from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setSessionFromRequest(HttpServletRequest request) {
      Optional.ofNullable(request.getSession(false))
          .map(HttpSession::getId)
          .ifPresent(this::setSession);
    }

    /**
     * Set the user from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setUserFromRequest(HttpServletRequest request) {
      safeTrimmedString(request.getRemoteUser()).ifPresent(this::setUser);
    }

    /**
     * Set the headers from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     */
    public void setHeadersFromRequest(HttpServletRequest request) {
      setHeaders(new ServletServerHttpRequest(request).getHeaders().toSingleValueMap());
      // for (String headerKey : getHeadersToOmitt()) {
      // if (headers.containsKey(headerKey)) {
      // headers.put(headerKey, "<omitted>");
      // }
      // }
    }

    /**
     * Set the payload from a {@link HttpServletRequest}.
     * 
     * @param request
     *          the request
     * @param maxPayloadLength
     *          the max payload length to parse
     */
    public void setPayloadFromRequest(HttpServletRequest request, int maxPayloadLength) {
      if (isIncludePayload(maxPayloadLength)) {
        Optional<ContentCachingRequestWrapper> optionalWrapper = Optional
            .ofNullable(WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class));
        optionalWrapper.ifPresent(wrapper -> {
          byte[] buf = wrapper.getContentAsByteArray();
          if (buf.length > 0) {
            int length = Math.min(buf.length, maxPayloadLength);
            String extractedPayload;
            try {
              extractedPayload = new String(buf, 0, length, wrapper.getCharacterEncoding());
            } catch (UnsupportedEncodingException ex) {
              extractedPayload = "[unknown]";
            }
            setPayload(extractedPayload);
          }
        });

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

      setUriFromRequest(request.getRequestURI(), request.getQueryString());
      setClientIpFromRequest(request);
      setSessionFromRequest(request);
      setUserFromRequest(request);
      setHeadersFromRequest(request);
      setPayloadFromRequest(request, maxPayloadLength);
    }
  }

  @Getter
  @Setter
  private int maxPayloadLength = -1;

  @Getter
  @Setter
  @NonNull
  private List<String> headersToOmitt = new ArrayList<>();

  public boolean isIncludePayload() {
    return isIncludePayload(getMaxPayloadLength());
  }

  public static boolean isIncludePayload(int maxPayloadLength) {
    return maxPayloadLength > 0;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    Instant startingInstant = Instant.now();
    boolean isFirstRequest = !isAsyncDispatch(request);
    HttpServletRequest requestToUse = request;

    String requestId = "req-" + UUID.randomUUID().toString();

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
          afterRequest(requestToUse, response, getElapsedMillisec(startingInstant));
        }
      }
    } finally {
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }

  public static Optional<String> safeTrimmedString(@Nullable String input) {
    return Optional.ofNullable(input).map(String::trim).map(Strings::emptyToNull);
  }

  public boolean shouldLog(HttpServletRequest request) {
    return LOG.isDebugEnabled();
  }

  /**
   * Writes a log message before the request is processed.
   */
  protected void beforeRequest(HttpServletRequest request) {
    try {
      LOG.debug(JsonUtility.serializeJson(new RequestWrapper(request, getMaxPayloadLength())));
    } catch (IOException ex) {
      // shouldn't happen
      LOG.error("Error logging request {}", request, ex);
    }
  }

  /**
   * Writes a log message after the request is processed.
   */
  protected void afterRequest(HttpServletRequest request, HttpServletResponse response,
      double responseTime) {
    try {
      LOG.debug(JsonUtility.serializeJson(
          new ResponseWrapper(request, response, getMaxPayloadLength(), responseTime)));
    } catch (IOException ex) {
      // shouldn't happen
      LOG.error("Error logging response {} for request ", response, request, ex);
    }
  }

  private double getElapsedMillisec(Instant startingInstant) {
    return Duration.between(startingInstant, Instant.now()).toMillis();
  }

  private static String getRemoteAddr(HttpServletRequest request) {
    return safeTrimmedString(request.getHeader(HttpHeaders.X_FORWARDED_FOR))
        .map(header -> header.split(", ?"))
        .map(Stream::of)
        .orElseGet(Stream::empty)
        .map(CustomRequestLoggingFilter::safeTrimmedString)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElseGet(request::getRemoteAddr);
  }

}
