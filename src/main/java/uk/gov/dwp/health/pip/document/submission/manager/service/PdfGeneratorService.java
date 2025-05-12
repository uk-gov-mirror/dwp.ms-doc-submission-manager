package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.PdfGeneratorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.S3ConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.mapping.ApplicationToPdfSubmissionDtoMapper;
import uk.gov.dwp.health.pip.document.submission.manager.exception.PdfGenerationException;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.CreatePdfS3;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3PdfReturn;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGeneratorService {

  private final RestTemplate restTemplate;
  private final PdfGeneratorConfig config;
  private final ApplicationToPdfSubmissionDtoMapper applicationToPdfSubmissionDtoMapper;
  private final S3ConfigProperties s3ConfigProperties;

  public S3PdfReturn generateS3Pdf(HealthCaptureApplicationDto application,
      AuditableFormSpecificationDto formSpecification) {

    var headers = setHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    if (formSpecification == null) {
      throw new PdfGenerationException("Input application with Id "
              + application.getApplicationId() + " missing a form specification");
    }

    var requestBody = new CreatePdfS3();
    requestBody.setBucket(s3ConfigProperties.getBucket());
    requestBody.setSubmissionDto(
        applicationToPdfSubmissionDtoMapper.map(application, formSpecification));

    try {
      var httpEntity = new HttpEntity<>(requestBody, headers);
      var generatePdfEndpoint = config.getGeneratePdfS3Uri();
      var result = restTemplate.postForEntity(
          generatePdfEndpoint,
          httpEntity,
          S3PdfReturn.class);

      return result.getBody();
    } catch (RestClientException restClientException) {
      log.error(restClientException.getMessage());
      throw new PdfGenerationException(
              "PDF generation failed for application ID %s"
                      .formatted(application.getApplicationId()), restClientException);
    }
  }

  private HttpHeaders setHeaders() {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_PDF));
    return headers;
  }
}
