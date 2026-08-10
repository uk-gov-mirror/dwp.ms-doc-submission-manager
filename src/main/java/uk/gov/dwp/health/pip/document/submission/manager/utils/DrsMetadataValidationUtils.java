package uk.gov.dwp.health.pip.document.submission.manager.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public class DrsMetadataValidationUtils {

  private static final Pattern NINO_PATTERN
      = Pattern.compile("[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9]{6}[A-D]$");
  private static final Pattern POSTCODE_PATTERN
      = Pattern.compile("^[A-Z]{1,2}[0-9A-Z]{1,2} ?[0-9]?[A-Z]{2}$");

  public static String sanitiseName(String name) {
    if (name == null) {
      return null;
    }

    String result = name;

    // Remove invalid characters
    result = result.replaceAll("[^A-Za-z'.\\-\\s]", "");

    // Remove spaces before and after punctuation
    result = result.replaceAll("\\s+(['.-])", "$1");
    result = result.replaceAll("(['.-])\\s+", "$1");

    // Collapse consecutive identical punctuation
    result = result.replaceAll("'{2,}", "'");
    result = result.replaceAll("\\.{2,}", ".");
    result = result.replaceAll("-{2,}", "-");

    // Collapse mixed punctuation sequences
    result = result.replaceAll("['.-]{2,}", "");

    // Collapse consecutive spaces
    result = result.replaceAll("\\s+", " ");

    // First character must be alphabetic
    result = result.replaceAll("^[^A-Za-z]+", "");

    // Last character must not be punctuation
    result = result.replaceAll("['.-]+$", "");

    log.info("sanitisedName: " + result.trim());

    return result.trim();
  }

  public static String sanitiseNino(String nino) {
    if (nino == null) {
      return null;
    }

    String sanitisedNino;

    // Convert to uppercase & remove non-alphanumeric characters
    sanitisedNino = nino.toUpperCase(Locale.ROOT)
        .replaceAll("\\s+", "")
        .replaceAll("[^A-Z0-9]", "");

    if (!NINO_PATTERN.matcher(sanitisedNino).matches()) {
      log.error("Invalid NINO");
    }

    return sanitisedNino;
  }

  public static String sanitisePostcode(String postcode) {
    if (postcode == null) {
      return null;
    }

    String sanitisedPostcode = postcode.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", "")
        .replaceAll(" +", " ").trim();

    if (!POSTCODE_PATTERN.matcher(sanitisedPostcode).matches()) {
      log.error("Invalid postcode");
    }

    return sanitisedPostcode;
  }

}
