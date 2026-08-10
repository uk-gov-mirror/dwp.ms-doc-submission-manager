package uk.gov.dwp.health.pip.document.submission.manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import support.TestConstant;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class RepositoryIntegrationTest {

  private static String submissionId;
  private static String documentId;
  @Autowired private DocumentationRepository docRepo;
  @Autowired private SubmissionRepository subRepo;

  @BeforeEach
  void setup() {
    docRepo.deleteAll();
    subRepo.deleteAll();
  }

  @Test
  void testCreateSubmission() {
    Submission sub =
        Submission.builder()
            .claimantId(TestConstant.CLAIMANT_ID_1)
            .applicationId(TestConstant.APPLICATION_ID)
            .completed(TestConstant.DATE)
            .started(TestConstant.DATE)
            .build();
    Submission submission = subRepo.save(sub);
    submissionId = submission.getId();
    assertThat(submissionId).isNotBlank();
    assertThat(submission.getDocumentIdIds()).isNull();
  }

  @Test
  void testAddDocumentToSubmission() {
    Submission sub =
        Submission.builder()
            .claimantId(TestConstant.CLAIMANT_ID_1)
            .applicationId(TestConstant.APPLICATION_ID)
            .completed(TestConstant.DATE)
            .started(TestConstant.DATE)
            .build();
    submissionId = subRepo.save(sub).getId();
    final Submission[] saved = new Submission[1];
    subRepo
        .findById(submissionId)
        .ifPresent(
            submission -> {
              Documentation doc =
                  Documentation.builder()
                      .claimantId(TestConstant.CLAIMANT_ID_1)
                      .applicationId(TestConstant.APPLICATION_ID)
                      .timestamp(TestConstant.TIME)
                      .documentType("pdf")
                      .storage(null)
                      .build();
              documentId = docRepo.save(doc).getId();
              submission.setDocumentIdIds(List.of(new DocumentId(documentId)));
              saved[0] = subRepo.save(submission);
              assertThat(saved[0].getDocumentIdIds().size()).isOne();
            });
    assertThat(documentId).isNotNull();
    assertThat(saved[0].getDocumentIdIds().get(0).getDocumentId()).isEqualTo(documentId);
  }
}
