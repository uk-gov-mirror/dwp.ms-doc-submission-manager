package uk.gov.dwp.health.pip.document.submission.manager.entity.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.BooleanResponseDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.MultiPartResponseDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RadioResponseDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ShortTextResponseDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.TextAreaResponseDto;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionType.BOOL_QUESTION;
import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionType.MULTI_PART_QUESTION;
import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionType.RADIO_QUESTION;
import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionType.SHORT_TEXT_QUESTION;
import static uk.gov.dwp.health.pip.document.submission.manager.openapi.model.QuestionType.TEXT_AREA_QUESTION;

class QuestionAnswerTests {

  private static final String BASE_PATH = "src/test/resources/entity/questionAnswer/";

  @ParameterizedTest
  @MethodSource("provideParameters")
  void willMapToCorrectResponseClass(String path, Class cls) throws IOException {
    var response = JsonUtils.readJsonFromFileAndMap(path, cls);

    assertNotNull(response);
    assertEquals(response.getClass(), cls);
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(BASE_PATH + "booleanResponse.json",
            BooleanResponseDto.class),
        Arguments.of(BASE_PATH + "textAreaResponse.json",
            TextAreaResponseDto.class),
        Arguments.of(BASE_PATH + "shortTextResponse.json",
            ShortTextResponseDto.class),
        Arguments.of(BASE_PATH + "radioResponse.json",
            RadioResponseDto.class),
        Arguments.of(BASE_PATH + "multiPartResponse.json",
            MultiPartResponseDto.class)
    );
  }

  @Test
  void willExposeProperties_BooleanResponse() throws IOException {
    var response = JsonUtils.readJsonFromFileAndMap(BASE_PATH + "booleanResponse.json",
        BooleanResponseDto.class);

    assertTrue(response.isResponse());
    assertEquals("moving-around-affected", response.getReference());
    assertEquals("Does your condition affect you moving around?", response.getQuestion());
    assertEquals(BOOL_QUESTION, response.getQuestionType());
  }

  @Test
  void willExposeProperties_TextAreaResponse() throws IOException {
    var response = JsonUtils.readJsonFromFileAndMap(BASE_PATH + "textAreaResponse.json",
        TextAreaResponseDto.class);

    assertTrue(response.getResponse().contains("I can only walk down one aisle"));
    assertEquals("moving-around-info", response.getReference());
    assertTrue(response.getQuestion().contains("Tell us more about the difficulties"));
    assertEquals(TEXT_AREA_QUESTION, response.getQuestionType());
  }

  @Test
  void willExposeProperties_ShortTextResponse() throws IOException {
    var response = JsonUtils.readJsonFromFileAndMap(BASE_PATH + "shortTextResponse.json",
        ShortTextResponseDto.class);

    assertTrue(response.getResponse().contains("Amputation"));
    assertEquals("health-condition-name", response.getReference());
    assertTrue(response.getQuestion().contains("Name of your first condition or disability"));
    assertEquals(SHORT_TEXT_QUESTION, response.getQuestionType());
  }

  @Test
  void willExposeProperties_RadioResponse() throws IOException {
    var response = JsonUtils.readJsonFromFileAndMap(BASE_PATH + "radioResponse.json",
        RadioResponseDto.class);

    assertEquals("variable", response.getResponseReference());
    assertEquals("moving-around-aids", response.getReference());
    assertEquals("How far can you walk using any aids or appliances you need?",
        response.getQuestion());
    assertEquals(RADIO_QUESTION, response.getQuestionType());
  }

  @Test
  void willExposeProperties_MultiPartResponse() throws IOException {
    var response = JsonUtils.readJsonFromFileAndMap(BASE_PATH +
        "multiPartResponse.json", MultiPartResponseDto.class);

    assertEquals(2, response.getResponses().size());
    assertEquals("health-condition-new", response.getReference());
    assertEquals("What health condition or disability do you have?",
        response.getQuestion());
    assertEquals(MULTI_PART_QUESTION, response.getQuestionType());
  }

}
