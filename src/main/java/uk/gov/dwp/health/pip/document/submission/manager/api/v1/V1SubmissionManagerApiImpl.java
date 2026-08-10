package uk.gov.dwp.health.pip.document.submission.manager.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.AttachDocumentResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QueryRequestResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ReportingResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitDrsRequestObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionAttachObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.v1.api.V1Api;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.QueryServiceImpl;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.V1SubmissionServiceImpl;

@Slf4j
@Controller
@RequiredArgsConstructor
public class V1SubmissionManagerApiImpl implements V1Api {

  private final V1SubmissionServiceImpl v1SubmissionService;
  private final QueryServiceImpl queryServiceImpl;

  @Override
  public ResponseEntity<AttachDocumentResponseObjectV1> attachToExisting(
      String userId, SubmissionAttachObjectV1 submissionAttachObjectV1) {
    log.info(
        "Invoke V1 attach further evidence for submission id {}",
        submissionAttachObjectV1.getSubmissionId());

    ResponseEntity<AttachDocumentResponseObjectV1> response =
        ResponseEntity.accepted()
            .body(
                v1SubmissionService.attachDocumentToExistingSubmission(
                    userId, submissionAttachObjectV1));

    log.info(
        "Finished V1 attach further evidence for submission id {}",
        submissionAttachObjectV1.getSubmissionId());

    return response;
  }

  @Override
  public ResponseEntity<ReportingResponseObject> getReportingData(final String dayOrWeek) {
    return ResponseEntity.ok(queryServiceImpl.getReportingData(dayOrWeek));
  }

  @Override
  public ResponseEntity<QueryRequestResponseObject> queryDrsRequestStatus(String requestId) {
    return ResponseEntity.ok(queryServiceImpl.queryRequestStatusById(requestId));
  }

  @Override
  public ResponseEntity<ResubmitResponseObject> resubmitDrsRequest(
      ResubmitDrsRequestObjectV1 resubmitDrsRequestObject) {
    return ResponseEntity.accepted().body(v1SubmissionService.resubmit(resubmitDrsRequestObject));
  }
}
