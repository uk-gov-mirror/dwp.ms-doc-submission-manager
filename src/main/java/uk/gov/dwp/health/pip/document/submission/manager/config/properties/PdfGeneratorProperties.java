package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(value = "uk.gov.dwp.health.pip2.pdf.generator")
@Getter
@Setter
@Validated
public class PdfGeneratorProperties {

  @NotBlank(message = "Base uri should not be blank")
  private String baseUri;

  @NotBlank(message = "Version should not be blank")
  private String apiVersion;

  @NotBlank(message = "S3 Endpoint should not be blank")
  private String pdfS3ApiEndpoint;
}
