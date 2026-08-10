package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.document.submission.manager.exception.EmailException;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.net.Proxy;

@Service
@Slf4j
public class EmailService {

  private final Proxy notifyProxy;

  @Autowired
  public EmailService(@Qualifier("notifyNetProxy") Proxy notifyProxy) {
    this.notifyProxy = notifyProxy;
  }

  public void sendEmail(String apiKey, String emailAddress,
      String emailTemplateId, String applicationId) {

    NotificationClient client = new NotificationClient(apiKey, notifyProxy);

    log.info("Sending email request to notify client");

    try {
      client.sendEmail(emailTemplateId, emailAddress, null, "");
    } catch (NotificationClientException e) {
      throw new EmailException(
              "Error when sending email notification with template ID:"
                      + emailTemplateId
                      + " for application:"
                      + applicationId, e);
    }
  }
}
