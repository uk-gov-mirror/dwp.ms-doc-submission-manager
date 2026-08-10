package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.RegistrationDto;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class HealthCaptureApplicationMapper {

  static uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
          .PersonalDetailsDto
      mapRegistrationDetailsToPersonalDetailsDtoV2(RegistrationDto registration) {
    log.info("Begin mapRegistrationDetailsToPersonalDetailsDtoV2()");

    final uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
            .PersonalDetailsDto
        personalDetailsDto =
            new uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
                .PersonalDetailsDto();
    personalDetailsDto.setDateOfBirth(registration.getPersonalDetails().getDateOfBirth());
    personalDetailsDto.setFirstName(registration.getPersonalDetails().getFirstName());
    personalDetailsDto.setSurname(registration.getPersonalDetails().getSurname());
    personalDetailsDto.setPostcode(registration.getPersonalDetails().getAddress().getPostcode());
    personalDetailsDto.setNationalInsuranceNumber(
        registration.getPersonalDetails().getNationalInsuranceNumber());

    log.info("End mapRegistrationDetailsToPersonalDetailsDtoV2()");

    return personalDetailsDto;
  }

  static uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
          .PersonalDetailsDto
      mapAccountDetailsToPersonalDetailsDtoV2(V7AccountDetails accountDetails) {
    log.info("Begin mapAccountDetailsToPersonalDetailsDtoV2()");

    final uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
            .PersonalDetailsDto
        personalDetailsDto =
            new uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
                .PersonalDetailsDto();
    personalDetailsDto.setDateOfBirth(accountDetails.getDob().format(ISO_LOCAL_DATE));
    personalDetailsDto.setFirstName(accountDetails.getForename());
    personalDetailsDto.setSurname(accountDetails.getSurname());
    personalDetailsDto.setPostcode(accountDetails.getPostcode());
    personalDetailsDto.setNationalInsuranceNumber(accountDetails.getNino());
    personalDetailsDto.setEmail(accountDetails.getEmail());

    log.info("End mapAccountDetailsToPersonalDetailsDtoV2()");

    return personalDetailsDto;
  }
}
