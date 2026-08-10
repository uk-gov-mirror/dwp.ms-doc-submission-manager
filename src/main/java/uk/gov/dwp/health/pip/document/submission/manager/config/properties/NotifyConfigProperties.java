package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import uk.gov.dwp.crypto.SecureStrings;
import uk.gov.dwp.health.pip.document.submission.manager.exception.SecureStringException;

@Slf4j
@Configuration
@Setter
@Validated
@ConfigurationProperties(prefix = "notify")
public class NotifyConfigProperties {

  // New (DTH) Invited Digital
  private String gbWelshInvitedSubmissionSuccessTemplateId;
  private String gbEnglishInvitedSubmissionSuccessTemplateId;
  private String niInvitedSubmissionSuccessTemplateId;

  // Legacy (PIP Creds) Invited Digital
  private String legacyGbInvitedSubmissionSuccessTemplateId;
  private String legacyNiInvitedSubmissionSuccessTemplateId;

  // Self serve
  private String gbSubmissionSuccessTemplateId;
  private String niAttachDocumentsTemplateId;
  private String gbAttachDocumentsTemplateId;
  private String niInvitedAttachDocumentsTemplateId;
  private String gbEnInvitedAttachDocumentsTemplateId;
  private String gbCyInvitedAttachDocumentsTemplateId;


  @NotNull(message = "NI Notify API key required")
  private SealedObject niApiKey;

  @NotNull(message = "GB Notify API key required")
  private SealedObject gbApiKey;

  private final SecureStrings cipher;
  @Getter
  private String proxyHost;
  @Getter
  private Integer proxyPort;

  public String getNiApiKey() {
    return cipher.revealString(niApiKey);
  }

  public String getGbApiKey() {
    return cipher.revealString(gbApiKey);
  }

  public NotifyConfigProperties(SecureStrings secureStrings) {
    this.cipher = secureStrings;
  }

  public void setNiApiKey(String niApiKey) {
    if (niApiKey != null && !niApiKey.isBlank()) {
      try {
        this.niApiKey = cipher.sealString(niApiKey);
      } catch (IllegalBlockSizeException | IOException ex) {
        throw new SecureStringException("Secure string unable to seal secret for NI api key");
      }
    }
  }

  public void setGbApiKey(String gbApiKey) {
    if (gbApiKey != null && !gbApiKey.isBlank()) {
      try {
        this.gbApiKey = cipher.sealString(gbApiKey);
      } catch (IllegalBlockSizeException | IOException ex) {
        throw new SecureStringException("Secure string unable to seal secret for GB api key");
      }
    }
  }

  public String getLegacyNiInvitedSubmissionSuccessEmailTemplateId() {
    if (legacyNiInvitedSubmissionSuccessTemplateId == null
            || legacyNiInvitedSubmissionSuccessTemplateId.isBlank()) {
      final String message =
              "Require Northern Ireland invited success email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return legacyNiInvitedSubmissionSuccessTemplateId;
  }

  public String getLegacyEnglishGbInvitedSubmissionSuccessEmailTemplateId() {
    if (legacyGbInvitedSubmissionSuccessTemplateId == null
            || legacyGbInvitedSubmissionSuccessTemplateId.isBlank()) {
      final String message =
              "Require English Great Britain invited success email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return legacyGbInvitedSubmissionSuccessTemplateId;
  }

  public String getWelshGbInvitedSubmissionSuccessEmailTemplateId() {
    if (gbWelshInvitedSubmissionSuccessTemplateId == null
            || gbWelshInvitedSubmissionSuccessTemplateId.isBlank()) {
      final String message =
              "Require Welsh Great Britain invited success email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return gbWelshInvitedSubmissionSuccessTemplateId;
  }

  public String getEnglishGbInvitedSubmissionSuccessEmailTemplateId() {
    if (gbEnglishInvitedSubmissionSuccessTemplateId == null
        || gbEnglishInvitedSubmissionSuccessTemplateId.isBlank()) {
      final String message =
          "Require Welsh Great Britain invited success email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return gbEnglishInvitedSubmissionSuccessTemplateId;
  }

  public String getNiInvitedSubmissionSuccessEmailTemplateId() {
    if (niInvitedSubmissionSuccessTemplateId == null
        || niInvitedSubmissionSuccessTemplateId.isBlank()) {
      final String message =
          "Require Welsh Great Britain invited success email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return niInvitedSubmissionSuccessTemplateId;
  }

  public String getGbSubmissionSuccessEmailTemplateId() {
    if (gbSubmissionSuccessTemplateId == null || gbSubmissionSuccessTemplateId.isBlank()) {
      final String message =
              "Require Great Britain success email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return gbSubmissionSuccessTemplateId;
  }

  public String getNiAttachDocumentsEmailTemplateId() {
    if (niAttachDocumentsTemplateId == null || niAttachDocumentsTemplateId.isBlank()) {
      final String message =
          "Require Northern Ireland attach documents email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return niAttachDocumentsTemplateId;
  }

  public String getGbAttachDocumentsEmailTemplateId() {
    if (gbAttachDocumentsTemplateId == null || gbAttachDocumentsTemplateId.isBlank()) {
      final String message =
          "Require Great Britain attach documents email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return gbAttachDocumentsTemplateId;
  }

  public String getNiInvitedAttachDocumentsEmailTemplateId() {
    if (niInvitedAttachDocumentsTemplateId == null
        || niInvitedAttachDocumentsTemplateId.isBlank()) {
      final String message =
          "Require Northern Ireland invited attach documents email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return niInvitedAttachDocumentsTemplateId;
  }

  public String getGbEnInvitedAttachDocumentsEmailTemplateId() {
    if (gbEnInvitedAttachDocumentsTemplateId == null
        || gbEnInvitedAttachDocumentsTemplateId.isBlank()) {
      final String message =
          "Require Great Britain English invited attach documents email template "
              + "id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return gbEnInvitedAttachDocumentsTemplateId;
  }

  public String getGbCyInvitedAttachDocumentsEmailTemplateId() {
    if (gbCyInvitedAttachDocumentsTemplateId == null
        || gbCyInvitedAttachDocumentsTemplateId.isBlank()) {
      final String message =
          "Require Great Britain Welsh invited attach documents email template id but not provided";
      log.error(message);
      throw new IllegalStateException(message);
    }
    return gbCyInvitedAttachDocumentsTemplateId;
  }
}
