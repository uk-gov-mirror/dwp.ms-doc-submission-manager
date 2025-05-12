package uk.gov.dwp.health.pip.document.submission.manager.utils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class QueryUtils {

  public static <T> Stream<T> findByPredicate(List<T> iterable, Predicate<? super T> predicate) {
    return iterable
        .stream()
        .filter(predicate);
  }

  public static <T> T findOneByPredicate(List<T> iterable, Predicate<? super T> predicate) {
    var byPredicate = findByPredicate(iterable, predicate);
    var res = byPredicate.findFirst();
    return res.isPresent() ? res.get() : null;
  }
}
