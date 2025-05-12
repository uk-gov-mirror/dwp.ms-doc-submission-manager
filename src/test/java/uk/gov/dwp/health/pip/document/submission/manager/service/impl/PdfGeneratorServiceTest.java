package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.CreatePdfS3;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3PdfReturn;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionDto;
import uk.gov.dwp.health.pip.document.submission.manager.service.PdfGeneratorService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PdfGeneratorServiceTest {

  private final static String MOCK_PDF_URI = "TEST_VERSION/TEST_BASE/TEST_ENDPOINT";
  private static HttpHeaders headers;
  @Mock
  private S3ConfigProperties s3ConfigProperties;
  private S3PdfReturn s3pdfReturn;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private PdfGeneratorConfig config;
  @Mock
  private ApplicationToPdfSubmissionDtoMapper applicationToPdfSubmissionDtoMapper;
  @InjectMocks
  private PdfGeneratorService pdfGeneratorService;
  private HttpEntity<CreatePdfS3> requestEntity;

  @BeforeAll
  static void setupSpec() {
    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

  }

  @BeforeEach
  void beforeEach() {
    pdfGeneratorService = new PdfGeneratorService(
        restTemplate, config,
        applicationToPdfSubmissionDtoMapper, s3ConfigProperties);
    s3pdfReturn = new S3PdfReturn();
    s3pdfReturn.setBucket("PIP_BUCKET");
    s3pdfReturn.setFileSizeKb(123456);
    s3pdfReturn.setS3Ref("S3_REF");
    when(s3ConfigProperties.getBucket()).thenReturn("PIP_BUCKET");
    var requestBody = new CreatePdfS3();
    requestBody.setSubmissionDto(new SubmissionDto());
    requestBody.setBucket("PIP_BUCKET");
    requestEntity = new HttpEntity<>(requestBody, headers);
  }

  @Test
  void pdf_generation_is_successful_return_success_in_result_wrapper() throws IOException {
    var existingApplication = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/healthCaptureApplication.json",
        HealthCaptureApplicationDto.class);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);

    when(config.getGeneratePdfS3Uri()).thenReturn(MOCK_PDF_URI);
    when(applicationToPdfSubmissionDtoMapper.map(existingApplication, formSpecification)).thenReturn(
        new SubmissionDto());
    when(restTemplate.postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class)).thenReturn(
        new ResponseEntity<>(s3pdfReturn, HttpStatus.OK));

    var result = pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification);
    assertThat(result).isNotNull();
    assertThat(result.getBucket()).isEqualTo("PIP_BUCKET");
    assertThat(result.getFileSizeKb()).isEqualTo(123456);
    assertThat(result.getS3Ref()).isEqualTo("S3_REF");
    verify(restTemplate, times(1)).postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class);
  }

  @Test
  void pdf_generation_is_unsuccessful_return_failure_in_result_wrapper() throws IOException {
    var existingApplication = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/healthCaptureApplication.json",
        HealthCaptureApplicationDto.class);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);

    when(config.getGeneratePdfS3Uri()).thenReturn(MOCK_PDF_URI);
    when(applicationToPdfSubmissionDtoMapper.map(existingApplication, formSpecification)).thenReturn(
        new SubmissionDto());
    when(restTemplate.postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class)).thenThrow(
        new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    PdfGenerationException result = assertThrows(PdfGenerationException.class,
            () -> pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification));

    assertThat(result.getMessage()).isEqualTo(
        "PDF generation failed for application ID 123456789");
    verify(restTemplate, times(1)).postForEntity(MOCK_PDF_URI, requestEntity, S3PdfReturn.class);
  }

}
