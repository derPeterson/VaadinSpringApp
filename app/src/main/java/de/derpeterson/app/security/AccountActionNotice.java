package de.derpeterson.app.security;

import de.derpeterson.app.service.AccountLifecycleService.AccountAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Optional;

/** Einmalige Rückmeldung in einer neuen anonymen Sitzung nach dem Logout. */
public record AccountActionNotice(
    AccountAction accountAction, boolean committed, boolean mailQueued) implements Serializable {
  private static final String SESSION_ATTRIBUTE = AccountActionNotice.class.getName();

  public void storeAfterLogout(HttpServletRequest httpServletRequest) {
    httpServletRequest.getSession(true).setAttribute(SESSION_ATTRIBUTE, this);
  }

  public static Optional<AccountActionNotice> consume(HttpServletRequest httpServletRequest) {
    HttpSession anonymousSession = httpServletRequest.getSession(false);
    if (anonymousSession == null) {
      return Optional.empty();
    }

    Object storedNotice = anonymousSession.getAttribute(SESSION_ATTRIBUTE);
    anonymousSession.removeAttribute(SESSION_ATTRIBUTE);
    return storedNotice instanceof AccountActionNotice accountActionNotice
        ? Optional.of(accountActionNotice)
        : Optional.empty();
  }
}
