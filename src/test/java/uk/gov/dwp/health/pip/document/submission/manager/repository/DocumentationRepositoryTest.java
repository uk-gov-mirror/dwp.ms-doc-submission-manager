package uk.gov.dwp.health.pip.document.submission.manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import support.TestConstant;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class DocumentationRepositoryTest {

  @Autowired private DocumentationRepository cut;

  @BeforeEach
  void setup() {
    cut.deleteAll();
  }

  @Test
  void testSaveDocumentation() {
    Documentation documentation =
        Documentation.builder()
            .claimantId(TestConstant.CLAIMANT_ID_1)
            .applicationId(TestConstant.APPLICATION_ID)
            .filename("test.pdf")
            .timestamp(TestConstant.TIME)
            .documentType("pdf")
            .storage(
                List.of(
                    Storage.builder()
                        .type("S3")
                        .url("S3_DOWNLOAD_URL")
                        .uniqueId("UNIQUE_ID")
                        .build()))
            .build();
    Documentation savedDocument = cut.save(documentation);
    Documentation actual = cut.findById(savedDocument.getId()).get();

    assertThat(actual.getClaimantId()).isEqualTo(TestConstant.CLAIMANT_ID_1);
    assertThat(actual.getFilename()).isEqualTo("test.pdf");
    assertThat(actual.getTimestamp()).isEqualTo(TestConstant.TIME);
    assertThat(actual.getDocumentType()).isEqualTo("pdf");
    assertThat(actual.getStorage())
        .usingFieldByFieldElementComparator()
        .isEqualTo(
            List.of(
                Storage.builder().type("S3").url("S3_DOWNLOAD_URL").uniqueId("UNIQUE_ID").build()));
  }
}
