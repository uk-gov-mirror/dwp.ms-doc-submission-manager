package uk.gov.dwp.health.pip.document.submission.manager.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import support.TestConstant;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.DrsBusinessUnit;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.DrsMetaProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.EventConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.event.request.DrsUploadRequest;
import uk.gov.dwp.health.pip.document.submission.manager.exception.SubmissionNotFoundException;
import uk.gov.dwp.health.pip.document.submission.manager.model.DrsStatusEnum;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.DataServiceImpl;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.EventPublisherImpl;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.S3UrlResolverImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceAbstractTest {

  @InjectMocks private SubmissionServiceAbstract submissionServiceAbstract;
  @Mock private EventPublisherImpl publisher;
  @Mock private S3UrlResolverImpl s3UrlResolver;
  @Mock private DrsMetaProperties drsMetaProperties;
  @Mock private EventConfigProperties eventConfigProperties;
  @Mock private DataServiceImpl dataService;

  @Test
  @DisplayName("test submission exist")
  void testSubmissionExist() {
    when(dataService.findSubmissionByClaimantIdAndClaimId(anyString(), anyString()))
        .thenReturn(null);

    var actual =
        submissionServiceAbstract.submissionExist(
            TestConstant.CLAIMANT_ID_1, TestConstant.APPLICATION_ID);
    assertThat(actual).isFalse();

    var strArgCaptor = ArgumentCaptor.forClass(String.class);
    verify(dataService)
        .findSubmissionByClaimantIdAndClaimId(strArgCaptor.capture(), strArgCaptor.capture());
    assertThat(strArgCaptor.getAllValues())
        .isEqualTo(List.of(TestConstant.CLAIMANT_ID_1, TestConstant.APPLICATION_ID));
  }

  @Test
  @DisplayName("test create drs audit trail in mongodb")
  void testCreateDrsAuditTrailInMongodb() {
    var submissionId = testFixtureRandomID();
    var docs = List.of(new DocumentId());
    var drsId = testFixtureRandomID();
    var mockReturn = new DrsUpload();
    mockReturn.setId(drsId);
    when(dataService.createUpdateDrsRequestAudit(any(DrsUpload.class))).thenReturn(mockReturn);

    var actualDrsId = submissionServiceAbstract.createDrsAuditTrailInMongo(submissionId, docs);
    assertThat(actualDrsId).isEqualTo(drsId);

    var drsArgCaptor = ArgumentCaptor.forClass(DrsUpload.class);
    verify(dataService).createUpdateDrsRequestAudit(drsArgCaptor.capture());

    var captured = drsArgCaptor.getValue();
    assertAll(
        "assert DRS audit request",
        () -> {
          assertThat(captured.getSubmissionId()).isEqualTo(submissionId);
          assertThat(captured.getStatus()).isEqualTo(DrsStatusEnum.RECEIVED.status);
          assertThat(captured.getDocumentIdIds()).isEqualTo(docs);
        });
  }

  private String testFixtureRandomID() {
    return UUID.randomUUID().toString();
  }

  @Test
  @DisplayName("test save submission in database")
  void testSaveSubmissionInDatabase() {
    var claimantId = testFixtureRandomID();
    var claimId = testFixtureRandomID();
    var docs = List.of(new DocumentId());

    var startDate = LocalDate.of(2021, 1, 1);
    var endDate = LocalDate.of(2021, 2, 1);

    var mockSubmission = new Submission();
    var submissionId = testFixtureRandomID();
    mockSubmission.setId(submissionId);
    when(dataService.createUpdateSubmission(any(Submission.class))).thenReturn(mockSubmission);

    var actualSubmissionId =
        submissionServiceAbstract.saveSubmissionInMongo(
            claimantId, claimId, docs, startDate, endDate);
    var submissionArgCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(dataService).createUpdateSubmission(submissionArgCaptor.capture());

    var captured = submissionArgCaptor.getValue();
    assertAll(
        "assert submission",
        () -> {
          assertThat(captured.getStarted()).isEqualTo(startDate);
          assertThat(captured.getCompleted()).isEqualTo(endDate);
          assertThat(captured.getClaimantId()).isEqualTo(claimantId);
          assertThat(captured.getApplicationId()).isEqualTo(claimId);
          assertThat(captured.getDocumentIdIds()).isEqualTo(docs);
          assertThat(actualSubmissionId).isEqualTo(submissionId);
        });
  }

  @Test
  @DisplayName("test publish drs event")
  void testPublishDrsEvent() {
    var event = mock(DrsUploadRequest.class);
    submissionServiceAbstract.publishDrsEvent(event);
    verify(publisher).publishEvent(any(DrsUploadRequest.class));
  }

  @Test
  @DisplayName("test save document in mongo")
  void testSaveDocumentInMongo() {
    var d1 = new Documentation();
    var d1_Id = testFixtureRandomID();
    d1.setId(d1_Id);
    var d2 = new Documentation();
    var d2_Id = testFixtureRandomID();
    d2.setId(d2_Id);
    var documentations = List.of(d1, d2);
    when(dataService.createUpdateDocumentation(any(Documentation.class)))
        .thenReturn(d1)
        .thenReturn(d2);
    var actual = submissionServiceAbstract.saveDocumentInMongo(documentations);
    var argumentCaptor = ArgumentCaptor.forClass(Documentation.class);
    verify(dataService, times(2)).createUpdateDocumentation(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues()).isEqualTo(documentations);
    assertThat(actual).extracting("documentId", String.class).containsExactly(d1_Id, d2_Id);
  }

  @Test
  @DisplayName("test transform doc model to document entity")
  void testTransformDocModelToDocumentEntity() {
    var docId = testFixtureRandomID();
    var documentId = new DocumentId();
    documentId.setDocumentId(docId);
    var documentation = new Documentation();
    documentation.setFilename("mock_file_name");
    var localDateTime = LocalDateTime.of(2021, 1, 1, 1, 10, 10, 10);
    documentation.setTimestamp(localDateTime);
    var storage = new Storage();
    storage.setUrl("http://s3.aws");
    documentation.setStorage(List.of(storage));
    documentation.setDocumentType("1407");
    when(dataService.findDocumentById(anyString())).thenReturn(documentation);

    var actual = submissionServiceAbstract.transformToDocumentForResubmission(documentId);

    assertAll(
        "assert document",
        () -> {
          assertThat(actual.getComment()).isEqualTo("mock_file_name");
          assertThat(actual.getUrl()).isEqualTo("http://s3.aws");
          assertThat(actual.getType()).isEqualTo("1407");
          assertThat(actual.getDate()).isEqualTo("2021-01-01T01:10:10.00000001");
        });
    var argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(dataService).findDocumentById(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(docId);
  }

  @Test
  @DisplayName("map client drs metadata to ms-doc drs meta dto")
  void mapClientDrsMetadataToMsDocDrsMetaDto() {
    var postCode = "LS1 1XX";
    var surname = "surname";
    var forename = "forename";
    var nino = "AA370773A";
    var dob = LocalDate.of(1990, 1, 1);
    var from = new DrsMetadata();
    from.setPostcode(postCode);
    from.setForename(forename);
    from.setSurname(surname);
    from.setNino(nino);
    from.setDob(dob);

    var actual = submissionServiceAbstract.mapDrsMetaToPipDrsMeta(from);
    assertAll(
        "assert all pip drs meta",
        () -> {
          assertThat(actual.getNinoBody()).isEqualTo("AA370773");
          assertThat(actual.getNinoSuffix()).isEqualTo("A");
          assertThat(actual.getSurname()).isEqualTo(surname);
          assertThat(actual.getForename()).isEqualTo(forename);
          assertThat(actual.getDob()).isEqualTo("1990-01-01");
          assertThat(actual.getPostcode()).isEqualTo(postCode);
        });
  }

  @Test
  @DisplayName("test attach to existing submission returns AttachDocumentResponseObject")
  void testAttachToExistingSubmissionReturnsAttachDocumentResponseObject() {
    var submissionId = testFixtureRandomID();
    var region = "GB";
    var existingSubmission = new Submission();
    var claimantId = testFixtureRandomID();
    var claimId = testFixtureRandomID();
    var s3Doc = new S3RequestDocumentObject();

    s3Doc.setBucket(TestConstant.S3_BUCKET);
    s3Doc.setContentType(TestConstant.FILE_CONTENT);
    s3Doc.setSize(1024);
    s3Doc.setS3Ref(TestConstant.URL);
    s3Doc.setDrsDocType(S3RequestDocumentObject.DrsDocTypeEnum._1241);
    s3Doc.setName(TestConstant.FILE_NAME);
    s3Doc.setDateTime(LocalDateTime.of(2021, 1, 1, 10, 10, 10));
    var documents = List.of(s3Doc);

    var drsMeta = new DrsMetadata();
    var surname = "surname";
    var forename = "forename";
    var nino = "AA370773A";
    var postcode = "LS1 1XX";
    var dob = LocalDate.of(1990, 1, 1);
    drsMeta.setSurname(surname);
    drsMeta.setForename(forename);
    drsMeta.setNino(nino);
    drsMeta.setPostcode(postcode);
    drsMeta.setDob(dob);

    existingSubmission.setId(submissionId);
    existingSubmission.setClaimantId(claimantId);
    existingSubmission.setApplicationId(claimId);
    existingSubmission.setDocumentIdIds(new ArrayList<>());
    when(dataService.findSubmissionById(anyString())).thenReturn(existingSubmission);
    when(s3UrlResolver.resolve(anyString(), anyString())).thenReturn("http://aws.com");
    var savedDocumentation = new Documentation();
    var documentationId = testFixtureRandomID();
    savedDocumentation.setId(documentationId);
    when(dataService.createUpdateDocumentation(any(Documentation.class)))
        .thenReturn(savedDocumentation);
    var savedDrsRequestAudit = new DrsUpload();
    var drsRequestId = testFixtureRandomID();
    savedDrsRequestAudit.setId(drsRequestId);
    when(dataService.createUpdateDrsRequestAudit(any(DrsUpload.class)))
        .thenReturn(savedDrsRequestAudit);
    var busUnit = new DrsBusinessUnit();
    busUnit.setCallerId("pip-online");
    when(drsMetaProperties.findBusinessUnitByRegionCode(anyString())).thenReturn(busUnit);
    when(eventConfigProperties.getIncomingRoutingKey()).thenReturn("routing-key");
    submissionServiceAbstract.attachToExisting(submissionId, documents, drsMeta, region);

    var order =
        Mockito.inOrder(
            dataService, s3UrlResolver, drsMetaProperties, eventConfigProperties, publisher);
    var strArgCaptor = ArgumentCaptor.forClass(String.class);
    order.verify(dataService).findSubmissionById(strArgCaptor.capture());
    order.verify(s3UrlResolver).resolve(strArgCaptor.capture(), strArgCaptor.capture());

    var documentationCapt = ArgumentCaptor.forClass(Documentation.class);
    order.verify(dataService).createUpdateDocumentation(documentationCapt.capture());

    var reqAuditArgCaptor = ArgumentCaptor.forClass(DrsUpload.class);
    order.verify(dataService).createUpdateDrsRequestAudit(reqAuditArgCaptor.capture());
    order.verify(drsMetaProperties).findBusinessUnitByRegionCode(strArgCaptor.capture());

    var uploadReqArgCaptor = ArgumentCaptor.forClass(DrsUploadRequest.class);
    order.verify(publisher).publishEvent(uploadReqArgCaptor.capture());
    order.verify(dataService).findDrsRequestByRequestId(strArgCaptor.capture());
    assertAll(
        "assert attach document captors",
        () -> {
          assertThat(strArgCaptor.getAllValues())
              .isEqualTo(
                  List.of(
                      submissionId, TestConstant.S3_BUCKET, TestConstant.URL, "GB", drsRequestId));
          var doc = documentationCapt.getValue();
          assertThat(doc.getDocumentType()).isEqualTo("1241");
          assertThat(doc.getSizeKb()).isEqualTo(1024);
          assertThat(doc.getClaimantId()).isEqualTo(claimantId);
          assertThat(doc.getApplicationId()).isEqualTo(claimId);
          assertThat(doc.getFilename()).isEqualTo(TestConstant.FILE_NAME);
          assertThat(doc.getTimestamp()).isEqualTo(LocalDateTime.of(2021, 1, 1, 10, 10, 10));
          var drs = reqAuditArgCaptor.getValue();
          assertThat(drs.getSubmissionId()).isEqualTo(submissionId);
          assertThat(drs.getStatus()).isEqualTo("RECEIVED");
          assertThat(drs.getDocumentIdIds().get(0).getDocumentId()).isEqualTo(documentationId);
          var upload = uploadReqArgCaptor.getValue();
          assertThat(upload.getCallerId()).isEqualTo("pip-online");
          assertThat(upload.getRequestId()).isEqualTo(drsRequestId);
          assertThat(upload.getResponseRoutingKey()).isEqualTo("routing-key");
          var meta = upload.getMetas().get(0);
          assertThat(meta.getSurname()).isEqualTo(surname);
          assertThat(meta.getForename()).isEqualTo(forename);
          assertThat(meta.getNinoBody()).isEqualTo("AA370773");
          assertThat(meta.getNinoSuffix()).isEqualTo("A");
          assertThat(meta.getDob()).isEqualTo("1990-01-01");
          assertThat(meta.getPostcode()).isEqualTo("LS1 1XX");
        });
  }

  @Test
  @DisplayName("test attach to existing submission throws submission not found exception ")
  void testAttachToExistingSubmissionThrowsSubmissionNotFoundException() {
    var submissionId = testFixtureRandomID();
    var documents = List.of(new S3RequestDocumentObject());
    var drsMeta = new DrsMetadata();
    var region = "GB";
    when(dataService.findSubmissionById(anyString())).thenReturn(null);
    assertThrows(
        SubmissionNotFoundException.class,
        () -> submissionServiceAbstract.attachToExisting(submissionId, documents, drsMeta, region));
  }
}
