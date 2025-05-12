package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import lombok.Getter;
import lombok.Setter;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.fileuploads.FileUpload;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.state.ApplicationTimeframe;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.state.State;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionAnswerSectionDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RegistrationDetailsDto;
import uk.gov.dwp.health.pip.forms.FormSpecification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class HealthCaptureApplicationDto {

  private String id;
  private String applicationId;
  private String claimantId;
  private String formSpecificationId;
  private String submissionId;
  @JsonFormat(shape = Shape.STRING, pattern = "EEE MMM dd HH:mm:ss zzz yyyy")
  private Date submissionDate;
  private RegistrationDetailsDto registrationDetails;
  private List<QuestionAnswerSectionDto> responses = new ArrayList<>();
  private List<FileUpload> files = new ArrayList<>();
  private State state;
  private Audit audit;
  private ApplicationTimeframe applicationTimeframe;

  public void addFile(FileUpload fileUpload) {
    this.files.add(fileUpload);
  }
}
