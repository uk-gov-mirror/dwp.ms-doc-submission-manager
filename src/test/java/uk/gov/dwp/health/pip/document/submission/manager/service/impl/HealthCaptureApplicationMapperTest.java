package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.AddressDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.RegistrationDto;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCaptureApplicationMapperTest {

  @Test
  void mapRegistrationDetailsToPersonalDetailsDtoV2() {
    uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
            .PersonalDetailsDto
        personalDetailsDto =
            HealthCaptureApplicationMapper.mapRegistrationDetailsToPersonalDetailsDtoV2(
                new RegistrationDto()
                    .personalDetails(
                        new PersonalDetailsDto()
                            .dateOfBirth("date-of-birth")
                            .firstName("first-name")
                            .surname("surname")
                            .nationalInsuranceNumber("nino")
                            .address(new AddressDto().postcode("postcode"))));

    assertThat(personalDetailsDto.getDateOfBirth()).isEqualTo("date-of-birth");
    assertThat(personalDetailsDto.getFirstName()).isEqualTo("first-name");
    assertThat(personalDetailsDto.getSurname()).isEqualTo("surname");
    assertThat(personalDetailsDto.getPostcode()).isEqualTo("postcode");
    assertThat(personalDetailsDto.getNationalInsuranceNumber()).isEqualTo("nino");
  }

  @Test
  void mapAccountDetailsToPersonalDetailsDtoV2() {
    uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
            .PersonalDetailsDto
        personalDetailsDto =
            HealthCaptureApplicationMapper.mapAccountDetailsToPersonalDetailsDtoV2(
                new V7AccountDetails()
                    .dob(LocalDate.EPOCH)
                    .forename("forename")
                    .surname("surname")
                    .postcode("postcode")
                    .nino("nino")
                    .email("email"));

    assertThat(personalDetailsDto.getDateOfBirth()).isEqualTo("1970-01-01");
    assertThat(personalDetailsDto.getFirstName()).isEqualTo("forename");
    assertThat(personalDetailsDto.getSurname()).isEqualTo("surname");
    assertThat(personalDetailsDto.getPostcode()).isEqualTo("postcode");
    assertThat(personalDetailsDto.getNationalInsuranceNumber()).isEqualTo("nino");
    assertThat(personalDetailsDto.getEmail()).isEqualTo("email");
  }
}
