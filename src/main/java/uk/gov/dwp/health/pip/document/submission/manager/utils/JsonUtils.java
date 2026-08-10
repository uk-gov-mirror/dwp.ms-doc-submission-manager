package uk.gov.dwp.health.pip.document.submission.manager.utils;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static <T> T readJsonFromFileAndMap(String path, Class<T> cls) throws IOException {
    return objectMapper.readValue(readJsonFromFile(path), cls);
  }

  public static String readJsonFromFile(String path) throws IOException {
    return Files.readString(Paths.get(path));
  }

  public static String mapToJson(Object obj)  {
    return objectMapper.writeValueAsString(obj);
  }
}
