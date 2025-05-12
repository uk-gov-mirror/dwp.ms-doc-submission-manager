package uk.gov.dwp.health.pip.document.submission.manager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static <T> T readJsonFromFileAndMap(String path, Class<T> cls) throws IOException {
    return objectMapper.readValue(readJsonFromFile(path), cls);
  }

  public static String readJsonFromFile(String path) throws IOException {
    return Files.readString(Paths.get(path));
  }

  public static String mapToJson(Object obj) throws JsonProcessingException {
    return objectMapper.writeValueAsString(obj);
  }
}
