package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.DrsMetaProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.EventConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.constant.DrsDocumentTypeEnum;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.event.request.Document;
import uk.gov.dwp.health.pip.document.submission.manager.event.request.DrsUploadRequest;
import uk.gov.dwp.health.pip.document.submission.manager.event.request.PipDrsMeta;
import uk.gov.dwp.health.pip.document.submission.manager.exception.AttachAdditionalDocumentException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.CreateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DrsRequestNotFoundException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.SubmissionNotFoundException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.TaskException;
import uk.gov.dwp.health.pip.document.submission.manager.model.DrsStatusEnum;
import uk.gov.dwp.health.pip.document.submission.manager.model.Record;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.AttachDocumentResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RequestId;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.Resubmission;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.DataServiceImpl;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.EventPublisherImpl;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.S3UrlResolverImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.Region.RegionEnum.GB;

@Slf4j
@AllArgsConstructor
public class SubmissionServiceAbstract {

  private final EventPublisherImpl publisher;
  private final DrsMetaProperties drsMetaProperties;
  private final EventConfigProperties eventConfigProperties;
  private final DataServiceImpl dataService;
  private final S3UrlResolverImpl s3UrlResolver;

  protected boolean submissionExist(final String claimantId, String claimId) {
    return Objects.nonNull(dataService.findSubmissionByClaimantIdAndClaimId(claimantId, claimId));
  }

  protected SubmissionResponseObjectV1 createSubmission(
      String claimantId,
      String applicationId,
      DrsMetadata meta,
      LocalDate startDate,
      LocalDate completeDate,
      String region,
      List<S3RequestDocumentObject> documents) {
    try {
      var record = processDocuments(claimantId, applicationId, documents);
      var documentIdRefs = saveDocumentInMongo(record.getMongoDocuments());
      var submissionId =
          saveSubmissionInMongo(claimantId, applicationId, documentIdRefs, startDate, completeDate);
      var requestId = createDrsAuditTrailInMongo(submissionId, documentIdRefs);

      publishDrsEvent(
          prepareDrsUploadRequest(
              requestId,
              record.getDrsDocuments(),
              mapDrsMetaToPipDrsMeta(meta),
              region == null ? GB.name() : region));
      log.info(
          "DRS event published to ms-document for submission [{}] with request audit [{}]",
          submissionId,
          requestId);

      findUpdateDrsAuditStatus(requestId, DrsStatusEnum.PUBLISHED);

      var resp = new SubmissionResponseObjectV1();
      resp.setSubmissionId(submissionId);
      resp.setDrsRequestIds(List.of(new RequestId().requestId(requestId)));
      return resp;

    } catch (TaskException ex) {
      log.error("Error while submitting {}", ex.getMessage());
      throw new CreateSubmissionException(ex.getMessage());
    }
  }

  protected AttachDocumentResponseObjectV1 attachToExisting(
      String submissionId,
      List<S3RequestDocumentObject> documents,
      DrsMetadata metadata,
      String region) {
    log.info("About to attach to existing submission id {}", submissionId);

    var mongoSubmission = getSubmissionById(submissionId);

    try {
      var record =
          processDocuments(
              mongoSubmission.getClaimantId(), mongoSubmission.getApplicationId(), documents);
      var documentIdRefs = saveDocumentInMongo(record.getMongoDocuments());
      attachDocumentToSubmission(mongoSubmission, documentIdRefs);
      var requestId = createDrsAuditTrailInMongo(submissionId, documentIdRefs);
      var drsUploadRequest =
          prepareDrsUploadRequest(
              requestId, record.getDrsDocuments(), mapDrsMetaToPipDrsMeta(metadata), region);
      log.info("About to publish drs event for submission id {}", submissionId);
      publishDrsEvent(drsUploadRequest);
      log.info("Published drs event for submission id {}", submissionId);
      findUpdateDrsAuditStatus(requestId, DrsStatusEnum.PUBLISHED);

      var response = new AttachDocumentResponseObjectV1();
      response.setDrsRequestIds(List.of(new RequestId().requestId(requestId)));
      log.info("Attached to existing submission id {}", submissionId);

      return response;
    } catch (TaskException ex) {
      log.error("Error submitting the attachment {}", ex.getMessage());
      throw new AttachAdditionalDocumentException(ex.getMessage());
    }
  }

  protected Submission getSubmissionById(final String submissionId) {
    var mongoSubmission = dataService.findSubmissionById(submissionId);

    if (mongoSubmission == null) {
      final var msg = String.format("Submission [%s] not found", submissionId);
      log.info(msg);
      throw new SubmissionNotFoundException(msg);
    }
    return mongoSubmission;
  }

  protected ResubmitResponseObject resubmitResponseObject(
      DrsMetadata drsMetadata, List<RequestId> requestIds, String region) {

    var responseObject = new ResubmitResponseObject();
    responseObject.setResubmits(new ArrayList<>());
    requestIds.forEach(
        failedRequest -> {
          log.info("Resubmit existing [{}] to region [{}]", failedRequest.getRequestId(), region);
          var requestId = failedRequest.getRequestId();
          var failedDrsRequest = dataService.findDrsRequestByRequestId(requestId);
          if (failedDrsRequest == null) {
            throw new DrsRequestNotFoundException(
                String.format("Failed DRS request audit does not exist - %s", requestId));
          }

          var resubmission = new Resubmission();
          if (failedDrsRequest.getStatus().equals(DrsStatusEnum.PUBLISHED.status)
              || failedDrsRequest.getStatus().equals(DrsStatusEnum.FAIL.status)
              || failedDrsRequest.getStatus().equals(DrsStatusEnum.RECEIVED.status)) {
            var submissionId = failedDrsRequest.getSubmissionId();
            var retryDrsId =
                createDrsAuditTrailInMongo(submissionId, failedDrsRequest.getDocumentIdIds());

            var drsUploadRequest =
                prepareDrsUploadRequest(
                    retryDrsId,
                    failedDrsRequest.getDocumentIdIds().stream()
                        .map(this::transformToDocumentForResubmission)
                        .collect(Collectors.toList()),
                    mapDrsMetaToPipDrsMeta(drsMetadata),
                    region);
            publishDrsEvent(drsUploadRequest);
            findUpdateDrsAuditStatus(retryDrsId, DrsStatusEnum.PUBLISHED);
            findUpdateDrsAuditStatus(requestId, DrsStatusEnum.RESUBMITTED);
            resubmission.setFailedDrsRequestId(requestId);
            resubmission.setRetryDrsRequestId(retryDrsId);
          }
          responseObject.getResubmits().add(resubmission);
        });
    return responseObject;
  }

  List<DocumentId> saveDocumentInMongo(List<Documentation> documentations) {
    return documentations.stream()
        .map(
            d ->
                DocumentId.builder()
                    .documentId(dataService.createUpdateDocumentation(d).getId())
                    .build())
        .collect(Collectors.toList());
  }

  String saveSubmissionInMongo(
      String claimantId,
      String applicationId,
      List<DocumentId> documentIdList,
      LocalDate applicationStartDate,
      LocalDate applicationCompleteDate) {

    var submission =
        Submission.builder()
            .documentIdIds(documentIdList)
            .started(applicationStartDate)
            .completed(applicationCompleteDate)
            .claimantId(claimantId)
            .applicationId(applicationId)
            .build();

    return dataService.createUpdateSubmission(submission).getId();
  }

  String createDrsAuditTrailInMongo(String submissionId, List<DocumentId> documentIds) {
    var drsUpload =
        DrsUpload.builder()
            .submissionId(submissionId)
            .documentIdIds(documentIds)
            .status(DrsStatusEnum.RECEIVED.status)
            .submittedAt(LocalDateTime.now())
            .build();
    return dataService.createUpdateDrsRequestAudit(drsUpload).getId();
  }

  void publishDrsEvent(DrsUploadRequest request) {
    publisher.publishEvent(request);
  }

  PipDrsMeta mapDrsMetaToPipDrsMeta(DrsMetadata drsMetadata) {
    return PipDrsMeta.builder()
        .surname(drsMetadata.getSurname())
        .forename(drsMetadata.getForename())
        .dob(drsMetadata.getDob().toString())
        .ninoBody(drsMetadata.getNino().substring(0, 8))
        .ninoSuffix(drsMetadata.getNino().substring(drsMetadata.getNino().length() - 1))
        .postcode(drsMetadata.getPostcode())
        .build();
  }

  Document transformToDocumentForResubmission(DocumentId documentId) {
    var documentation = dataService.findDocumentById(documentId.getDocumentId());
    return Document.builder()
        .comment(documentation.getFilename())
        .date(documentation.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME))
        .url(documentation.getStorage().get(0).getUrl())
        .type(documentation.getDocumentType())
        .build();
  }

  private Record processDocuments(
      String claimantId, String applicationId, List<S3RequestDocumentObject> documents) {

    var drsDocuments = new ArrayList<Document>();
    var mongoDocuments = new ArrayList<Documentation>();

    documents.forEach(
        doc -> {
          var s3ref = doc.getS3Ref();
          var url = s3UrlResolver.resolve(doc.getBucket(), s3ref);
          var drsDocType = doc.getDrsDocType().getValue();
          var dateTime = doc.getDateTime();
          drsDocuments.add(
              Document.builder()
                  .date(dateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                  .type(drsDocType)
                  .comment(doc.getName())
                  .url(url)
                  .build());
          mongoDocuments.add(
              Documentation.builder()
                  .applicationId(applicationId)
                  .claimantId(claimantId)
                  .filename(doc.getName())
                  .documentType(DrsDocumentTypeEnum.get(drsDocType).identifier())
                  .sizeKb(doc.getSize())
                  .timestamp(dateTime)
                  .storage(
                      Collections.singletonList(
                          Storage.builder().type("S3").uniqueId(s3ref).url(url).build()))
                  .build());
        });
    return Record.builder().mongoDocuments(mongoDocuments).drsDocuments(drsDocuments).build();
  }

  private DrsUploadRequest prepareDrsUploadRequest(
      String requestId, List<Document> documents, PipDrsMeta meta, String region) {

    meta.setDocumentList(documents);
    var businessUnit =
        drsMetaProperties.findBusinessUnitByRegionCode(region == null ? GB.name() : region);

    return DrsUploadRequest.builder()
        .callerId(businessUnit.getCallerId())
        .correlationId(Optional.ofNullable(businessUnit.getCorrelationId()).orElse(""))
        .requestId(requestId)
        .responseRoutingKey(eventConfigProperties.getIncomingRoutingKey())
        .metas(Collections.singletonList(meta))
        .build();
  }

  private void findUpdateDrsAuditStatus(String requestId, DrsStatusEnum statusEnum) {
    Optional.ofNullable(dataService.findDrsRequestByRequestId(requestId))
        .ifPresentOrElse(
            request -> {
              request.setStatus(statusEnum.status);
              request.setCompletedAt(LocalDateTime.now());
              dataService.createUpdateDrsRequestAudit(request);
            },
            () ->
                log.warn(
                    "REQUEST {} NOT FOUND, UNABLE TO UPDATE STATUS TO {}",
                    requestId,
                    statusEnum.status));
  }

  private void attachDocumentToSubmission(
      Submission mongoSubmission, List<DocumentId> mongoDocumentation) {
    log.info(
        "Attach [{}] documents to Submission [{}]",
        mongoDocumentation.size(),
        mongoSubmission.getId());
    if (mongoSubmission.getDocumentIdIds().addAll(mongoDocumentation)) {
      dataService.createUpdateSubmission(mongoSubmission);
    }
  }
}
