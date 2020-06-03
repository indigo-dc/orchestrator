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

package it.reply.orchestrator.dto.messaging.rucio;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferFailedEvent
    extends Event<TransferFailedEvent.TransferFailedEventEventPayload> {

  public static final String EVENT_TYPE = "transfer-failed";

  public TransferFailedEvent(@NonNull String eventType, @NonNull String createdAt,
      @NonNull TransferFailedEventEventPayload payload) {
    super(eventType, createdAt, payload);
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TransferFailedEventEventPayload implements EventPayload {
    @JsonProperty("src-url")
    public String srcUrl;
    @JsonProperty("protocol")
    public String protocol;
    @JsonProperty("checksum-adler")
    public String checksumAdler;
    @JsonProperty("dst-rse")
    public String dstRse;
    @JsonProperty("previous-request-id")
    public String previousRequestId;
    @JsonProperty("tool-id")
    public String toolId;
    @JsonProperty("duration")
    public BigInteger duration;
    @JsonProperty("transfer-endpoint")
    public String transferEndpoint;
    @JsonProperty("started_at")
    public String startedAt;
    @JsonProperty("guid")
    public String guid;
    @JsonProperty("file-size")
    public BigInteger fileSize;
    @JsonProperty("scope")
    public String scope;
    @JsonProperty("transferred_at")
    public String transferredAt;
    @JsonProperty("src-type")
    public String srcType;
    @JsonProperty("dst-type")
    public String dstType;
    @JsonProperty("reason")
    public String reason;
    @JsonProperty("name")
    public String name;
    @JsonProperty("account")
    public String account;
    @JsonProperty("submitted_at")
    public String submittedAt;
    @JsonProperty("transfer-id")
    public String transferId;
    @JsonProperty("request-id")
    public String requestId;
    @JsonProperty("checksum-md5")
    public String checksumMd5;
    @JsonProperty("bytes")
    public BigInteger bytes;
    @JsonProperty("dst-url")
    public String dstUrl;
    @JsonProperty("activity")
    public String activity;
    @JsonProperty("transfer-link")
    public String transferLink;
    @JsonProperty("src-rse")
    public String srcRse;
    @JsonProperty("created_at")
    public String createdAt;

  }
}
