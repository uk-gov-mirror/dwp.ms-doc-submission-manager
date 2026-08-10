package uk.gov.dwp.health.pip.document.submission.manager.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.AttachDocumentResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QueryRequestResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ReportingResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RequestId;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.Resubmission;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitDrsRequestObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionAttachObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionId;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.QueryServiceImpl;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.V1SubmissionServiceImpl;

@ExtendWith(MockitoExtension.class)
class V1SubmissionManagerApiImplTest {

  @Captor ArgumentCaptor<ResubmitDrsRequestObjectV1> requestObjectArgumentCaptor;

  @InjectMocks private V1SubmissionManagerApiImpl cut;
  @Mock private QueryServiceImpl queryService;
  @Mock private V1SubmissionServiceImpl submissionService;

  @Test
  void testAttachToExistingEndpoint() {
    var submissionAttachObject = mock(SubmissionAttachObjectV1.class);
    var response = new AttachDocumentResponseObjectV1();
    response.setDrsRequestIds(List.of(new RequestId().requestId("drs_request_id")));

    when(submissionService.attachDocumentToExistingSubmission("userId", submissionAttachObject))
        .thenReturn(response);
    var actual = cut.attachToExisting("userId",
        submissionAttachObject);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(Objects.requireNonNull(actual.getBody())).isEqualTo(response);
    verify(submissionService).attachDocumentToExistingSubmission("userId", submissionAttachObject);
  }

  @Test
  void testGetSubmissionDocumentEndpoint() {
    var requestId = "request-id";
    var resp = new QueryRequestResponseObject();
    resp.setDrsUploadStatus(QueryRequestResponseObject.DrsUploadStatusEnum.SUCCESS);
    resp.setRequestId("drs_request_id");
    when(queryService.queryRequestStatusById(requestId)).thenReturn(resp);
    var actual = cut.queryDrsRequestStatus(requestId);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(Objects.requireNonNull(actual.getBody())).isEqualTo(resp);
    verify(queryService).queryRequestStatusById(requestId);
  }

  @Test
  void testGetReportingDataEndpoint() {
    var requestId = "day";
    var resp = new ReportingResponseObject();
    resp.setSubmissionTotal(6);
    resp.setSuccessfulSubmission(3);
    resp.setInflightSubmission(1);
    resp.setFailedSubmission(1);
    resp.setReceivedSubmission(1);
    var subId = new SubmissionId();
    subId.setSubmissionId("sub-id-444");
    resp.setFailureDetails(List.of(subId));
    when(queryService.getReportingData(requestId)).thenReturn(resp);
    var actual = cut.getReportingData(requestId);
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(Objects.requireNonNull(actual.getBody())).isEqualTo(resp);
    verify(queryService).getReportingData(requestId);
  }

  @Test
  @DisplayName("should return resubmitted with accepted status code")
  void shouldReturnResubmittedWithAcceptedStatusCode() {
    var drsRequestObject = new ResubmitDrsRequestObjectV1();
    var requestId = new RequestId();
    requestId.setRequestId("1234");
    drsRequestObject.setDrsRequestIds(List.of(requestId));
    var responseObject = new ResubmitResponseObject();
    var resubmission = new Resubmission();
    resubmission.setRetryDrsRequestId("5678");
    resubmission.setFailedDrsRequestId("1234");
    responseObject.setResubmits(List.of());
    when(submissionService.resubmit(any(ResubmitDrsRequestObjectV1.class)))
        .thenReturn(responseObject);
    var actualResponse = cut.resubmitDrsRequest(drsRequestObject);
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(actualResponse.getBody()).isEqualTo(responseObject);
    verify(submissionService).resubmit(requestObjectArgumentCaptor.capture());
    assertThat(requestObjectArgumentCaptor.getValue()).isEqualTo(drsRequestObject);
  }

}
