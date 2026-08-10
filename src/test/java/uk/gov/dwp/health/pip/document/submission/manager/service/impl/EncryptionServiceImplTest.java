package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dwp.crypto.CryptoDataManager;
import uk.gov.dwp.crypto.CryptoMessage;
import uk.gov.dwp.crypto.exception.CryptoException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.TaskException;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceImplTest {

  private final TestLogger testLogger =
      TestLoggerFactory.getTestLogger(EncryptionServiceImpl.class);
  @Captor ArgumentCaptor<String> strArgCaptor;
  @Captor ArgumentCaptor<CryptoMessage> msgArgCaptor;
  @Captor ArgumentCaptor<byte[]> byteArgCaptor;
  @InjectMocks private EncryptionServiceImpl cut;
  @Mock private CryptoDataManager cryptoDataManager;
  @Mock private Base64ServiceImpl base64Service;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(cut, "log", testLogger);
    testLogger.clearAll();
  }

  @Test
  void testEncryption() throws Exception {
    byte[] data = "content".getBytes();
    when(cryptoDataManager.encrypt(anyString())).thenReturn(new CryptoMessage());
    when(base64Service.byteArrayToBase64String(any())).thenReturn("encode-base64-content");
    cut.encrypt(data);

    InOrder order = inOrder(base64Service, cryptoDataManager);
    order.verify(base64Service).byteArrayToBase64String(byteArgCaptor.capture());
    assertThat(byteArgCaptor.getValue()).isEqualTo("content".getBytes());

    order.verify(cryptoDataManager).encrypt(strArgCaptor.capture());
    assertThat(strArgCaptor.getValue()).isEqualTo("encode-base64-content");
  }

  @Test
  void testEncryptionServiceThrowAwsKmsCryptoException() throws Exception {
    byte[] data = "content".getBytes();
    when(cryptoDataManager.encrypt(anyString())).thenThrow(CryptoException.class);
    when(base64Service.byteArrayToBase64String(any())).thenReturn("encoded-base64-content");
    assertThrows(TaskException.class, () -> cut.encrypt(data));
    assertThat(testLogger.getLoggingEvents().size()).isOne();
    verify(cryptoDataManager).encrypt(strArgCaptor.capture());
    assertThat(strArgCaptor.getValue()).isEqualTo("encoded-base64-content");
  }

  @Test
  void testDecryption() throws Exception {
    CryptoMessage cryptoMessage = mock(CryptoMessage.class);
    when(cryptoDataManager.decrypt(any(CryptoMessage.class)))
        .thenReturn("decrypted-encoded-base64");
    when(base64Service.base64StringToByteArray(anyString())).thenReturn("decrypted".getBytes());
    byte[] actual = cut.decrypt(cryptoMessage);

    InOrder order = Mockito.inOrder(cryptoDataManager, base64Service);
    order.verify(cryptoDataManager).decrypt(msgArgCaptor.capture());
    assertThat(msgArgCaptor.getValue()).isEqualTo(cryptoMessage);

    order.verify(base64Service).base64StringToByteArray(strArgCaptor.capture());
    assertThat(strArgCaptor.getValue()).isEqualTo("decrypted-encoded-base64");
    assertThat(actual).isEqualTo("decrypted".getBytes());
  }

  @Test
  void testDecryptServiceThrowAwsKmsCryptoException() throws Exception {
    CryptoMessage cryptoMessage = mock(CryptoMessage.class);
    when(cryptoDataManager.decrypt(any(CryptoMessage.class))).thenThrow(CryptoException.class);
    assertThrows(TaskException.class, () -> cut.decrypt(cryptoMessage));
    assertThat(testLogger.getLoggingEvents().size()).isOne();
    verify(cryptoDataManager).decrypt(cryptoMessage);
    verifyNoInteractions(base64Service);
  }
}
