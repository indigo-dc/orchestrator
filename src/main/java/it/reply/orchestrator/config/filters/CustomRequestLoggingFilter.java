package it.reply.orchestrator.config.filters;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CustomRequestLoggingFilter extends CommonsRequestLoggingFilter {

  private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

  private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

  private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

  private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;

  private boolean includeHeaders = false;

  private boolean includeResponseStatus = false;

  private boolean includeResponseTime = false;

  private List<String> headersToOmitt = Lists.newArrayList();

  /**
   * Set whether the request headers should be included in the log message.
   */
  public void setIncludeHeaders(boolean includeHeaders) {
    this.includeHeaders = includeHeaders;
  }

  /**
   * Return whether the request headers should be included in the log message.
   */
  public boolean isIncludeHeaders() {
    return this.includeHeaders;
  }

  /**
   * Set whether the response status should be included in the log message.
   */
  public void setIncludeResponseStatus(boolean includeResponseStatus) {
    this.includeResponseStatus = includeResponseStatus;
  }

  /**
   * Return whether the response status should be included in the log message.
   */
  public boolean isIncludeResponseStatus() {
    return this.includeResponseStatus;
  }

  public boolean isIncludeResponseTime() {
    return includeResponseTime;
  }

  public void setIncludeResponseTime(boolean includeResponseTime) {
    this.includeResponseTime = includeResponseTime;
  }

  public List<String> getHeadersToOmitt() {
    return headersToOmitt;
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

    if (isIncludePayload() && isFirstRequest
        && !(request instanceof ContentCachingRequestWrapper)) {
      requestToUse = new ContentCachingRequestWrapper(request);
    }

    boolean shouldLog = shouldLog(requestToUse);
    if (shouldLog && isFirstRequest) {
      beforeRequest(requestToUse, getBeforeMessage(requestToUse));
    }
    try {
      filterChain.doFilter(requestToUse, response);
    } finally {
      if (shouldLog && !isAsyncStarted(requestToUse)) {
        afterRequest(requestToUse, getAfterMessage(requestToUse, response, startTime));
      }
    }
  }

  protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
    return this.buildMessage(request, null, -1, prefix, suffix).toString();
  }

  protected String createMessage(HttpServletRequest request, HttpServletResponse response,
      long startTime, String prefix, String suffix) {
    return this.buildMessage(request, response, startTime, prefix, suffix).toString();
  }

  protected StringBuilder buildMessage(HttpServletRequest request, HttpServletResponse response,
      long startTime, String prefix, String suffix) {
    StringBuilder msg = new StringBuilder();
    msg.append(prefix);
    msg.append("httpMethod=").append(request.getMethod());
    msg.append(";uri=").append(request.getRequestURI());

    if (isIncludeQueryString()) {
      String queryString = request.getQueryString();
      if (queryString != null) {
        msg.append('?').append(queryString);
      }
    }

    if (isIncludeClientInfo()) {
      String client = getRemoteAddr(request);
      if (StringUtils.hasLength(client)) {
        msg.append(";client=").append(client);
      }
      HttpSession session = request.getSession(false);
      if (session != null) {
        msg.append(";session=").append(session.getId());
      }
      String user = request.getRemoteUser();
      if (user != null) {
        msg.append(";user=").append(user);
      }
    }

    if (isIncludeHeaders()) {
      Map<String, String> headers =
          new ServletServerHttpRequest(request).getHeaders().toSingleValueMap();
      for (String headerKey : getHeadersToOmitt()) {
        if (headers.containsKey(headerKey)) {
          headers.put(headerKey, "<omitted>");
        }
      }
      msg.append(";headers=").append(headers);
    }

    if (isIncludePayload()) {
      ContentCachingRequestWrapper wrapper =
          WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
      if (wrapper != null) {
        byte[] buf = wrapper.getContentAsByteArray();
        if (buf.length > 0) {
          int length = Math.min(buf.length, getMaxPayloadLength());
          String payload;
          try {
            payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
          } catch (UnsupportedEncodingException ex) {
            payload = "[unknown]";
          }
          msg.append(";payload=").append(payload);
        }
      }
    }
    if (response != null) {
      if (isIncludeResponseStatus()) {
        msg.append(";responseStatus=").append(response.getStatus());
      }
      if (this.isIncludeResponseTime()) {
        msg.append(";responseTime=").append((System.nanoTime() - startTime) / 1e6).append("ms");
      }
    }

    msg.append(suffix);
    return msg;
  }

  private String getRemoteAddr(HttpServletRequest request) {
    String remoteAddress = null;
    String[] forwardedAddress =
        Strings.nullToEmpty(request.getHeader("X-Forwarded-For")).split(", ?");
    if (ArrayUtils.isEmpty(forwardedAddress)
        || Strings.nullToEmpty(forwardedAddress[0]).trim().isEmpty()) {
      remoteAddress = request.getRemoteAddr();
    } else {
      remoteAddress = forwardedAddress[0];
    }
    return remoteAddress;
  }

  /**
   * Get the message to write to the log before the request.
   * 
   * @see #createMessage
   */
  private String getBeforeMessage(HttpServletRequest request) {
    return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
  }

  /**
   * Get the message to write to the log after the request.
   * 
   * @param startDate
   * 
   * @see #createMessage
   */
  private String getAfterMessage(HttpServletRequest request, HttpServletResponse response,
      long startTime) {
    return createMessage(request, response, startTime, this.afterMessagePrefix,
        this.afterMessageSuffix);
  }

}
