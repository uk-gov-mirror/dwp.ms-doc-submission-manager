package uk.gov.dwp.health.pip.document.submission.manager.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DrsMetadataValidationUtilsTest {

  @Test
  void shouldSanitiseValidName() {
    assertEquals(
        "John",
        DrsMetadataValidationUtils.sanitiseName("John")
    );
  }

  @Test
  void shouldRemoveInvalidCharactersFromName() {
    assertEquals(
        "Smith",
        DrsMetadataValidationUtils.sanitiseName("123@_ Smith!")
    );
  }

  @Test
  void shouldNormaliseSpacesAndPunctuationInName() {
    assertEquals(
        "Anne-Marie",
        DrsMetadataValidationUtils.sanitiseName("  Anne -- Marie   ''  ")
    );
  }

  @Test
  void shouldReturnEmptyStringWhenNameContainsNoLetters() {
    assertEquals(
        "",
        DrsMetadataValidationUtils.sanitiseName("123@£---")
    );
  }

  @Test
  void shouldReturnNullWhenNameIsNull() {
    assertNull(DrsMetadataValidationUtils.sanitiseName(null));
  }

  @Test
  void shouldSanitiseValidNino() {
    assertEquals(
        "AB123456C",
        DrsMetadataValidationUtils.sanitiseNino("AB123456C")
    );
  }

  @Test
  void shouldUppercaseAndRemoveSpacesFromNino() {
    assertEquals(
        "AB123456C",
        DrsMetadataValidationUtils.sanitiseNino("ab 12 34 56 c")
    );
  }

  @Test
  void shouldRemoveInvalidCharactersFromNino() {
    assertEquals(
        "AB123456C",
        DrsMetadataValidationUtils.sanitiseNino("A@B-12.34/56-C")
    );
  }

  @Test
  void shouldReturnEmptyStringWhenNinoContainsNoAllowedCharacters() {
    assertEquals(
        "",
        DrsMetadataValidationUtils.sanitiseNino("@£$%")
    );
  }

  @Test
  void shouldReturnNullWhenNinoIsNull() {
    assertNull(DrsMetadataValidationUtils.sanitiseNino(null));
  }

  @Test
  void shouldSanitiseValidPostcode() {
    assertEquals(
        "NE26 4RS",
        DrsMetadataValidationUtils.sanitisePostcode("NE26 4RS")
    );
  }

  @Test
  void shouldUppercasePostcode() {
    assertEquals(
        "NE26 4RS",
        DrsMetadataValidationUtils.sanitisePostcode("ne26 4rs")
    );
  }

  @Test
  void shouldPreservePostcodeWithoutSpace() {
    assertEquals(
        "NE264RS",
        DrsMetadataValidationUtils.sanitisePostcode("ne264rs")
    );
  }

  @Test
  void shouldCollapseMultiplePostcodeSpaces() {
    assertEquals(
        "NE26 4RS",
        DrsMetadataValidationUtils.sanitisePostcode("NE26    4RS")
    );
  }

  @Test
  void shouldRemoveLeadingAndTrailingPostcodeSpaces() {
    assertEquals(
        "NE26 4RS",
        DrsMetadataValidationUtils.sanitisePostcode("   NE26 4RS   ")
    );
  }

  @Test
  void shouldRemoveInvalidCharactersFromPostcode() {
    assertEquals(
        "NE26 4RS",
        DrsMetadataValidationUtils.sanitisePostcode("N!E2£6 4R$S")
    );
  }

  @Test
  void shouldNotMoveAnExistingPostcodeSpace() {
    assertEquals(
        "NE2 64RS",
        DrsMetadataValidationUtils.sanitisePostcode("NE2 64RS")
    );
  }

  @Test
  void shouldReturnEmptyStringWhenPostcodeContainsNoAllowedCharacters() {
    assertEquals(
        "",
        DrsMetadataValidationUtils.sanitisePostcode("@£$%")
    );
  }

  @Test
  void shouldReturnNullWhenPostcodeIsNull() {
    assertNull(DrsMetadataValidationUtils.sanitisePostcode(null));
  }

}
