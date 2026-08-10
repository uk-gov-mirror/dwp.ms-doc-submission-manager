package uk.gov.dwp.health.pip.document.submission.manager.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.v3.api.V3Api;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.v3.model.ApplicationIdDto;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.V3SubmissionServiceImpl;

@Slf4j
@Controller
@RequiredArgsConstructor
public class V3SubmissionManagerApiImpl implements V3Api {

  private final V3SubmissionServiceImpl v3SubmissionService;

  @Override
  public ResponseEntity<Object> applyPIP(
      String userId, ApplicationIdDto applicationIdDto) {
    String applicationId = applicationIdDto.getApplicationId();
    log.info("Invoke V3 initial submission for application ID {}", applicationId);
    return ResponseEntity.accepted()
        .body(v3SubmissionService.createNewSubmission(applicationId, userId));
  }
}
