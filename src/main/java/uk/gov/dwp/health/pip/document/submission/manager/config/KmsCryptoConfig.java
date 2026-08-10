package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import uk.gov.dwp.crypto.CryptoConfig;
import uk.gov.dwp.crypto.CryptoDataManager;
import uk.gov.dwp.crypto.exception.CryptoException;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.CryptoConfigProperties;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
public class KmsCryptoConfig {

  private final CryptoConfigProperties properties;

  @Autowired
  public KmsCryptoConfig(final CryptoConfigProperties properties) {
    this.properties = properties;
  }

  @Bean
  public CryptoDataManager cryptoDataManager() {
    log.info("Create crypto data manager bean for messaging");
    try {
      CryptoConfig config = createConfigurationOverride();
      config.setDataKeyId(this.properties.getMessageDataKeyId());
      return new CryptoDataManager(config);
    } catch (IOException
        | NoSuchAlgorithmException
        | InvalidKeyException
        | CryptoException
        | NoSuchPaddingException
        | IllegalBlockSizeException e) {
      final String msg =
          String.format("Failed to config DataCryptoManager for Messaging %s", e.getMessage());
      log.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  private CryptoConfig createConfigurationOverride()
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException,
          IllegalBlockSizeException {
    CryptoConfig config = new CryptoConfig();
    if (this.properties.getKmsOverride() != null && !this.properties.getKmsOverride().isBlank()) {
      config.setKmsEndpointOverride(this.properties.getKmsOverride());
    }
    if (this.properties.getRegion() != null && !this.properties.getRegion().isBlank()) {
      config.setRegion(Region.of(this.properties.getRegion()));
    }
    config.setCacheKmsDataKeys(this.properties.isKmsKeyCache());
    return config;
  }
}
