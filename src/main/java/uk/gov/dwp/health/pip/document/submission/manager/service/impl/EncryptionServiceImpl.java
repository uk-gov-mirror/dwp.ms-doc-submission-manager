package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.dwp.crypto.CryptoDataManager;
import uk.gov.dwp.crypto.CryptoMessage;
import uk.gov.dwp.crypto.exception.CryptoException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.TaskException;
import uk.gov.dwp.health.pip.document.submission.manager.service.Base64Service;
import uk.gov.dwp.health.pip.document.submission.manager.service.EncryptionService;

@Service
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService<byte[], CryptoMessage> {

  private static Logger log = LoggerFactory.getLogger(EncryptionServiceImpl.class);
  private final CryptoDataManager cryptoDataManager;
  private final Base64Service base64Service;

  @Override
  public CryptoMessage encrypt(final byte[] content) {
    try {
      return cryptoDataManager.encrypt(base64Service.byteArrayToBase64String(content));
    } catch (CryptoException e) {
      final String message = String.format("Fail encrypt file content with KMS %s", e.getMessage());
      log.error(message);
      throw new TaskException(message);
    }
  }

  @Override
  public byte[] decrypt(final CryptoMessage cipher) {
    try {
      return base64Service.base64StringToByteArray(cryptoDataManager.decrypt(cipher));
    } catch (CryptoException e) {
      final String message = String.format("Fail decrypt file content with KMS %s", e.getMessage());
      log.error(message);
      throw new TaskException(message);
    }
  }
}
