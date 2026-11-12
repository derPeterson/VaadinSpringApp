package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import de.derpeterson.app.service.AccountLifecycleService.AccountAction;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AccountActionNoticeTest {
  @Test
  void noticeSurvivesLogoutInNewAnonymousSessionAndIsConsumedOnce() {
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    String previousSessionId = httpRequest.getSession().getId();
    httpRequest.getSession().invalidate();
    AccountActionNotice deletionNotice = new AccountActionNotice(AccountAction.DELETE, true, true);

    deletionNotice.storeAfterLogout(httpRequest);

    assertNotEquals(previousSessionId, httpRequest.getSession().getId());
    assertEquals(deletionNotice, AccountActionNotice.consume(httpRequest).orElseThrow());
    assertFalse(AccountActionNotice.consume(httpRequest).isPresent());
  }

  @Test
  void queryParameterCannotForgeSuccessAndReadingDoesNotCreateASession() {
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    httpRequest.setParameter("accountDeleted", "true");

    assertFalse(AccountActionNotice.consume(httpRequest).isPresent());
    assertEquals(null, httpRequest.getSession(false));
  }
}
