package uk.gov.dwp.health.pip.document.submission.manager.entity.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.HealthInformationGatherRequest;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.RegistrationDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.SubmissionDtoV4;

import static uk.gov.dwp.health.pip.document.submission.manager.utils.DateTimeUtils.convertISOToYYYYMMDD;

@Component
@RequiredArgsConstructor
public class ApplicationToPdfSubmissionDtoMapper {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public SubmissionDtoV4 map(final HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2) {
    SubmissionDtoV4 dto = new SubmissionDtoV4();
    dto.setApplicationId(healthCaptureApplicationDtoV2.getApplicationId());
    dto.setClaimantId(healthCaptureApplicationDtoV2.getClaimantId());
    dto.setSubmissionDate(convertISOToYYYYMMDD(healthCaptureApplicationDtoV2.getSubmissionDate()));
    dto.setFormData(
        convertJsonToObject(
            healthCaptureApplicationDtoV2.getFormData().getMappedData(),
            HealthInformationGatherRequest.class));
    dto.setRegistrationDetails(
        mapRegistrationDetails(healthCaptureApplicationDtoV2.getRegistrationDetails()));

    return dto;
  }

  private RegistrationDetailsDto mapRegistrationDetails(
      uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
              .RegistrationDetailsDto
          registrationDetails) {
    RegistrationDetailsDto registrationDetailsDto = new RegistrationDetailsDto();

    if (registrationDetails != null) {
      registrationDetailsDto.personalDetails(mapPersonalDetails(registrationDetails));
    }

    return registrationDetailsDto;
  }

  private PersonalDetailsDto mapPersonalDetails(
      uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto
              .RegistrationDetailsDto
          registrationDetails) {
    PersonalDetailsDto personalDetailsDto = new PersonalDetailsDto();

    if (registrationDetails.getPersonalDetails() != null) {
      personalDetailsDto
          .firstName(registrationDetails.getPersonalDetails().getFirstName())
          .surname(registrationDetails.getPersonalDetails().getSurname())
          .dateOfBirth(registrationDetails.getPersonalDetails().getDateOfBirth())
          .nationalInsuranceNumber(
              registrationDetails.getPersonalDetails().getNationalInsuranceNumber())
          .postcode(registrationDetails.getPersonalDetails().getPostcode())
          .email(registrationDetails.getPersonalDetails().getEmail());
    }

    return personalDetailsDto;
  }

  private <T> T convertJsonToObject(Object object, Class<T> clazz) {
    try {
      if (object instanceof String) {
        return OBJECT_MAPPER.readValue((String) object, clazz);
      }
      return OBJECT_MAPPER.convertValue(object, clazz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize object to " + clazz.getSimpleName(), e);
    }
  }
}
