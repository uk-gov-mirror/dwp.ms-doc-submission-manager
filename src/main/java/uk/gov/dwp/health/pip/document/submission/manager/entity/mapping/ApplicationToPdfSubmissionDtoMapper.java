package uk.gov.dwp.health.pip.document.submission.manager.entity.mapping;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionAnswerSectionDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionDto;
import uk.gov.dwp.health.pip.forms.FormSpecification;
import uk.gov.dwp.health.pip.forms.enumerations.ViewType;
import uk.gov.dwp.health.pip.forms.viewspecifications.TaskList;
import uk.gov.dwp.health.pip.forms.viewspecifications.elements.TaskListTask;

@Component
@RequiredArgsConstructor
public class ApplicationToPdfSubmissionDtoMapper {

  public SubmissionDto map(final HealthCaptureApplicationDto healthCaptureApplication,
      final AuditableFormSpecificationDto formSpecification) {
    var dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    var submissionDate = dateFormatter.format(healthCaptureApplication.getSubmissionDate());

    var dto = new SubmissionDto();
    dto.setApplicationId(healthCaptureApplication.getApplicationId());
    dto.setClaimantId(healthCaptureApplication.getClaimantId());
    dto.setFormSpecificationId(healthCaptureApplication.getFormSpecificationId());
    dto.setSubmissionDate(submissionDate);
    dto.setResponses(
        filterAndSortResponsesIntoFormSpecOrder(healthCaptureApplication.getResponses(),
            formSpecification));
    dto.setRegistrationDetails(healthCaptureApplication.getRegistrationDetails());

    return dto;
  }

  private List<QuestionAnswerSectionDto> filterAndSortResponsesIntoFormSpecOrder(
      List<QuestionAnswerSectionDto> responses, FormSpecification formSpecification) {
    var sectionReferences = ((TaskList) formSpecification.getViews().stream()
        .filter(view -> view.getType().equals(ViewType.TASK_LIST)).findFirst().get()).getTasks()
        .stream().map(TaskListTask::getReference).toList();

    Map<String, Integer> order = IntStream.range(0, sectionReferences.size()).boxed()
        .collect(Collectors.toMap(sectionReferences::get, Function.identity()));

    Comparator<QuestionAnswerSectionDto> comparator = Comparator.comparingInt(
        obj -> order.getOrDefault(obj.getReference(), order.size()));

    return responses.stream().filter(
            questionAnswerSection -> !questionAnswerSection.getReference().contains("submission"))
        .sorted(comparator).toList();
  }
}
