package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum.PIP2_INVITED;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.NotifyConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.LanguageEnum;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.RegionEnum;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum;
import uk.gov.dwp.health.pip.document.submission.manager.service.EmailService;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl {

  private final NotifyConfigProperties notifyConfigProperties;
  private final EmailService emailService;

  public void sendSubmissionEmailNotification(String email, RegionEnum region,
      UserJourneyEnum journeyType, String applicationId, LanguageEnum language,
      boolean isPipCredsUser) {
    String apiKey = getApiKey(region);
    String emailTemplateId = getSubmissionEmailTemplateId(region, journeyType, language,
        isPipCredsUser);

    emailService.sendEmail(apiKey, email, emailTemplateId, applicationId);
  }

  private String getApiKey(RegionEnum region) {
    if (region.equals(RegionEnum.NI)) {
      return notifyConfigProperties.getNiApiKey();
    } else {
      return notifyConfigProperties.getGbApiKey();
    }
  }

  private String getSubmissionEmailTemplateId(RegionEnum region, UserJourneyEnum journeyType,
      LanguageEnum language, boolean sendLegacyEmail) {
    if (journeyType.equals(PIP2_INVITED)) {
      if (sendLegacyEmail) {
        return region.equals(RegionEnum.NI)
            ? notifyConfigProperties.getLegacyNiInvitedSubmissionSuccessEmailTemplateId()
            : notifyConfigProperties.getLegacyEnglishGbInvitedSubmissionSuccessEmailTemplateId();
      }

      if (region.equals(RegionEnum.NI)) {
        return notifyConfigProperties.getNiInvitedSubmissionSuccessEmailTemplateId();
      } else {
        return language == LanguageEnum.CY
            ? notifyConfigProperties.getWelshGbInvitedSubmissionSuccessEmailTemplateId()
            : notifyConfigProperties.getEnglishGbInvitedSubmissionSuccessEmailTemplateId();
      }
    } else {
      return notifyConfigProperties.getGbSubmissionSuccessEmailTemplateId();
    }

  }

  public void sendAttachDocsEmailNotification(
      String email, String region, String language, String journeyType, String applicationId) {
    String apiKey = getApiKey(RegionEnum.valueOf(region));
    String emailTemplateId =
        getAttachDocsEmailTemplateId(
            RegionEnum.valueOf(region),
            LanguageEnum.valueOf(language),
            UserJourneyEnum.valueOf(journeyType));

    log.info(
        "Attempting to send attach document confirmation email using template:{} "
            + "for application Id:{}, region:{} and language:{}",
        emailTemplateId,
        applicationId,
        region,
        language);

    emailService.sendEmail(apiKey, email, emailTemplateId, applicationId);

    log.info(
        "Attach document confirmation email sent using template:{} for application Id:{}, "
            + "region:{} and language:{}",
        emailTemplateId,
        applicationId,
        region,
        language);
  }

  private String getAttachDocsEmailTemplateId(
      RegionEnum region, LanguageEnum language, UserJourneyEnum journeyType) {

    if (journeyType.equals(PIP2_INVITED)) {
      if (region.equals(RegionEnum.NI)) {
        return notifyConfigProperties.getNiInvitedAttachDocumentsEmailTemplateId();
      } else {
        if (language.equals(LanguageEnum.CY)) {
          return notifyConfigProperties.getGbCyInvitedAttachDocumentsEmailTemplateId();
        } else {
          return notifyConfigProperties.getGbEnInvitedAttachDocumentsEmailTemplateId();
        }
      }
    } else {
      if (region.equals(RegionEnum.NI)) {
        return notifyConfigProperties.getNiAttachDocumentsEmailTemplateId();
      }
      return notifyConfigProperties.getGbAttachDocumentsEmailTemplateId();
    }
  }
}
