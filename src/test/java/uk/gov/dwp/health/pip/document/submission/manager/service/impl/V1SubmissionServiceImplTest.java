package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.TestConstant;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.DrsBusinessUnit;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.DrsMetaProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.EventConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.event.request.DrsUploadRequest;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DuplicateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.model.DrsStatusEnum;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ApplicationMeta;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RequestId;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitDrsRequestObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionAttachObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.service.ApplicationCoordinatorService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.Batch;
import uk.gov.dwp.health.pip.document.submission.manager.utils.RequestPartition;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionAttachObjectV1.RegionEnum.GB;

@ExtendWith(MockitoExtension.class)
class V1SubmissionServiceImplTest {

  @Captor private ArgumentCaptor<String> strArgCaptor;
  @Captor private ArgumentCaptor<DrsUploadRequest> drsUploadReqArgCaptor;
  @Captor private ArgumentCaptor<Submission> submissionArgCaptor;
  @Captor private ArgumentCaptor<List<S3RequestDocumentObject>> listArgumentCaptor;
  @Captor private ArgumentCaptor<DrsUpload> auditArgumentCaptor;
  @InjectMocks private V1SubmissionServiceImpl cut;
  @Mock private EventPublisherImpl publisher;
  @Mock private S3UrlResolverImpl s3UrlResolver;
  @Mock private DrsMetaProperties drsMetaProperties;
  @Mock private EventConfigProperties eventConfigProperties;
  @Mock private DataServiceImpl dataService;
  @Mock private DateFormat dateFormat;
  @Mock private RequestPartition partitionUtil;
  @Mock private ApplicationCoordinatorService applicationCoordinatorService;
  @Mock private EmailNotificationServiceImpl emailNotificationService;
  @Mock private GetClaimantEmailService getClaimantEmailService;

  @Test
  @DisplayName("test create new submission returns submission response object")
  void testCreateNewSubmissionReturnsSubmissionResponseObject() {
    var submission = new PipApplicationV1();
    submission.setRegion(PipApplicationV1.RegionEnum.NI);
    submission.setClaimantId(TestConstant.CLAIMANT_ID_1);
    submission.setApplicationId(TestConstant.APPLICATION_ID);
    submission.setDrsMetadata(drsMetaFixture());

    var reqDocObj = new S3RequestDocumentObject();
    reqDocObj.setBucket(TestConstant.S3_BUCKET);
    reqDocObj.setContentType("application/pdf");
    reqDocObj.setS3Ref(TestConstant.URL);
    reqDocObj.setSize(1024);
    reqDocObj.setName(TestConstant.FILE_NAME);
    reqDocObj.setDrsDocType(S3RequestDocumentObject.DrsDocTypeEnum._1241);
    reqDocObj.setDateTime(LocalDateTime.of(2021, 1, 1, 1, 10, 10));
    var appMeta = new ApplicationMeta();
    appMeta.setStartDate(LocalDate.of(2021, 1, 1));
    appMeta.setCompletedDate(LocalDate.of(2021, 1, 2));
    submission.setApplicationMeta(appMeta);

    when(dataService.findSubmissionByClaimantIdAndClaimId(anyString(), anyString()))
        .thenReturn(null);
    Batch batch = mock(Batch.class);
    when(batch.getBatch()).thenReturn(List.of(reqDocObj));
    when(partitionUtil.partition(any())).thenReturn(List.of(batch));

    var savedSubmission = mock(Submission.class);
    when(savedSubmission.getId()).thenReturn(TestConstant.SUBMISSION_ID);
    when(dataService.createUpdateSubmission(any(Submission.class))).thenReturn(savedSubmission);
    var savedDrsUpload = mock(DrsUpload.class);
    var documentation = new Documentation();
    documentation.setId(TestConstant.DOC_ID_1);
    when(dataService.createUpdateDocumentation(any(Documentation.class))).thenReturn(documentation);
    when(savedDrsUpload.getId()).thenReturn(TestConstant.REQUEST_ID_1);
    when(dataService.createUpdateDrsRequestAudit(any(DrsUpload.class))).thenReturn(savedDrsUpload);

    var businessUnit = new DrsBusinessUnit();
    businessUnit.setCorrelationId("test-coll-id");
    businessUnit.setCallerId("test-caller-id");
    when(drsMetaProperties.findBusinessUnitByRegionCode(anyString())).thenReturn(businessUnit);

    var actual = cut.createNewSubmission(submission);
    assertAll("assert submission response object", () -> assertNotNull(actual));

    InOrder order = inOrder(s3UrlResolver, drsMetaProperties, publisher, dataService, dateFormat);

    order
        .verify(dataService)
        .findSubmissionByClaimantIdAndClaimId(strArgCaptor.capture(), strArgCaptor.capture());
    order.verify(s3UrlResolver).resolve(strArgCaptor.capture(), strArgCaptor.capture());
    order.verify(publisher).publishEvent(drsUploadReqArgCaptor.capture());
    DrsUploadRequest actualRequest = drsUploadReqArgCaptor.getValue();
    order.verify(dataService).findDrsRequestByRequestId(strArgCaptor.capture());

    assertAll(
        "assert all",
        () -> {
          assertThat(actualRequest.getCallerId()).isEqualTo("test-caller-id");
          assertThat(actualRequest.getCorrelationId()).isEqualTo("test-coll-id");
          assertThat(strArgCaptor.getAllValues())
              .isEqualTo(
                  List.of(
                      TestConstant.CLAIMANT_ID_1,
                      TestConstant.APPLICATION_ID,
                      TestConstant.S3_BUCKET,
                      TestConstant.URL,
                      TestConstant.REQUEST_ID_1));
        });
  }

  @Test
  @DisplayName("test create new submission throws duplicate exception")
  void testCreateNewSubmissionThrowsDuplicateException() {
    var application = mock(PipApplicationV1.class);
    when(application.getClaimantId()).thenReturn(TestConstant.CLAIMANT_ID_2);
    when(application.getApplicationId()).thenReturn(TestConstant.APPLICATION_ID);
    when(dataService.findSubmissionByClaimantIdAndClaimId(anyString(), anyString()))
        .thenReturn(mock(Submission.class));
    assertThrows(DuplicateSubmissionException.class, () -> cut.createNewSubmission(application));
    verify(dataService)
        .findSubmissionByClaimantIdAndClaimId(strArgCaptor.capture(), strArgCaptor.capture());
    assertThat(strArgCaptor.getAllValues())
        .containsSequence(TestConstant.CLAIMANT_ID_2, TestConstant.APPLICATION_ID);
    verifyNoInteractions(s3UrlResolver, publisher, dateFormat);
  }

  @Test
  @DisplayName("test attach further evidence returns attach document response object")
  void testAttachFurtherEvidenceReturnsAttachDocumentResponseObject() throws JsonProcessingException {
    var attachObject = new SubmissionAttachObjectV1();
    attachObject.setSubmissionId(TestConstant.SUBMISSION_ID);
    attachObject.setRegion(GB);
    var drsMeta = new DrsMetadata();
    drsMeta.setForename("forename");
    drsMeta.setSurname("surname");
    drsMeta.setNino("AA370773A");
    drsMeta.setDob(LocalDate.of(1990, 1, 1));
    attachObject.setDrsMetadata(drsMeta);

    var s3ReqDoc = new S3RequestDocumentObject();
    s3ReqDoc.setBucket(TestConstant.S3_BUCKET);
    s3ReqDoc.setContentType("application/pdf");
    s3ReqDoc.setS3Ref(TestConstant.URL);
    s3ReqDoc.setSize(1024);
    s3ReqDoc.setName(TestConstant.FILE_NAME);
    s3ReqDoc.setDrsDocType(S3RequestDocumentObject.DrsDocTypeEnum._1241);
    s3ReqDoc.setDateTime(LocalDateTime.of(2021, 1, 1, 1, 10, 10));
    attachObject.setDocuments(List.of(s3ReqDoc));

    var batch = mock(Batch.class);
    when(batch.getBatch()).thenReturn(List.of(s3ReqDoc));
    when(partitionUtil.partition(any())).thenReturn(List.of(batch));

    var existingSubmission = new Submission();
    existingSubmission.setId(TestConstant.SUBMISSION_ID);
    existingSubmission.setClaimantId(TestConstant.CLAIMANT_ID_1);
    existingSubmission.setApplicationId(TestConstant.APPLICATION_ID);
    existingSubmission.setDocumentIdIds(new ArrayList<>());
    when(dataService.findSubmissionById(anyString())).thenReturn(existingSubmission);

    var documentation = new Documentation();
    documentation.setId(TestConstant.DOC_ID_1);
    when(dataService.createUpdateDocumentation(any(Documentation.class))).thenReturn(documentation);

    var drsRequestAudit = new DrsUpload();
    drsRequestAudit.setId(TestConstant.REQUEST_ID_1);
    when(dataService.createUpdateDrsRequestAudit(any(DrsUpload.class))).thenReturn(drsRequestAudit);

    var businessUnit = new DrsBusinessUnit();
    businessUnit.setCorrelationId("test-coll-id");
    businessUnit.setCallerId("test-caller-id");
    when(drsMetaProperties.findBusinessUnitByRegionCode(anyString())).thenReturn(businessUnit);

    when(applicationCoordinatorService.getApplication(TestConstant.APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<ApplicationDetails>builder()
                .value(
                    new ApplicationDetails()
                        .region(ApplicationDetails.RegionEnum.GB)
                        .journeyType(ApplicationDetails.JourneyTypeEnum.PIP2_INVITED)
                        .language(ApplicationDetails.LanguageEnum.EN)
                        .nino("AA370773A"))
                .build());

    when(getClaimantEmailService.getEmailFromIdentityByUserId("test-user-id"))
        .thenReturn("test-email");

    doNothing()
        .when(emailNotificationService)
        .sendAttachDocsEmailNotification(
            anyString(), anyString(), anyString(), anyString(), anyString());

    var actual = cut.attachDocumentToExistingSubmission("test-user-id",
        attachObject);

    InOrder order =
        inOrder(
            s3UrlResolver, drsMetaProperties, publisher, dataService, dateFormat, partitionUtil);

    order.verify(partitionUtil).partition(listArgumentCaptor.capture());
    order.verify(dataService).findSubmissionById(strArgCaptor.capture());
    order.verify(s3UrlResolver).resolve(strArgCaptor.capture(), strArgCaptor.capture());
    order.verify(dataService).createUpdateSubmission(submissionArgCaptor.capture());
    order.verify(publisher).publishEvent(drsUploadReqArgCaptor.capture());
    order.verify(dataService).findDrsRequestByRequestId(strArgCaptor.capture());

    assertThat(listArgumentCaptor.getValue()).hasSize(1);
    Submission capturedSubmission = submissionArgCaptor.getValue();
    assertThat(capturedSubmission.getId()).isEqualTo("submission_id_001");
    assertThat(capturedSubmission.getApplicationId()).isEqualTo("claim_id_001");
    assertThat(capturedSubmission.getClaimantId()).isEqualTo("claimant_id_001");
    var expected = new DocumentId();
    expected.setDocumentId(TestConstant.DOC_ID_1);
    assertThat(capturedSubmission.getDocumentIdIds())
        .usingFieldByFieldElementComparator()
        .contains(expected);
    assertAll(
        "assert attach document response object",
        () -> {
          assertThat(actual.getDrsRequestIds())
              .isEqualTo(List.of(new RequestId().requestId(TestConstant.REQUEST_ID_1)));
          assertThat(strArgCaptor.getAllValues())
              .isEqualTo(
                  List.of(
                      TestConstant.SUBMISSION_ID,
                      TestConstant.S3_BUCKET,
                      TestConstant.URL,
                      TestConstant.REQUEST_ID_1));
        });

    verify(getClaimantEmailService).getEmailFromIdentityByUserId("test-user-id");
    verify(emailNotificationService)
        .sendAttachDocsEmailNotification(
            "test-email", "GB", "EN", "PIP2_INVITED", TestConstant.APPLICATION_ID);
  }

  @Test
  @DisplayName("test partition a submission into more than one drs upload requests")
  void testPartitionASubmissionIntoMoreThanOneDrsUploadRequests() {
    var submission = new PipApplicationV1();
    submission.setRegion(PipApplicationV1.RegionEnum.NI);
    submission.setClaimantId(TestConstant.CLAIMANT_ID_1);
    submission.setApplicationId(TestConstant.APPLICATION_ID);
    submission.setDrsMetadata(drsMetaFixture());

    var documentObjects = List.of(new S3RequestDocumentObject(), new S3RequestDocumentObject());
    submission.setDocuments(documentObjects);

    var reqDocObjOne = new S3RequestDocumentObject();
    reqDocObjOne.setBucket(TestConstant.S3_BUCKET);
    reqDocObjOne.setContentType("application/pdf");
    reqDocObjOne.setS3Ref(TestConstant.URL);
    reqDocObjOne.setSize(1024);
    reqDocObjOne.setName(TestConstant.FILE_NAME);
    reqDocObjOne.setDrsDocType(S3RequestDocumentObject.DrsDocTypeEnum._1241);
    reqDocObjOne.setDateTime(LocalDateTime.of(2021, 1, 1, 1, 10, 10));

    var reqDocObjTwo = new S3RequestDocumentObject();
    reqDocObjTwo.setBucket(TestConstant.S3_BUCKET);
    reqDocObjTwo.setContentType("application/pdf");
    reqDocObjTwo.setS3Ref(TestConstant.URL);
    reqDocObjTwo.setSize(1024);
    reqDocObjTwo.setName(TestConstant.FILE_NAME);
    reqDocObjTwo.setDrsDocType(S3RequestDocumentObject.DrsDocTypeEnum._1241);
    reqDocObjTwo.setDateTime(LocalDateTime.of(2021, 1, 1, 1, 10, 10));

    var appMeta = new ApplicationMeta();
    appMeta.setStartDate(LocalDate.of(2021, 1, 1));
    appMeta.setCompletedDate(LocalDate.of(2021, 1, 2));
    submission.setApplicationMeta(appMeta);

    when(dataService.findSubmissionByClaimantIdAndClaimId(anyString(), anyString()))
        .thenReturn(null);

    var firstBatch = mock(Batch.class);
    var secondBatch = mock(Batch.class);
    when(firstBatch.getBatch()).thenReturn(List.of(reqDocObjOne));
    when(secondBatch.getBatch()).thenReturn(List.of(reqDocObjTwo));
    when(partitionUtil.partition(any())).thenReturn(List.of(firstBatch, secondBatch));

    var savedSubmission = mock(Submission.class);
    when(savedSubmission.getId()).thenReturn(TestConstant.SUBMISSION_ID);
    when(dataService.createUpdateSubmission(any(Submission.class)))
        .thenReturn(savedSubmission)
        .thenReturn(savedSubmission);
    var savedDrsUpload = mock(DrsUpload.class);
    var documentation = new Documentation();
    documentation.setId(TestConstant.DOC_ID_1);
    when(savedDrsUpload.getId()).thenReturn(TestConstant.REQUEST_ID_1);

    var drsRequestAudit = new DrsUpload();
    drsRequestAudit.setId(TestConstant.REQUEST_ID_2);
    when(dataService.createUpdateDrsRequestAudit(any(DrsUpload.class)))
        .thenReturn(savedDrsUpload)
        .thenReturn(drsRequestAudit);
    var businessUnit = new DrsBusinessUnit();
    businessUnit.setCorrelationId("test-coll-id");
    businessUnit.setCallerId("test-caller-id");
    when(drsMetaProperties.findBusinessUnitByRegionCode(anyString()))
        .thenReturn(businessUnit)
        .thenReturn(businessUnit);

    // attach partitioned drs request
    when(dataService.findSubmissionById(anyString())).thenReturn(savedSubmission);
    var documentation2 = new Documentation();
    documentation.setId(TestConstant.DOC_ID_2);
    when(dataService.createUpdateDocumentation(any(Documentation.class)))
        .thenReturn(documentation2);

    var actual = cut.createNewSubmission(submission);
    assertAll(
        "assert submission response",
        () -> {
          assertThat(actual.getSubmissionId()).isEqualTo(TestConstant.SUBMISSION_ID);
          assertThat(actual.getDrsRequestIds()).hasSize(2);
          var ids =
              actual.getDrsRequestIds().stream()
                  .map(RequestId::getRequestId)
                  .collect(Collectors.toList());
          assertThat(ids).containsSequence(TestConstant.REQUEST_ID_1, TestConstant.REQUEST_ID_2);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"PUBLISHED", "FAIL", "RECEIVED"})
  @DisplayName("test should return ResubmitResponseObject provided with ResubmitDrsRequestObject ")
  void testShouldReturnResubmitResponseObjectProvidedWithResubmitDrsRequestObject(String status) {
    var drsRequestObject = new ResubmitDrsRequestObjectV1();
    var drsMetaData = new DrsMetadata();
    drsMetaData.setForename("Johnz");
    drsMetaData.setSurname("Smithz");
    drsMetaData.setPostcode("LS1 1XX");
    drsMetaData.setNino("AA370773A");
    drsMetaData.setDob(LocalDate.of(2000, 1, 1));
    drsRequestObject.setDrsMetadata(drsMetaData);
    var failedRequestId = new RequestId();
    failedRequestId.setRequestId("failed-request-id");
    drsRequestObject.setDrsRequestIds(List.of(failedRequestId));
    drsRequestObject.setRegion(ResubmitDrsRequestObjectV1.RegionEnum.GB);

    var drsRequestAudit = new DrsUpload();
    drsRequestAudit.setId("failed-request-id");
    drsRequestAudit.setStatus(status);
    drsRequestAudit.setSubmissionId("failed-request-submission-id");
    var doc = new DocumentId();
    doc.setDocumentId("doc-id");
    drsRequestAudit.setDocumentIdIds(List.of(doc));
    var retryDrsRequestAudit = new DrsUpload();
    retryDrsRequestAudit.setId("retry-request-id");
    retryDrsRequestAudit.setSubmissionId("failed-request-submission-id");
    var documentation = new Documentation();
    var storage = new Storage();
    storage.setUrl("doc-s3-url");
    documentation.setId("doc-id");
    documentation.setDocumentType("doc-type");
    documentation.setFilename("doc-name");
    documentation.setTimestamp(LocalDateTime.of(2021, 2, 4, 10, 10, 10));
    documentation.setStorage(List.of(storage));

    var drsBusinessUnit = new DrsBusinessUnit();
    drsBusinessUnit.setCallerId("gb-caller-id");

    when(drsMetaProperties.findBusinessUnitByRegionCode("GB")).thenReturn(drsBusinessUnit);
    when(dataService.findDrsRequestByRequestId(anyString()))
        .thenReturn(drsRequestAudit, drsRequestAudit, retryDrsRequestAudit);
    when(dataService.createUpdateDrsRequestAudit(any(DrsUpload.class)))
        .thenReturn(retryDrsRequestAudit);
    when(dataService.findDocumentById(anyString())).thenReturn(documentation);

    var actual = cut.resubmit(drsRequestObject);
    assertThat(actual).isNotNull().isExactlyInstanceOf(ResubmitResponseObject.class);
    var resubmission = actual.getResubmits().get(0);
    verify(publisher).publishEvent(drsUploadReqArgCaptor.capture());
    var capturedDrsUploadRequest = drsUploadReqArgCaptor.getValue();

    assertThat(capturedDrsUploadRequest.getRequestId()).isEqualTo("retry-request-id");
    assertThat(capturedDrsUploadRequest.getMetas()).hasSize(1);

    var capturedDrsMeta = capturedDrsUploadRequest.getMetas().get(0);
    var capturedDocument = capturedDrsMeta.getDocumentList().get(0);
    assertAll(
        "assert all drs meta data equals",
        () -> {
          assertThat(capturedDrsMeta.getNinoBody()).isEqualTo("AA370773");
          assertThat(capturedDrsMeta.getNinoSuffix()).isEqualTo("A");
          assertThat(capturedDrsMeta.getForename()).isEqualTo("Johnz");
          assertThat(capturedDrsMeta.getSurname()).isEqualTo("Smithz");
          assertThat(capturedDrsMeta.getPostcode()).isEqualTo("LS1 1XX");
          assertThat(capturedDocument.getType()).isEqualTo("doc-type");
          assertThat(capturedDocument.getUrl()).isEqualTo("doc-s3-url");
          assertThat(capturedDocument.getComment()).isEqualTo("doc-name");
        });
    assertThat(resubmission.getFailedDrsRequestId()).isEqualTo("failed-request-id");
    assertThat(resubmission.getRetryDrsRequestId()).isEqualTo("retry-request-id");
    assertThat(actual.getResubmits()).hasSize(1);

    var actualSubmission = actual.getResubmits().get(0);
    assertThat(actualSubmission.getRetryDrsRequestId()).isEqualTo("retry-request-id");
    assertThat(actualSubmission.getFailedDrsRequestId()).isEqualTo("failed-request-id");

    verify(dataService, times(3)).createUpdateDrsRequestAudit(auditArgumentCaptor.capture());
    var capturedDrsRequestAudits = auditArgumentCaptor.getAllValues();
    assertAll(
        "assert all captured drs request audits",
        () -> {
          var retryDrsRequestReceived = capturedDrsRequestAudits.get(0);
          assertThat(retryDrsRequestReceived.getStatus()).isEqualTo(DrsStatusEnum.RECEIVED.status);
          var retryDrsRequestPublished = capturedDrsRequestAudits.get(1);
          assertThat(retryDrsRequestPublished.getStatus())
              .isEqualTo(DrsStatusEnum.PUBLISHED.status);
          var failedDrsRequest = capturedDrsRequestAudits.get(2);
          assertThat(failedDrsRequest.getStatus()).isEqualTo(DrsStatusEnum.RESUBMITTED.status);
        });
    verify(dataService, times(3)).findDrsRequestByRequestId(strArgCaptor.capture());
    assertThat(strArgCaptor.getAllValues())
        .isEqualTo(List.of("failed-request-id", "retry-request-id", "failed-request-id"));
  }

  private DrsMetadata drsMetaFixture() {
    var drsMeta = new DrsMetadata();
    drsMeta.setDob(LocalDate.now().minusYears(20));
    drsMeta.setSurname("test-surname");
    drsMeta.setForename("test-forename");
    drsMeta.setPostcode("LS1 1XX");
    drsMeta.setNino("AA370773A");
    return drsMeta;
  }
}
