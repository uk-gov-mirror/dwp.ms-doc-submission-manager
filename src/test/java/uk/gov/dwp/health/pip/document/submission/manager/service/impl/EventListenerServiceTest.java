package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.EventConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.event.response.DrsUploadResponse;
import uk.gov.dwp.health.pip.document.submission.manager.event.response.Error;
import uk.gov.dwp.health.pip.document.submission.manager.service.CloudWatchMetricsService;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventListenerServiceTest {

  private static TestLogger mockLogger;
  @Captor ArgumentCaptor<String> strArgCaptor;
  @Captor ArgumentCaptor<DrsUpload> auditArgCaptor;
  @InjectMocks private EventListenerService cut;
  @Mock private EventConfigProperties props;
  @Mock private DataServiceImpl dataService;
  @Mock private ObjectMapper objectMapper;
  @Mock private CloudWatchMetricsService cloudWatchMetricsService;

  @BeforeAll
  static void setupSpec() {
    mockLogger = TestLoggerFactory.getTestLogger(EventListenerService.class);
  }

  @BeforeEach
  void setup() {
    mockLogger.clearAll();
    ReflectionTestUtils.setField(cut, "log", mockLogger);
  }

  @Nested
  class GetProperties {
    @Test
    void testGetQueueName() {
      when(props.getQueueName()).thenReturn("drs-response-queue");
      assertThat(cut.getQueueName()).isEqualTo("drs-response-queue");
    }
  }

  @Nested
  class HandleEventMessage {

    @Test
    @DisplayName("Test handle DRS successful response")
    void testHandleDrsSuccessfulResponse() {
      DrsUpload audit = spy(new DrsUpload());
      audit.setId("drs-request-id");
      Map<String, Object> payload = mock(Map.class);
      DrsUploadResponse response = new DrsUploadResponse();
      response.setSuccess(true);
      response.setRequestId("drs-request-id");
      MessageHeaders messageHeaders = new MessageHeaders(null);

      when(dataService.findDrsRequestByRequestId(anyString())).thenReturn(audit);
      when(objectMapper.convertValue(any(Map.class), any(Class.class))).thenReturn(response);
      cut.handleMessage(messageHeaders, payload);

      verify(dataService).findDrsRequestByRequestId(strArgCaptor.capture());
      assertThat(strArgCaptor.getValue()).isEqualTo("drs-request-id");
      assertThat(audit.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
      assertThat(audit.getErrors()).isNull();
      assertThat(audit.getAdditionalErrorDetails()).isNull();
    }

    @Test
    @DisplayName("Test handle DRS failure response")
    void testHandleDrsFailureResponse() {
      DrsUpload audit = spy(new DrsUpload());
      audit.setId("drs-request-id");
      Map<String, Object> payload = mock(Map.class);
      DrsUploadResponse response = new DrsUploadResponse();
      response.setSuccess(false);
      response.setErrorMessage("DRS FAIL REJECTED UPLOAD");
      response.setAdditionalErrorDetails(List.of(new Error()));
      response.setRequestId("drs-request-id");
      MessageHeaders messageHeaders = new MessageHeaders(null);

      when(dataService.findDrsRequestByRequestId(anyString())).thenReturn(audit);
      when(objectMapper.writeValueAsString(any(List.class)))
          .thenReturn("{additional-error-in-json}");
      when(objectMapper.convertValue(any(Map.class), any(Class.class))).thenReturn(response);
      cut.handleMessage(messageHeaders, payload);

      verify(dataService).findDrsRequestByRequestId(strArgCaptor.capture());
      assertThat(strArgCaptor.getValue()).isEqualTo("drs-request-id");
      verify(dataService).createUpdateDrsRequestAudit(auditArgCaptor.capture());
      DrsUpload requestAudit = auditArgCaptor.getValue();
      assertThat(requestAudit.getId()).isEqualTo("drs-request-id");
      assertThat(requestAudit.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
      assertThat(requestAudit.getErrors()).isEqualTo("DRS FAIL REJECTED UPLOAD");
      assertThat(requestAudit.getAdditionalErrorDetails()).isEqualTo("{additional-error-in-json}");
    }

    @Test
    @DisplayName("Test fail to marshal DRS details error to json string error logged")
    void testFailToMarshalDrsDetailsErrorToJsonStringErrorLogged() {
      DrsUpload audit = spy(new DrsUpload());
      audit.setId("drs-request-id");
      DrsUploadResponse response = new DrsUploadResponse();
      response.setSuccess(false);
      response.setErrorMessage("DRS FAIL REJECTED UPLOAD");
      response.setAdditionalErrorDetails(List.of(new Error()));
      response.setRequestId("drs-request-id");
      MessageHeaders messageHeaders = new MessageHeaders(null);
      Map<String, Object> payload = mock(Map.class);
      when(dataService.findDrsRequestByRequestId(anyString())).thenReturn(audit);
      when(objectMapper.writeValueAsString(any(List.class)))
          .thenThrow(JacksonException.class);
      when(objectMapper.convertValue(any(Map.class), any(Class.class))).thenReturn(response);
      cut.handleMessage(messageHeaders, payload);
      verify(dataService).findDrsRequestByRequestId(strArgCaptor.capture());
      assertThat(strArgCaptor.getValue()).isEqualTo("drs-request-id");
      verify(dataService).createUpdateDrsRequestAudit(auditArgCaptor.capture());
      DrsUpload requestAudit = auditArgCaptor.getValue();

      assertThat(requestAudit.getId()).isEqualTo("drs-request-id");
      assertThat(requestAudit.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
      assertThat(requestAudit.getErrors()).isEqualTo("DRS FAIL REJECTED UPLOAD");
      assertThat(requestAudit.getAdditionalErrorDetails()).isNull();

      assertThat(mockLogger.getLoggingEvents()).isNotNull();
      assertThat(mockLogger.getLoggingEvents().size())
          .isEqualTo(2);
      assertThat(mockLogger.getLoggingEvents().get(0))
          .isEqualTo(
                  new LoggingEvent(
                      Level.ERROR, "Fail to marshal additional error to JSON string"));
    }

    @Test
    @DisplayName("Test DRS request not found warning logged")
    void testDrsRequestNotFoundWarningLogged() {
      MessageHeaders messageHeaders = mock(MessageHeaders.class);
      DrsUploadResponse uploadResponse = mock(DrsUploadResponse.class);
      Map<String, Object> payload = mock(Map.class);
      when(objectMapper.convertValue(any(Map.class), any(Class.class))).thenReturn(uploadResponse);
      when(uploadResponse.getRequestId()).thenReturn("drs-request-id");
      when(dataService.findDrsRequestByRequestId(anyString())).thenReturn(null);
      cut.handleMessage(messageHeaders, payload);
      assertThat(mockLogger.getLoggingEvents())
          .isEqualTo(
              Collections.singletonList(
                  new LoggingEvent(
                      Level.WARN,
                      "Request ID {} did not find corresponding submission",
                      "drs-request-id")));
    }
  }
}
