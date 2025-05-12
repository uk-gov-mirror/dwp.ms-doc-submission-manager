package uk.gov.dwp.health.pip.document.submission.manager.model.application.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class History {

  private String state;

  private Instant timeStamp;
}