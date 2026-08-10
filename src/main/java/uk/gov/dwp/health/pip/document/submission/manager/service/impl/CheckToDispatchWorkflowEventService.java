package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum.STRATEGIC;

import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;

@Component
class CheckToDispatchWorkflowEventService {

  static boolean shouldDispatchEvent(ApplicationDetails applicationDetails) {
    // Treat null as strategic. Old strategic applications will not have a journey type flag set.
    return applicationDetails.getJourneyType() == null
        || ApplicationDetails.JourneyTypeEnum.STRATEGIC.equals(
        applicationDetails.getJourneyType());
  }

  static boolean shouldDispatchEvent(V7AccountDetails account) {
    return STRATEGIC.equals(account.getUserJourney());
  }
}
