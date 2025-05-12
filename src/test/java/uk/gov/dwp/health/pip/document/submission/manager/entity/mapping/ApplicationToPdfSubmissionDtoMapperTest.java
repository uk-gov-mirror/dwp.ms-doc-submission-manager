package uk.gov.dwp.health.pip.document.submission.manager.entity.mapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionAnswerSectionDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ShortTextResponseDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;
import uk.gov.dwp.health.pip.forms.enumerations.ViewType;
import uk.gov.dwp.health.pip.forms.viewspecifications.TaskList;
import uk.gov.dwp.health.pip.forms.viewspecifications.elements.TaskListTask;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class ApplicationToPdfSubmissionDtoMapperTest {

  private final ApplicationToPdfSubmissionDtoMapper applicationToPdfSubmissionDtoMapper =
      new ApplicationToPdfSubmissionDtoMapper();

  @Test
  void when_mapping_to_health_capture_application_to_submission_dto() throws IOException {
    var existingApplication = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/healthCaptureApplication_submission.json",
        HealthCaptureApplicationDto.class);
    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "pip-form-specifications/specifications/en_health.json",
        AuditableFormSpecificationDto.class);

    var dto = new ShortTextResponseDto();
    dto.setQuestion("EXAMPLE_QUESTION");

    var result = applicationToPdfSubmissionDtoMapper.map(existingApplication, formSpecification);
    assertThat(result).isInstanceOf(SubmissionDto.class);
    assertThat(result.getApplicationId()).isEqualTo(existingApplication.getApplicationId());
    assertThat(result.getClaimantId()).isEqualTo(existingApplication.getClaimantId());
    assertThat(result.getFormSpecificationId()).isEqualTo(
        existingApplication.getFormSpecificationId());
    assertThat(result.getResponses().size()).isGreaterThan(0);
    assertThat(result.getRegistrationDetails().getPersonalDetails().getFirstName()).isEqualTo(
        existingApplication.getRegistrationDetails().getPersonalDetails().getFirstName());
    assertThat(result.getSubmissionDate()).isEqualTo("17-08-2023");

    var sectionReferences = ((TaskList) formSpecification.getViews().stream()
        .filter(view -> view.getType().equals(ViewType.TASK_LIST))
        .findFirst()
        .get()).getTasks().stream().map(TaskListTask::getReference).filter(
        reference -> !reference.contains("submission")).toList();
    var resultSectionReferences = result.getResponses().stream()
        .map(QuestionAnswerSectionDto::getReference).toList();
    assertThat(resultSectionReferences).isEqualTo(sectionReferences);
  }
}
