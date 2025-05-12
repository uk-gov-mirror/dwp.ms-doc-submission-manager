package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import lombok.Getter;
import uk.gov.dwp.health.pip.forms.FormSpecification;

import java.time.LocalDateTime;


@Getter
public class AuditableFormSpecificationDto extends FormSpecification {

  private LocalDateTime createdDate;
}