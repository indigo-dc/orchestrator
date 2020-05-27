package it.reply.orchestrator.config.properties;

import java.net.URI;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@ConfigurationProperties(prefix = "rucio")
@NoArgsConstructor
public class RucioProperties {

  @NotNull
  @NonNull
  private URI url;

}
