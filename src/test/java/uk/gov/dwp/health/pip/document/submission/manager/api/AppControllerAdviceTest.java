package uk.gov.dwp.health.pip.document.submission.manager.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.CreateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DrsRequestNotFoundException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.FileExceedLimitException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.V3SubmissionServiceException;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ErrorResponseObject;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class AppControllerAdviceTest {

  private static AppControllerAdvice appControllerAdvice;
  private TestLogger testLogger = TestLoggerFactory.getTestLogger(AppControllerAdvice.class);

  @BeforeAll
  static void setupSpec() {
    appControllerAdvice = new AppControllerAdvice();
  }

  @BeforeEach
  void setup() {
    testLogger.clearAll();
    ReflectionTestUtils.setField(appControllerAdvice, "log", testLogger);
  }

  @Test
  void testHandleValidationMethodArgHttpMsgExceptionErrorLogged() {
    Exception ex = mock(Exception.class);
    when(ex.getMessage()).thenReturn("bad request");
    ResponseEntity<ErrorResponseObject> actual = appControllerAdvice.handle400(ex);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(Objects.requireNonNull(actual.getBody()).getMessage()).isEqualTo("bad request");
    assertThat(testLogger.getLoggingEvents())
        .containsExactly(new LoggingEvent(Level.ERROR, "Request failed on {}", "bad request"));
  }

  @Test
  void testHandleHttpRequestMethodNotSupportedExceptionErrorLogged() {
    HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
    when(ex.getMessage()).thenReturn("incorrect rest verb");
    ResponseEntity<Void> actual = appControllerAdvice.handle405(ex);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(actual.getBody()).isNull();
    assertThat(testLogger.getLoggingEvents())
        .containsExactly(
            new LoggingEvent(Level.WARN, "Request method fail on {}", "incorrect rest verb"));
  }

  @Test
  void testHandleUnknownRuntimeExceptionErrorLogged() {
    V3SubmissionServiceException ex = mock(V3SubmissionServiceException.class);
    when(ex.getMessage()).thenReturn("unknown internal error");
    when(ex.getCause()).thenReturn(new Throwable("submission throwable"));
    ResponseEntity<Void> actual = appControllerAdvice.handleV3SubmissionServiceException(ex);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(actual.getBody()).isNull();

    ImmutableList<LoggingEvent> loggingEvents = testLogger.getLoggingEvents();
    assertThat(loggingEvents)
        .containsExactly(
            new LoggingEvent(
                Level.ERROR,
                loggingEvents.get(0).getThrowable().get(),
                "Submission error {}",
                "unknown internal error"));
  }

  @Test
  void testHandleFailCreateNewSubmission() {
    CreateSubmissionException ex = mock(CreateSubmissionException.class);
    when(ex.getMessage()).thenReturn("fail create new submission");
    ResponseEntity<ErrorResponseObject> actual =
        appControllerAdvice.handleCreateNewSubmissionException(ex);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(Objects.requireNonNull(actual.getBody()).getMessage())
        .isEqualTo("fail create new submission");
  }

  @Test
  void testHandleDuplicationException() {
    ResponseEntity<Void> actual = appControllerAdvice.handleDuplicationException();
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(actual.getBody()).isNull();
  }

  @Test
  @DisplayName("Test handle drs request record not found exception")
  void testHandleDrsRecordNotFoundException() {
    RuntimeException exception = new DrsRequestNotFoundException("drs request not found");
    ResponseEntity<ErrorResponseObject> actual =
        appControllerAdvice.handleRecordNotFound(exception);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(Objects.requireNonNull(actual.getBody()).getMessage())
        .isEqualTo("drs request not found");
  }

  @Test
  @DisplayName("Test handle illegal argument not found exception")
  void testHandleIllegalArgumentNotFoundException() {
    RuntimeException exception = new IllegalArgumentException("Illegal method argument passed");
    ResponseEntity<ErrorResponseObject> actual =
        appControllerAdvice.handleRecordNotFound(exception);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(Objects.requireNonNull(actual.getBody()).getMessage())
        .isEqualTo("Illegal method argument passed");
  }

  @Test
  @DisplayName("test should return NOT_ACCEPTABLE status")
  void testShouldReturnNotAcceptableStatus() {
    var exception = new FileExceedLimitException("File exceed limit 999");
    ResponseEntity<ErrorResponseObject> actual =
        appControllerAdvice.handleFileExceedAllowedBatchVol(exception);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    assertThat(Objects.requireNonNull(actual.getBody()).getMessage())
        .isEqualTo("File exceed limit 999");
  }
}
