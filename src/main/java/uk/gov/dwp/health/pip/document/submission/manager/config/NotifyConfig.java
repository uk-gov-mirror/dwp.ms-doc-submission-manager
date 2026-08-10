package uk.gov.dwp.health.pip.document.submission.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.crypto.SecureStrings;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.NotifyConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.exception.SecureStringException;

import javax.crypto.NoSuchPaddingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static uk.gov.dwp.health.pip.document.submission.manager.utils.ProxyPropsValidation.proxyHostNotBlank;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.ProxyPropsValidation.proxyPortNotBlank;


@Configuration
public class NotifyConfig {

  @Bean("notifyNetProxy")
  public Proxy notifyNetProxy(final NotifyConfigProperties config) {
    return proxyHostNotBlank().and(proxyPortNotBlank()).apply(config)
            ? new Proxy(
            Proxy.Type.HTTP, new InetSocketAddress(config.getProxyHost(), config.getProxyPort()))
            : null;
  }

  @Bean
  public SecureStrings secureStrings() {
    try {
      return new SecureStrings();
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException ex) {
      throw new SecureStringException("Unable to configure secure string to encrypt password");
    }
  }
}
