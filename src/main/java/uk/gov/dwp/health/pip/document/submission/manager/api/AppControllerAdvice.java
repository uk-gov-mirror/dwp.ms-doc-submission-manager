package uk.gov.dwp.health.pip.document.submission.manager.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.dwp.health.pip.document.submission.manager.exception.CreateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DrsRequestNotFoundException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DuplicateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.FileExceedLimitException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.SubmissionNotFoundException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.V3SubmissionServiceException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.ValidationException;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ErrorResponseObject;

import jakarta.validation.ConstraintViolationException;

@Component
@ControllerAdvice
public class AppControllerAdvice {

  private static Logger log = LoggerFactory.getLogger(AppControllerAdvice.class);

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Void> handle405(HttpRequestMethodNotSupportedException ex) {
    log.warn("Request method fail on {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
  }

  @ExceptionHandler(
      value = {
        ConstraintViolationException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        ValidationException.class,
      })
  public ResponseEntity<ErrorResponseObject> handle400(Exception ex) {
    log.error("Request failed on {}", ex.getMessage());
    ErrorResponseObject resp = new ErrorResponseObject();
    resp.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
  }

  @ExceptionHandler(V3SubmissionServiceException.class)
  public ResponseEntity<Void> handleV3SubmissionServiceException(V3SubmissionServiceException ex) {
    log.error("Submission error {}", ex.getMessage(), ex.getCause());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  @ExceptionHandler(CreateSubmissionException.class)
  public ResponseEntity<ErrorResponseObject> handleCreateNewSubmissionException(
      CreateSubmissionException ex) {
    ErrorResponseObject resp = new ErrorResponseObject();
    resp.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
  }

  @ExceptionHandler(DuplicateSubmissionException.class)
  public ResponseEntity<Void> handleDuplicationException() {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @ExceptionHandler(
      value = {
        SubmissionNotFoundException.class,
        DrsRequestNotFoundException.class,
        IllegalArgumentException.class
      })
  public ResponseEntity<ErrorResponseObject> handleRecordNotFound(RuntimeException ex) {
    log.error("Record not found error {}", ex.getMessage(), ex.getCause());
    ErrorResponseObject resp = new ErrorResponseObject();
    resp.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
  }

  @ExceptionHandler(FileExceedLimitException.class)
  public ResponseEntity<ErrorResponseObject> handleFileExceedAllowedBatchVol(
      FileExceedLimitException ex) {
    var resp = new ErrorResponseObject();
    resp.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(resp);
  }
}
