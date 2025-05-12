package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsAccountConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.service.AccountManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
class AccountManagerServiceTests {

  private RestTemplate restTemplate;

  private AccountManagerService sut;

  private MockRestServiceServer mockServer;

  @Mock
  private MsAccountConfig msAccountConfig;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void accountExistsInAccMgr_getAccountMgrData_returnsEmailAddress()
      throws IOException, URISyntaxException {

    var response = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/dto/accountMgrResponse.json", V7AccountDetails[].class);

    when(msAccountConfig.getAccountMgrDataUri())
        .thenReturn("http://www.teststring.com/v4/account/details/id/{accountId}");

    sut = new AccountManagerService(msAccountConfig, restTemplate, new ObjectMapper());

    mockServer.expect(ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/account/details/id/123456789")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK)
            .body(JsonUtils.mapToJson(response))
        );

    var result = sut.getAccountMgrData("123456789");
    assertEquals(0, result.getFailures().size());
    assertEquals("testing@test.com", result.getValue().getEmail());
  }

  @Test
  void applicationDoesntExistInAccMgr_getAccountMgrData_returnsNotFound()
      throws URISyntaxException, JsonProcessingException {

    when(msAccountConfig.getAccountMgrDataUri())
        .thenReturn("http://www.teststring.com/v4/account/details/id/{accountId}");

    sut = new AccountManagerService(msAccountConfig, restTemplate, new ObjectMapper());

    mockServer.expect(ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/account/details/id/123")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND)
        );

    var result = sut.getAccountMgrData("123");
    assertEquals(1, result.getFailures().size());
    assertEquals("Data for claimant with ID: 123 not found in Account Manager.",
        result.getFailures().get(0).getFailureReason());
  }

  @Test
  void applicationDoesntExistInAccMgr_getAccountMgrData_returnsEmptyList()
      throws URISyntaxException, JsonProcessingException {

    when(msAccountConfig.getAccountMgrDataUri())
        .thenReturn("http://www.teststring.com/v4/account/details/id/{accountId}");

    sut = new AccountManagerService(msAccountConfig, restTemplate, new ObjectMapper());

    mockServer.expect(ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/account/details/id/123456789")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK)
            .body("[]")
        );

    var result = sut.getAccountMgrData("123456789");
    assertEquals(1, result.getFailures().size());
    assertEquals("Data for claimant with ID: 123456789 not found in Account Manager.",
        result.getFailures().get(0).getFailureReason());
  }

  @Test
  void applicationDoesntExistInAccMgr_getAccountMgrData_returnsFailure()
      throws URISyntaxException, JsonProcessingException {

    when(msAccountConfig.getAccountMgrDataUri())
        .thenReturn("http://www.teststring.com/v4/account/details/id/{accountId}");

    sut = new AccountManagerService(msAccountConfig, restTemplate, new ObjectMapper());

    mockServer.expect(ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/account/details/id/123")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST)
        );

    var result = sut.getAccountMgrData("123");
    assertEquals(1, result.getFailures().size());
  }
}
