package uk.gov.dwp.health.pip.document.submission.manager.model.application.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class State {

  private String current;

  private List<History> history;

  public void addHistory(History history) {
    current = history.getState();
    if (this.history == null) {
      this.history = new LinkedList<>();
    }
    this.history.add(0, history);
  }
}
