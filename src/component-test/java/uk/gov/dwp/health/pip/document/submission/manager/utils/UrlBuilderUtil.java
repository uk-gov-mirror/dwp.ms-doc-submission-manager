package uk.gov.dwp.health.pip.document.submission.manager.utils;

import static io.restassured.RestAssured.baseURI;

public class UrlBuilderUtil {

  public static String postApplyUrlV3() {
    return baseURI + "/v3/apply";
  }

  public static String postAttachUrl() {
    return baseURI + "/v1/attach";
  }

  public static String postAdministrationResubmitUrl() {
    return baseURI + "/v1/administration/resubmit";
  }

  public static String getStatusUrl(String requestId) {
    return baseURI + "/v1/status/" + requestId;
  }

  public static String getReportUrl(String dayOrWeek) {
    return baseURI + "/v1/administration/report/" + dayOrWeek;
  }
}
