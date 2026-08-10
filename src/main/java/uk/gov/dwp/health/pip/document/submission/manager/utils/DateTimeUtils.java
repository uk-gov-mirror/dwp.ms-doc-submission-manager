package uk.gov.dwp.health.pip.document.submission.manager.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateTimeUtils {

  public static String instantToString(Instant instant) {
    return instant != null
        ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC).format(instant)
        : null;
  }

  public static Instant stringToInstant(String string) {
    if (string != null) {
      LocalDateTime localDateTime =
          LocalDateTime.parse(string, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      return localDateTime.toInstant(ZoneOffset.UTC);
    }
    return null;
  }

  public static String convertISOToYYYYMMDD(String isoDate) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(isoDate));
  }
}
