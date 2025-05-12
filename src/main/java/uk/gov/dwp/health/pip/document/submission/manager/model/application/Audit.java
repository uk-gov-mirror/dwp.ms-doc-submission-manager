package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Audit {

  @CreatedDate
  private Instant created;
  @LastModifiedDate
  private Instant lastModified;
}