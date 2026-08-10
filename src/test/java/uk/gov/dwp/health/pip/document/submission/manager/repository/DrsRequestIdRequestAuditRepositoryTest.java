package uk.gov.dwp.health.pip.document.submission.manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.model.DrsStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class DrsRequestIdRequestAuditRepositoryTest {

  @Autowired private DrsRequestAuditRepository cut;

  @BeforeEach
  void setup() {
    cut.deleteAll();
  }

  @Test
  @DisplayName("Test create DRS request audit record in mongo, request id returned")
  void testCreateDrsRequestAuditRecordInMongoRequestIdReturned() {
    LocalDateTime today = LocalDateTime.of(2020, 9, 1, 13, 17, 20);
    LocalDateTime todayMinus1Day = today.minusDays(1);
    DrsUpload drsRequest =
        DrsUpload.builder()
            .submissionId("456")
            .submittedAt(todayMinus1Day)
            .completedAt(today)
            .errors("mocked_error_response_json")
            .status(DrsStatusEnum.PUBLISHED.status)
            .documentIdIds(List.of(DocumentId.builder().documentId("doc_id_123").build()))
            .build();
    DrsUpload actual = cut.save(drsRequest);
    assertThat(actual.getId()).isNotNull();
    assertThat(actual.getSubmissionId()).isEqualTo("456");
    assertThat(actual.getSubmittedAt()).isEqualTo(todayMinus1Day);
    assertThat(actual.getStatus()).isEqualTo(DrsStatusEnum.PUBLISHED.status);
    assertThat(actual.getErrors()).isEqualTo("mocked_error_response_json");
    assertThat(actual.getDocumentIdIds())
        .usingFieldByFieldElementComparator()
        .isEqualTo(List.of(DocumentId.builder().documentId("doc_id_123").build()));
  }

  @Test
  @DisplayName("Test to find by submittedAt from last one day, returns drs requests")
  void testFindBySubmittedAtIsAfter() {
    var today = LocalDateTime.of(2021, 1, 1, 15, 18, 24);
    var today1 = today.minusMinutes(3);
    var drsRequest =
        DrsUpload.builder()
            .submissionId("sub-1")
            .submittedAt(today.minusDays(1))
            .completedAt(today)
            .status(DrsStatusEnum.SUCCESS.status)
            .build();
    cut.save(drsRequest);
    var drsRequest1 =
        DrsUpload.builder()
            .submissionId("sub-2")
            .submittedAt(today1.minusDays(2))
            .completedAt(today1)
            .status(DrsStatusEnum.PUBLISHED.status)
            .build();
    cut.save(drsRequest1);
    var drsRequest2 =
        DrsUpload.builder()
            .submissionId("sub-3")
            .submittedAt(today1.minusDays(1))
            .completedAt(today1)
            .errors("mocked_error_response_json")
            .status(DrsStatusEnum.FAIL.status)
            .build();
    cut.save(drsRequest2);
    var drsRequest3 =
        DrsUpload.builder()
            .submissionId("sub-4")
            .submittedAt(today1.minusDays(1))
            .completedAt(today1)
            .status(DrsStatusEnum.RESUBMITTED.status)
            .build();
    cut.save(drsRequest3);
    List<DrsUpload> drsUploads =
        cut.findBySubmittedAtIsAfter(today.toLocalDate().atStartOfDay().minusDays(1));
    assertThat(drsUploads.size()).isEqualTo(3);
    DrsUpload drsUpload = drsUploads.get(0);
    DrsUpload drsUpload2 = drsUploads.get(1);
    DrsUpload drsUpload3 = drsUploads.get(2);
    assertThat(drsUpload.getId()).isNotNull();
    assertThat(drsUpload2.getId()).isNotNull();
    assertThat(drsUpload.getSubmissionId()).isEqualTo("sub-1");
    assertThat(drsUpload.getCompletedAt()).isEqualTo(today);
    assertThat(drsUpload.getSubmittedAt()).isEqualTo(today.minusDays(1));
    assertThat(drsUpload.getStatus()).isEqualTo(DrsStatusEnum.SUCCESS.status);
    assertThat(drsUpload3.getStatus()).isEqualTo(DrsStatusEnum.RESUBMITTED.status);
    assertThat(drsUpload2.getStatus()).isEqualTo(DrsStatusEnum.FAIL.status);
    assertThat(drsUpload2.getErrors()).isEqualTo("mocked_error_response_json");
  }
}
