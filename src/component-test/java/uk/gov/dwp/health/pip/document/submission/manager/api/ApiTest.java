package uk.gov.dwp.health.pip.document.submission.manager.api;

import ch.qos.logback.classic.Level;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.pip.document.submission.manager.utils.MessageUtil;

import static io.restassured.RestAssured.given;

@Slf4j
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class ApiTest {
  protected static MessageUtil messageUtil;

  static RequestSpecification requestSpec;

  private static boolean loggingReconfigured = false;

  @BeforeAll
  static void setup() {
    reduceLoggerOutput();
    RestAssured.baseURI = getEnv("HOST", "http://localhost");
    RestAssured.port = Integer.parseInt(getEnv("PORT", "9945"));
    RestAssured.defaultParser = Parser.JSON;

    requestSpec =
        new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .build();

    var awsEndpointOverride = getEnv("AWS_ENDPOINT_OVERRIDE", "http://localhost:4566");
    var awsRegion = getEnv("AWS_REGION", "eu-west-2");

    messageUtil = new MessageUtil(awsEndpointOverride, awsRegion);
  }

  protected static String getEnv(String name, String defaultValue) {
    String env = System.getenv(name);
    return env == null ? defaultValue : env;
  }

  protected Response postRequest(String path, Object bodyPayload) {
    return given().spec(requestSpec).body(bodyPayload).when().post(path);
  }

  protected Response postRequestWithHeader(
      String path, Object bodyPayload, String headerName, String headerValue) {

    RequestSpecification requestSpecWithHeaders =
        new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .addHeader(headerName, headerValue)
            .build();

    return given().spec(requestSpecWithHeaders).body(bodyPayload).when().post(path);
  }

  protected Response getRequest(String path) {
    return given().spec(requestSpec).when().get(path);
  }

  private static void reduceLoggerOutput() {
    if (!loggingReconfigured) {
      loggingReconfigured = true;
      reduceLoggerOutput("org.springframework.data.convert.CustomConversions");
      reduceLoggerOutput("org.mongodb.driver.client");
      reduceLoggerOutput("org.mongodb.driver.cluster");
      reduceLoggerOutput("org.mongodb.driver.connection");
    }
  }

  private static void reduceLoggerOutput(final String name) {
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name)).setLevel(Level.ERROR);
  }
}
