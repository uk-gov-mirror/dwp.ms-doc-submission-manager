package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.PdfGeneratorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.S3ConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.mapping.ApplicationToPdfSubmissionDtoMapper;
import uk.gov.dwp.health.pip.document.submission.manager.exception.PdfGenerationException;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.CreatePdfS3V4;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.S3PdfReturn;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.SubmissionDtoV4;
import uk.gov.dwp.health.pip.document.submission.manager.service.PdfGeneratorService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

@ExtendWith(MockitoExtension.class)
class PdfGeneratorServiceTest {

  private static final String MOCK_PDF_URI = "TEST_VERSION/TEST_BASE/TEST_ENDPOINT";
  private static HttpHeaders headers;
  @Mock private S3ConfigProperties s3ConfigProperties;
  @Mock private RestTemplate restTemplate;
  @Mock private PdfGeneratorConfig config;
  @Mock private ApplicationToPdfSubmissionDtoMapper applicationToPdfSubmissionDtoMapper;
  @InjectMocks private PdfGeneratorService pdfGeneratorService;

  @BeforeAll
  static void setupSpec() {
    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
  }

  @Nested
  class V3PdfGeneration {

    private S3PdfReturn s3pdfReturn;
    private HttpEntity<CreatePdfS3V4> requestEntity;

    @BeforeEach
    void beforeEach() {
      pdfGeneratorService =
          new PdfGeneratorService(
              restTemplate, config,
              applicationToPdfSubmissionDtoMapper, s3ConfigProperties);
      s3pdfReturn = new S3PdfReturn();
      s3pdfReturn.setBucket("PIP_BUCKET");
      s3pdfReturn.setFileSizeKb(123456);
      s3pdfReturn.setS3Ref("S3_REF");
      when(s3ConfigProperties.getBucket()).thenReturn("PIP_BUCKET");
      CreatePdfS3V4 requestBody = new CreatePdfS3V4();
      requestBody.setSubmissionDto(new SubmissionDtoV4());
      requestBody.setBucket("PIP_BUCKET");
      requestEntity = new HttpEntity<>(requestBody, headers);
    }

    @Test
    void v3_pdf_generation_is_successful_return_success_in_result_wrapper() throws IOException {
      HealthCaptureApplicationDtoV2 existingApplication =
          JsonUtils.readJsonFromFileAndMap(
              "src/test/resources/entity/healthCaptureApplication.json",
              HealthCaptureApplicationDtoV2.class);

      when(config.getGeneratePdfS3Uri()).thenReturn(MOCK_PDF_URI);
      when(applicationToPdfSubmissionDtoMapper.map(existingApplication))
          .thenReturn(new SubmissionDtoV4());
      when(restTemplate.postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class))
          .thenReturn(new ResponseEntity<>(s3pdfReturn, HttpStatus.OK));

      var result = pdfGeneratorService.generateS3Pdf(existingApplication);
      assertThat(result).isNotNull();
      assertThat(result.getBucket()).isEqualTo("PIP_BUCKET");
      assertThat(result.getFileSizeKb()).isEqualTo(123456);
      assertThat(result.getS3Ref()).isEqualTo("S3_REF");
      verify(restTemplate, times(1)).postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class);
    }

    @Test
    void v3_pdf_generation_is_unsuccessful_return_failure_in_result_wrapper() throws IOException {
      HealthCaptureApplicationDtoV2 existingApplication =
          JsonUtils.readJsonFromFileAndMap(
              "src/test/resources/entity/dto/getV2HealthDataResponseBody.json",
              HealthCaptureApplicationDtoV2.class);

      when(config.getGeneratePdfS3Uri()).thenReturn(MOCK_PDF_URI);
      when(applicationToPdfSubmissionDtoMapper.map(existingApplication))
          .thenReturn(new SubmissionDtoV4());
      when(restTemplate.postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class))
          .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

      PdfGenerationException result =
          assertThrows(
              PdfGenerationException.class,
              () -> pdfGeneratorService.generateS3Pdf(existingApplication));

      assertThat(result.getMessage())
          .isEqualTo("Request to PDF generation service failed for application ID 123456789");
      verify(restTemplate, times(1)).postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class);
    }
  }
}
