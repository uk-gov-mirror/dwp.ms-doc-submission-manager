package uk.gov.dwp.health.pip.document.submission.manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import support.TestConstant;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class SubmissionRepositoryTest {

  @Autowired private SubmissionRepository cut;

  @BeforeEach
  void setup() {
    cut.deleteAll();
  }

  @Nested
  class FoundSubmission {
    @Test
    void testSaveAndFindSubmission() {
      var sub =
          Submission.builder()
              .claimantId(TestConstant.CLAIMANT_ID_1)
              .applicationId(TestConstant.APPLICATION_ID)
              .documentIdIds(List.of(new DocumentId(TestConstant.DOC_ID_1), new DocumentId(TestConstant.DOC_ID_2)))
              .started(TestConstant.DATE)
              .completed(TestConstant.DATE.plusDays(2))
              .build();
      cut.save(sub);

      Submission actual = cut.findById(sub.getId()).get();
      assertThat(actual.getClaimantId()).isEqualTo(TestConstant.CLAIMANT_ID_1);
      assertThat(actual.getApplicationId()).isEqualTo(TestConstant.APPLICATION_ID);
      assertThat(actual.getStarted()).isEqualTo(TestConstant.DATE);
      assertThat(actual.getCompleted()).isEqualTo(TestConstant.DATE.plusDays(2));
      assertThat(actual.getDocumentIdIds().stream().map(DocumentId::getDocumentId).collect(Collectors.toList()))
          .containsSequence(TestConstant.DOC_ID_1, TestConstant.DOC_ID_2);
    }

    @Test
    void testFindSubmissionWithGivenDocumentId() {
      var sub1 =
          Submission.builder()
              .claimantId(TestConstant.CLAIMANT_ID_1)
              .applicationId(TestConstant.APPLICATION_ID)
              .documentIdIds(List.of(new DocumentId(TestConstant.DOC_ID_1), new DocumentId(TestConstant.DOC_ID_2)))
              .started(TestConstant.DATE)
              .completed(TestConstant.DATE.plusDays(1))
              .build();
      cut.save(sub1);

      var sub2 =
          Submission.builder()
              .claimantId(TestConstant.CLAIMANT_ID_2)
              .applicationId(TestConstant.APPLICATION_ID)
              .documentIdIds(List.of(new DocumentId(TestConstant.DOC_ID_3)))
              .started(TestConstant.DATE)
              .completed(TestConstant.DATE.plusDays(1))
              .build();

      cut.save(sub2);
      assertThat(cut.count()).isEqualTo(2L);

      var actualSavedSubmission2 =
          cut.findByDocumentIdIdsContains(List.of(new DocumentId(TestConstant.DOC_ID_3))).get();
      assertThat(actualSavedSubmission2.getDocumentIdIds().size()).isOne();
      assertThat(actualSavedSubmission2.getClaimantId()).isEqualTo(TestConstant.CLAIMANT_ID_2);
      assertThat(actualSavedSubmission2.getCompleted()).isEqualTo(TestConstant.DATE.plusDays(1));
      assertThat(actualSavedSubmission2.getStarted()).isEqualTo(TestConstant.DATE);
      assertThat(actualSavedSubmission2.getApplicationId()).isEqualTo(TestConstant.APPLICATION_ID);
      assertThat(
              actualSavedSubmission2.getDocumentIdIds().stream()
                  .map(DocumentId::getDocumentId)
                  .collect(Collectors.toList()))
          .containsSequence(TestConstant.DOC_ID_3);
    }

    @Test
    void testFindSubmissionByClaimantIdAndClaimId() {
      var sub1 =
          Submission.builder()
              .claimantId(TestConstant.CLAIMANT_ID_1)
              .applicationId(TestConstant.APPLICATION_ID)
              .documentIdIds(List.of(new DocumentId(TestConstant.DOC_ID_1), new DocumentId(TestConstant.DOC_ID_2)))
              .started(TestConstant.DATE)
              .completed(TestConstant.DATE.plusDays(1))
              .build();
      cut.save(sub1);
      Optional<Submission> actual =
          cut.findByClaimantIdAndApplicationId(TestConstant.CLAIMANT_ID_1, TestConstant.APPLICATION_ID);
      assertThat(actual).isPresent();
      Submission find = actual.get();
      assertThat(find.getDocumentIdIds().size()).isEqualTo(2);
      assertThat(find.getStarted()).isEqualTo(TestConstant.DATE);
      assertThat(find.getCompleted()).isEqualTo(TestConstant.DATE.plusDays(1));
    }
  }

  @Nested
  class NotingFound {
    @Test
    void testNotFindSubmissionWithGivenDocumentId() {
      var sub1 =
          Submission.builder()
              .claimantId(TestConstant.CLAIMANT_ID_1)
              .applicationId(TestConstant.APPLICATION_ID)
              .documentIdIds(List.of(new DocumentId(TestConstant.DOC_ID_1), new DocumentId(TestConstant.DOC_ID_2)))
              .started(TestConstant.DATE)
              .completed(TestConstant.DATE.plusDays(1))
              .build();
      cut.save(sub1);
      Optional<Submission> actual =
          cut.findByDocumentIdIdsContains(List.of(new DocumentId(TestConstant.DOC_ID_3)));
      assertThat(actual).isNotPresent();
    }

    @Test
    void testNotFindSubmissionByClaimantIdAndClaimId() {
      var sub1 =
          Submission.builder()
              .claimantId(TestConstant.CLAIMANT_ID_1)
              .applicationId(TestConstant.APPLICATION_ID)
              .documentIdIds(List.of(new DocumentId(TestConstant.DOC_ID_1), new DocumentId(TestConstant.DOC_ID_2)))
              .started(TestConstant.DATE)
              .completed(TestConstant.DATE.plusDays(1))
              .build();
      cut.save(sub1);
      assertThat(cut.count()).isOne();
      Optional<Submission> actual =
          cut.findByClaimantIdAndApplicationId(TestConstant.CLAIMANT_ID_2, TestConstant.APPLICATION_ID);
      assertThat(actual).isNotPresent();
    }
  }
}
