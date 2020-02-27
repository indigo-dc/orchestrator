/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import it.reply.orchestrator.utils.JsonUtils;
import it.reply.orchestrator.utils.MdcUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

@Slf4j
public class CustomRequestLoggingFilter extends OncePerRequestFilter {

  public static final String X_REQUEST_ID = "X-Request-ID";

  public static final String ATTRIBUTE_REQUEST_ID = CustomRequestLoggingFilter.class.getName()
      + ".RequestId";

  private static final String ATTRIBUTE_STOP_WATCH = CustomRequestLoggingFilter.class.getName()
      + ".StopWatch";

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class RequestWrapper extends AbstractWrapper {

    private String type = "request";

    public RequestWrapper(HttpServletRequest request, int maxPayloadLength,
        Set<String> headersToOmit) {
      super(request, maxPayloadLength, headersToOmit);
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
     * @param headersToOmit
     *          the set of headers to omit
     * @param responseTime
     *          the response time
     */
    public ResponseWrapper(HttpServletRequest request, HttpServletResponse response,
        int maxPayloadLength, Set<String> headersToOmit, double responseTime) {
      super(request, maxPayloadLength, headersToOmit);
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
      safeTrimmedString(request.getRemoteAddr()).ifPresent(this::setClientIp);
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
     * @param headersToOmit
     *          the set of headers to omit
     */
    public void setHeadersFromRequest(HttpServletRequest request,
        @NonNull Set<String> headersToOmit) {
      HttpHeaders httpHeaders = new ServletServerHttpRequest(request)
          .getHeaders();
      if (!headersToOmit.isEmpty()) {
        httpHeaders.replaceAll((key, value) -> {
          return headersToOmit.contains(key) ? Lists.newArrayList("<omitted>") : value;
        });
      }
      setHeaders(httpHeaders.toSingleValueMap());
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
     * @param headersToOmit
     *          the Set of headers to omit
     */
    public AbstractWrapper(HttpServletRequest request, int maxPayloadLength,
        Set<String> headersToOmit) {
      setHttpMethod(request.getMethod());

      setUriFromRequest(request.getRequestURI(), request.getQueryString());
      setClientIpFromRequest(request);
      setSessionFromRequest(request);
      setUserFromRequest(request);
      setHeadersFromRequest(request, headersToOmit);
      setPayloadFromRequest(request, maxPayloadLength);
    }
  }

  @Getter
  @Setter
  private int maxPayloadLength = -1;

  @Getter
  @Setter
  @NonNull
  private Set<String> headersToOmit = new HashSet<>();

  public boolean isIncludePayload() {
    return isIncludePayload(getMaxPayloadLength());
  }

  public static boolean isIncludePayload(int maxPayloadLength) {
    return maxPayloadLength > 0;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    StopWatch stopWatch = createStopWatchIfNecessary(request);
    String requestId = createRequestIdIfNecessary(request);

    boolean isFirstRequest = !isAsyncDispatch(request);
    HttpServletRequest requestToUse = request;

    try {
      if (isFirstRequest) {
        MdcUtils.setRequestId(requestId);
        response.addHeader(X_REQUEST_ID, requestId);
      }

      boolean shouldLog = shouldLog(requestToUse);

      if (shouldLog && isFirstRequest) {
        if (isIncludePayload() && !(request instanceof ContentCachingRequestWrapper)) {
          requestToUse = new ContentCachingRequestWrapper(request);
        }
        beforeRequest(requestToUse);
      }

      try {
        filterChain.doFilter(requestToUse, response);
      } finally {
        if (!isAsyncStarted(requestToUse)) {
          stopWatch.stop();
          request.removeAttribute(ATTRIBUTE_STOP_WATCH);
          request.removeAttribute(ATTRIBUTE_REQUEST_ID);
          if (shouldLog) {
            afterRequest(requestToUse, response, stopWatch.getTotalTimeMillis());
          }
        }
      }
    } finally {
      if (isFirstRequest) {
        MdcUtils.clean();
      }
    }
  }

  private StopWatch createStopWatchIfNecessary(HttpServletRequest request) {
    return Optional
        .ofNullable(request.getAttribute(ATTRIBUTE_STOP_WATCH))
        .filter(StopWatch.class::isInstance)
        .map(StopWatch.class::cast)
        .orElseGet(() -> {
          StopWatch stopWatch = new StopWatch();
          stopWatch.start();
          request.setAttribute(ATTRIBUTE_STOP_WATCH, stopWatch);
          return stopWatch;
        });
  }

  private String createRequestIdIfNecessary(HttpServletRequest request) {
    return Optional
        .ofNullable(request.getAttribute(ATTRIBUTE_REQUEST_ID))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .orElseGet(() -> {
          String requestId = "req-" + UUID.randomUUID().toString();
          request.setAttribute(ATTRIBUTE_REQUEST_ID, requestId);
          return requestId;
        });
  }

  /**
   * The default value is "false" so that the filter may log a "before" message at the start of
   * request processing and an "after" message at the end from when the last asynchronously
   * dispatched thread is exiting.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  public static Optional<String> safeTrimmedString(@Nullable String input) {
    return Optional.ofNullable(input).map(String::trim).map(Strings::emptyToNull);
  }

  public boolean shouldLog(HttpServletRequest request) {
    return LOG.isDebugEnabled();
  }

  /**
   * Writes a log message before the request is processed.
   *
   * @param request The HttpServletRequest object
   *
   */
  protected void beforeRequest(HttpServletRequest request) {
    try {
      LOG.debug(JsonUtils
          .serialize(new RequestWrapper(request, getMaxPayloadLength(), getHeadersToOmit())));
    } catch (JsonProcessingException ex) {
      LOG.error("Error logging request {}", request, ex);
    }
  }

  /**
   * Writes a log message after the request is processed.
   *
   * @param request The HttpServletRequest object
   * @param response The HttpServletResponse object
   * @param responseTime The response time
   */
  protected void afterRequest(HttpServletRequest request, HttpServletResponse response,
      double responseTime) {
    try {
      LOG.debug(JsonUtils.serialize(
          new ResponseWrapper(request, response, getMaxPayloadLength(), getHeadersToOmit(),
              responseTime)));
    } catch (JsonProcessingException ex) {
      LOG.error("Error logging response {} for request {}", response, request, ex);
    }
  }

}
