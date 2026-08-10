package uk.gov.dwp.health.pip.document.submission.manager.entity.mapping;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.AddressDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.FormDataDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthProfessionalDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.RegistrationDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.SubmissionDtoV4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.DateTimeUtils.instantToString;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ApplicationToPdfSubmissionDtoMapperTest {

  private final ApplicationToPdfSubmissionDtoMapper applicationToPdfSubmissionDtoMapper =
      new ApplicationToPdfSubmissionDtoMapper();

  @Test
  void when_mapping_health_capture_application_dto_v2_to_submission_dto_v4() throws IOException {
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2 =
        getHealthCaptureApplicationDtoV2();

    SubmissionDtoV4 submissionDtoV3 =
        applicationToPdfSubmissionDtoMapper.map(healthCaptureApplicationDtoV2);

    verifySubmissionDtoV4(submissionDtoV3);
  }

  private void verifySubmissionDtoV4(SubmissionDtoV4 submissionDtoV3) {
    assertThat(submissionDtoV3.getApplicationId()).isEqualTo("application-id-1");
    assertThat(submissionDtoV3.getClaimantId()).isEqualTo("claimant-id-1");
    assertThat(submissionDtoV3.getSubmissionDate()).isEqualTo("2025-07-15");
    uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.PersonalDetailsDto
        personalDetailsDto = submissionDtoV3.getRegistrationDetails().getPersonalDetails();
    assertThat(personalDetailsDto.getFirstName()).isEqualTo("first-name");
    assertThat(personalDetailsDto.getSurname()).isEqualTo("surname");
    assertThat(personalDetailsDto.getDateOfBirth()).isEqualTo("date-of-birth");
    assertThat(personalDetailsDto.getNationalInsuranceNumber()).isEqualTo("nino");
    assertThat(personalDetailsDto.getPostcode()).isEqualTo("postcode");
    assertThat(personalDetailsDto.getEmail()).isEqualTo("email");
    assertThat(submissionDtoV3.getApplicationId()).isEqualTo("application-id-1");
    assertThat(submissionDtoV3.getFormData()).isNotNull();

  }

  private HealthCaptureApplicationDtoV2 getHealthCaptureApplicationDtoV2() throws IOException {
    String jsonRequest = readFromFile("entity/dto/healthInformationGatherRequest.json");
    return new HealthCaptureApplicationDtoV2()
        .applicationId("application-id-1")
        .claimantId("claimant-id-1")
        .submissionDate(instantToString(Instant.parse("2025-07-15T23:30:00Z")))
        .formData(
            new FormDataDto()
                .journeyContexts("journey-contexts")
                .appName("app-name")
                .version("version")
                .mappedData(jsonRequest))
        .registrationDetails(
            new RegistrationDetailsDto()
                .healthDetails(
                    new HealthDetailsDto()
                        .addHealthProfessionalsItem(
                            new HealthProfessionalDto()
                                .name("hp-name")
                                .address(
                                    new AddressDto()
                                        .line1("address-line-1")
                                        .line2("address-line-2")
                                        .line3("address-line-3")
                                        .town("town")
                                        .county("county")
                                        .country("country")
                                        .postcode("postcode"))
                                .phoneNumber("phone-number")
                                .profession("profession")
                                .lastContact("last-contact")))
                .personalDetails(
                    new PersonalDetailsDto()
                        .firstName("first-name")
                        .surname("surname")
                        .email("email")
                        .postcode("postcode")
                        .dateOfBirth("date-of-birth")
                        .nationalInsuranceNumber("nino")));
  }

  private String readFromFile(String filePath) throws IOException {
    String formData =
        Files.readString(Path.of(getClass().getClassLoader().getResource(filePath).getPath()));
    return formData;
  }
}
