package uk.gov.dwp.health.pip.document.submission.manager.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.PdfGeneratorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.S3ConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.mapping.ApplicationToPdfSubmissionDtoMapper;
import uk.gov.dwp.health.pip.document.submission.manager.exception.PdfGenerationException;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.CreatePdfS3V4;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.S3PdfReturn;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGeneratorService {

  private final RestTemplate restTemplate;
  private final PdfGeneratorConfig config;
  private final ApplicationToPdfSubmissionDtoMapper applicationToPdfSubmissionDtoMapper;
  private final S3ConfigProperties s3ConfigProperties;

  public S3PdfReturn generateS3Pdf(HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2) {

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    CreatePdfS3V4 requestBody = new CreatePdfS3V4();
    requestBody.setBucket(s3ConfigProperties.getBucket());
    requestBody.setSubmissionDto(
        applicationToPdfSubmissionDtoMapper.map(healthCaptureApplicationDtoV2));

    try {
      HttpEntity<CreatePdfS3V4> httpEntity = new HttpEntity<>(requestBody, headers);
      log.info(
          "Size of http entity for application id {} is {} bytes",
          healthCaptureApplicationDtoV2.getApplicationId(),
          httpEntity.toString().getBytes(StandardCharsets.UTF_8).length);

      String generatePdfEndpoint = config.getGeneratePdfS3Uri();
      ResponseEntity<S3PdfReturn> result =
          restTemplate.postForEntity(generatePdfEndpoint, httpEntity, S3PdfReturn.class);

      return result.getBody();
    } catch (RestClientException restClientException) {
      log.error(restClientException.getMessage());
      throw new PdfGenerationException(
          "Request to PDF generation service failed for application ID %s"
              .formatted(healthCaptureApplicationDtoV2.getApplicationId()),
          restClientException);
    }
  }
}
