package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.LanguageEnum.CY;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.LanguageEnum.EN;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.RegionEnum.GB;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.RegionEnum.NI;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum.PIP2_INVITED;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum.STRATEGIC;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.NotifyConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.service.EmailService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email notification service test")
class EmailNotificationServiceImplTest {
  public static final String NI_API_KEY = "ni-api-key";
  public static final String GB_API_KEY = "gb-api-key";
  public static final String EMAIL = "test@email.com";
  public static final String APPLICATION_ID = "application-id";
  @Mock private NotifyConfigProperties notifyConfigProperties;
  @Mock private EmailService emailService;
  @InjectMocks private EmailNotificationServiceImpl emailNotificationService;

  @Test
  @DisplayName("Send correct submission email for Self Serve GB region")
  void sendSubmissionEmailNotificationSelfServeGBRegion() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getGbSubmissionSuccessEmailTemplateId())
        .thenReturn("gb-self-serve-email-template-id");

    emailNotificationService.sendSubmissionEmailNotification(
        EMAIL, GB, STRATEGIC, APPLICATION_ID, EN, false);

    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getGbSubmissionSuccessEmailTemplateId();
    verify(emailService).sendEmail(GB_API_KEY, EMAIL, "gb-self-serve-email-template-id", APPLICATION_ID);
  }

  @Test
  @DisplayName("Send correct submission email for DTH Invited Digital GB region English language")
  void sendSubmissionEmailNotificationDTHInvitedDigitalGBRegionEnglishLanguage() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getEnglishGbInvitedSubmissionSuccessEmailTemplateId())
        .thenReturn("gb-english-invited-template-id");

    emailNotificationService.sendSubmissionEmailNotification(
        EMAIL, GB, PIP2_INVITED, APPLICATION_ID, EN, false);

    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getEnglishGbInvitedSubmissionSuccessEmailTemplateId();
    verify(emailService).sendEmail(GB_API_KEY, EMAIL, "gb-english-invited-template-id", APPLICATION_ID);
  }

  @Test
  @DisplayName("Send correct submission email for DTH Invited Digital GB region Welsh language")
  void sendSubmissionEmailNotificationDTHInvitedDigitalGBRegionWelshLanguage() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getWelshGbInvitedSubmissionSuccessEmailTemplateId())
        .thenReturn("gb-welsh-invited-template-id");

    emailNotificationService.sendSubmissionEmailNotification(
        EMAIL, GB, PIP2_INVITED, APPLICATION_ID, CY, false);

    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getWelshGbInvitedSubmissionSuccessEmailTemplateId();
    verify(emailService).sendEmail(GB_API_KEY, EMAIL, "gb-welsh-invited-template-id", APPLICATION_ID);
  }

  @Test
  @DisplayName("Send correct submission email for DTH Invited Digital NI region English language")
  void sendSubmissionEmailNotificationDTHInvitedDigitalNIRegionEnglishLanguage() {
    when(notifyConfigProperties.getNiApiKey()).thenReturn(NI_API_KEY);
    when(notifyConfigProperties.getNiInvitedSubmissionSuccessEmailTemplateId())
        .thenReturn("ni-english-invited-template-id");

    emailNotificationService.sendSubmissionEmailNotification(
        EMAIL, NI, PIP2_INVITED, APPLICATION_ID, EN, false);

    verify(notifyConfigProperties).getNiApiKey();
    verify(notifyConfigProperties).getNiInvitedSubmissionSuccessEmailTemplateId();
    verify(emailService).sendEmail(NI_API_KEY, EMAIL, "ni-english-invited-template-id", APPLICATION_ID);
  }

  @Test
  @DisplayName("Send correct submission email for legacy (PIP Creds) Invited Digital NI region English language")
  void sendSubmissionEmailNotificationLegacyInvitedDigitalNIRegionEnglishLanguage() {
    when(notifyConfigProperties.getNiApiKey()).thenReturn(NI_API_KEY);
    when(notifyConfigProperties.getLegacyNiInvitedSubmissionSuccessEmailTemplateId())
        .thenReturn("legacy-ni-english-invited-template-id");

    emailNotificationService.sendSubmissionEmailNotification(
        EMAIL, NI, PIP2_INVITED, APPLICATION_ID, EN, true);

    verify(notifyConfigProperties).getNiApiKey();
    verify(notifyConfigProperties).getLegacyNiInvitedSubmissionSuccessEmailTemplateId();
    verify(emailService).sendEmail(NI_API_KEY, EMAIL, "legacy-ni-english-invited-template-id", APPLICATION_ID);
  }

  @Test
  @DisplayName("Send correct submission email for legacy (PIP Creds) Invited Digital GB region English language")
  void sendSubmissionEmailNotificationLegacyInvitedDigitalGBRegionEnglishLanguage() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getLegacyEnglishGbInvitedSubmissionSuccessEmailTemplateId())
        .thenReturn("legacy-gb-english-invited-template-id");

    emailNotificationService.sendSubmissionEmailNotification(
        EMAIL, GB, PIP2_INVITED, APPLICATION_ID, EN, true);

    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getLegacyEnglishGbInvitedSubmissionSuccessEmailTemplateId();
    verify(emailService).sendEmail(GB_API_KEY, EMAIL, "legacy-gb-english-invited-template-id", APPLICATION_ID);
  }

  @Test
  @DisplayName("Send attach documents email should be sent for NON invited NI region")
  void sendAttachDocsEmailNotificationNonInvitedNiRegion() {
    when(notifyConfigProperties.getNiApiKey()).thenReturn(NI_API_KEY);
    when(notifyConfigProperties.getNiAttachDocumentsEmailTemplateId())
        .thenReturn("ni-email-template-id");

    emailNotificationService.sendAttachDocsEmailNotification(
        EMAIL, NI.name(), EN.name(), STRATEGIC.name(), APPLICATION_ID);

    verify(emailService).sendEmail(NI_API_KEY, EMAIL, "ni-email-template-id", APPLICATION_ID);
    verify(notifyConfigProperties).getNiApiKey();
    verify(notifyConfigProperties).getNiAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbApiKey();
    verify(notifyConfigProperties, times(0)).getNiInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbCyInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbEnInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbAttachDocumentsEmailTemplateId();
  }

  @Test
  @DisplayName("Send attach documents email should be sent for NON invited GB region")
  void sendAttachDocsEmailNotificationNonInvitedGBRegion() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getGbAttachDocumentsEmailTemplateId())
        .thenReturn("gb-email-template-id");

    emailNotificationService.sendAttachDocsEmailNotification(
        EMAIL, GB.name(), EN.name(), STRATEGIC.name(), APPLICATION_ID);

    verify(emailService).sendEmail(GB_API_KEY, EMAIL, "gb-email-template-id", APPLICATION_ID);
    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getGbAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiApiKey();
    verify(notifyConfigProperties, times(0)).getNiAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbCyInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbEnInvitedAttachDocumentsEmailTemplateId();
  }

  @Test
  @DisplayName("Send attach documents email should be sent for invited NI region")
  void sendAttachDocsEmailNotificationInvitedNiRegion() {
    when(notifyConfigProperties.getNiApiKey()).thenReturn(NI_API_KEY);
    when(notifyConfigProperties.getNiInvitedAttachDocumentsEmailTemplateId())
        .thenReturn("ni-invited-email-template-id");

    emailNotificationService.sendAttachDocsEmailNotification(
        EMAIL, NI.name(), EN.name(), PIP2_INVITED.name(), APPLICATION_ID);

    verify(emailService)
        .sendEmail(NI_API_KEY, EMAIL, "ni-invited-email-template-id", APPLICATION_ID);
    verify(notifyConfigProperties).getNiApiKey();
    verify(notifyConfigProperties).getNiInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbApiKey();
    verify(notifyConfigProperties, times(0)).getNiAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbCyInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbEnInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbAttachDocumentsEmailTemplateId();
  }

  @Test
  @DisplayName("Send attach documents email should be sent for invited GB region with EN language")
  void sendAttachDocsEmailNotificationInvitedGBRegionEnLanguage() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getGbEnInvitedAttachDocumentsEmailTemplateId())
        .thenReturn("gb-invited-en-email-template-id");

    emailNotificationService.sendAttachDocsEmailNotification(
        EMAIL, GB.name(), EN.name(), PIP2_INVITED.name(), APPLICATION_ID);

    verify(emailService)
        .sendEmail(GB_API_KEY, EMAIL, "gb-invited-en-email-template-id", APPLICATION_ID);
    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getGbEnInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiApiKey();
    verify(notifyConfigProperties, times(0)).getGbAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbCyInvitedAttachDocumentsEmailTemplateId();
  }

  @Test
  @DisplayName("Send attach documents email should be sent for invited GB region with Cy language")
  void sendAttachDocsEmailNotificationInvitedGBRegionCyLanguage() {
    when(notifyConfigProperties.getGbApiKey()).thenReturn(GB_API_KEY);
    when(notifyConfigProperties.getGbCyInvitedAttachDocumentsEmailTemplateId())
        .thenReturn("gb-invited-cy-email-template-id");

    emailNotificationService.sendAttachDocsEmailNotification(
        EMAIL, GB.name(), CY.name(), PIP2_INVITED.name(), APPLICATION_ID);

    verify(emailService)
        .sendEmail(GB_API_KEY, EMAIL, "gb-invited-cy-email-template-id", APPLICATION_ID);
    verify(notifyConfigProperties).getGbApiKey();
    verify(notifyConfigProperties).getGbCyInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiApiKey();
    verify(notifyConfigProperties, times(0)).getGbAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getGbEnInvitedAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiAttachDocumentsEmailTemplateId();
    verify(notifyConfigProperties, times(0)).getNiInvitedAttachDocumentsEmailTemplateId();
  }
}
