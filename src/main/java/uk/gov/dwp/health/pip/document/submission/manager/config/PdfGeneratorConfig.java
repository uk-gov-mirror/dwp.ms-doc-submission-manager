package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.PdfGeneratorProperties;

@Slf4j
@Configuration
public class PdfGeneratorConfig {

  private final PdfGeneratorProperties pdfGeneratorProperties;

  public PdfGeneratorConfig(PdfGeneratorProperties pdfGeneratorProperties) {
    this.pdfGeneratorProperties = pdfGeneratorProperties;
  }

  public String getGeneratePdfUri() {
    return getBaseUriAndVersionUri() + this.pdfGeneratorProperties.getPdfApiEndpoint();
  }

  public String getGeneratePdfS3Uri() {
    return getBaseUriAndVersionUri() + this.pdfGeneratorProperties.getPdfS3ApiEndpoint();
  }

  private String getBaseUriAndVersionUri() {
    return this.pdfGeneratorProperties.getBaseUri() + "/"
        + this.pdfGeneratorProperties.getApiVersion() + "/";
  }
}
