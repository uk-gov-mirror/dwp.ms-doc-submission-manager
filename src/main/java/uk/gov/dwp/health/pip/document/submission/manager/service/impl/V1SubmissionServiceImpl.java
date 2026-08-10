package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.DrsMetaProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.EventConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.LanguageEnum;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DuplicateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.AttachDocumentResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RequestId;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitDrsRequestObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ResubmitResponseObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionAttachObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.service.ApplicationCoordinatorService;
import uk.gov.dwp.health.pip.document.submission.manager.service.ResubmissionService;
import uk.gov.dwp.health.pip.document.submission.manager.service.SubmissionService;
import uk.gov.dwp.health.pip.document.submission.manager.service.SubmissionServiceAbstract;
import uk.gov.dwp.health.pip.document.submission.manager.service.SubmissionSupplementaryService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.Batch;
import uk.gov.dwp.health.pip.document.submission.manager.utils.RequestPartition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class V1SubmissionServiceImpl extends SubmissionServiceAbstract
    implements SubmissionService<PipApplicationV1, SubmissionResponseObjectV1>,
        SubmissionSupplementaryService<SubmissionAttachObjectV1, AttachDocumentResponseObjectV1>,
        ResubmissionService<ResubmitDrsRequestObjectV1, ResubmitResponseObject> {

  private final RequestPartition partitionUtil;
  private final ApplicationCoordinatorService applicationCoordinatorService;
  private final GetClaimantEmailService getClaimantEmailService;
  private final EmailNotificationServiceImpl emailNotificationService;

  public V1SubmissionServiceImpl(
      EventPublisherImpl publisher,
      S3UrlResolverImpl s3UrlResolver,
      DataServiceImpl dataService,
      DrsMetaProperties properties,
      EventConfigProperties eventConfigProperties,
      RequestPartition partition,
      GetClaimantEmailService getClaimantEmailService,
      ApplicationCoordinatorService applicationCoordinatorService,
      EmailNotificationServiceImpl emailNotificationService) {
    super(publisher, properties, eventConfigProperties, dataService, s3UrlResolver);
    this.partitionUtil = partition;
    this.getClaimantEmailService = getClaimantEmailService;
    this.applicationCoordinatorService = applicationCoordinatorService;
    this.emailNotificationService = emailNotificationService;
  }

  @Override
  public SubmissionResponseObjectV1 createNewSubmission(PipApplicationV1 submission) {
    final var applicationId = submission.getApplicationId();
    if (submissionExist(submission.getClaimantId(), applicationId)) {
      final String message =
          String.format(
              "Submission already exist for claimant [%s] and claim [%s]",
              submission.getClaimantId(), applicationId);
      log.info(message);
      throw new DuplicateSubmissionException(message);
    }
    var subRespObjV2 = new SubmissionResponseObjectV1();
    List<RequestId> requestIds = new ArrayList<>();
    List<Batch> batches = partitionUtil.partition(submission.getDocuments());
    log.info(
        "Initial upload file count [{}] to be partition in [{}]",
        submission.getDocuments().size(),
        batches.size());
    var submissionId = new AtomicReference<String>();
    final DrsMetadata drsMeta = submission.getDrsMetadata();
    IntStream.rangeClosed(0, batches.size() - 1)
        .forEach(
            idx -> {
              if (idx == 0) {
                log.info(
                    "Add [{}] a batch:  FileCount [{}] diskVol [{}] to existing submission",
                    idx,
                    batches.get(idx).getBatch().size(),
                    batches.get(idx).currentVolume());
                var resp =
                    createSubmission(
                        submission.getClaimantId(),
                        submission.getApplicationId(),
                        drsMeta,
                        submission.getApplicationMeta().getStartDate(),
                        submission.getApplicationMeta().getCompletedDate(),
                        submission.getRegion().getValue(),
                        batches.get(idx).getBatch());
                subRespObjV2.setSubmissionId(resp.getSubmissionId());
                requestIds.addAll(resp.getDrsRequestIds());
                submissionId.set(resp.getSubmissionId());
              } else {
                log.info(
                    "Add [{}] batch [{}] to existing submission as further evidence",
                    idx,
                    batches.get(idx).getBatch().size());
                var resp =
                    attachToExisting(
                        Objects.requireNonNull(submissionId).get(),
                        batches.get(idx).getBatch(),
                        drsMeta,
                        submission.getRegion().getValue());
                requestIds.addAll(resp.getDrsRequestIds());
              }
            });
    subRespObjV2.setDrsRequestIds(requestIds);
    return subRespObjV2;
  }

  @Override
  public AttachDocumentResponseObjectV1 attachDocumentToExistingSubmission(
      String userId, SubmissionAttachObjectV1 attachObjectV2) {
    final List<Batch> batches = partitionUtil.partition(attachObjectV2.getDocuments());
    log.info(
        "Further evidence upload file count [{}] to be partition in [{}]",
        attachObjectV2.getDocuments().size(),
        batches.size());
    final List<RequestId> collect =
        batches.stream()
            .map(
                batch -> {
                  var resp =
                      attachToExisting(
                          attachObjectV2.getSubmissionId(),
                          batch.getBatch(),
                          attachObjectV2.getDrsMetadata(),
                          attachObjectV2.getRegion().getValue());
                  return resp.getDrsRequestIds().get(0);
                })
            .collect(Collectors.toList());

    sendEmailNotification(userId, attachObjectV2);

    var resp = new AttachDocumentResponseObjectV1();
    resp.setDrsRequestIds(collect);
    return resp;
  }

  private void sendEmailNotification(String userId, SubmissionAttachObjectV1 attachObjectV1) {
    log.info("Send email notification for submission id [{}]", attachObjectV1.getSubmissionId());

    final Submission submission = getSubmissionById(attachObjectV1.getSubmissionId());
    final String applicationId = submission.getApplicationId();
    String email;
    // Default to region in request from account manager.
    String region = attachObjectV1.getRegion().getValue();
    // Default to Strategic application.
    String journeyType = UserJourneyEnum.STRATEGIC.getValue();
    // Default to EN.
    String language = LanguageEnum.EN.getValue();

    final ResultWrapper<ApplicationDetails> applicationCoordinatorResult =
        getCoordinatorApplicationDetails(applicationId);

    if (applicationCoordinatorResult.isSuccess()) {
      final ApplicationDetails applicationDetails = applicationCoordinatorResult.getValue();
      region =
          Optional.ofNullable(applicationDetails.getRegion())
              .map(ApplicationDetails.RegionEnum::getValue)
              .orElse(region);
      journeyType =
          Optional.ofNullable(applicationDetails.getJourneyType())
              .map(ApplicationDetails.JourneyTypeEnum::getValue)
              .orElse(journeyType);
      language =
          Optional.ofNullable(applicationDetails.getLanguage())
              .map(ApplicationDetails.LanguageEnum::getValue)
              .orElse(language);

      email = getEmailFromIdentity(userId);

      emailNotificationService.sendAttachDocsEmailNotification(
          email, region, language, journeyType, applicationId);

    } else {
      throw new RuntimeException(
          "Failed to get application details from application coordinator for application id "
              + applicationId);
    }
  }

  private ResultWrapper<ApplicationDetails> getCoordinatorApplicationDetails(String applicationId) {
    ResultWrapper<ApplicationDetails> applicationCoordinatorResult;
    try {
      applicationCoordinatorResult = applicationCoordinatorService.getApplication(applicationId);
    } catch (JacksonException exception) {
      throw new RuntimeException("Failed to get application details from application coordinator");
    }

    return applicationCoordinatorResult;
  }

  private String getEmailFromIdentity(String userId) {
    try {
      return getClaimantEmailService.getEmailFromIdentityByUserId(userId);
    } catch (JacksonException e) {
      throw new RuntimeException("Failed to get email from identity service for user id " + userId);
    }
  }

  @Override
  public ResubmitResponseObject resubmit(ResubmitDrsRequestObjectV1 resubmitDrsReqObjV2) {
    return resubmitResponseObject(
        resubmitDrsReqObjV2.getDrsMetadata(),
        resubmitDrsReqObjV2.getDrsRequestIds(),
        resubmitDrsReqObjV2.getRegion().getValue());
  }
}
