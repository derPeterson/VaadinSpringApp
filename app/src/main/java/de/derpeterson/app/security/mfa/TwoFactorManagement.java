package de.derpeterson.app.security.mfa;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.TwoFactorChallengeEntity;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.repository.TwoFactorRecoveryCodeRepository;
import de.derpeterson.app.security.ExternalLoginConfiguration;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Eigene Faktoren verwalten. Ein Ticket oder eine fremde Benutzer-ID kann keine Freigabe ersetzen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorManagement {
  public enum DisableProblem {
    NONE,
    REQUIRED,
    NO_INDEPENDENT_ACCESS
  }

  private final TwoFactorAuthorization twoFactorAuthorization;
  private final TwoFactorPolicies policies;
  private final TwoFactorStore factors;
  private final TwoFactorEnrollment enrollment;
  private final TwoFactorRecoveryCodeRepository recoveryCodes;
  private final TwoFactorChallengeRepository challenges;
  private final TwoFactorMail mail;
  private final ObjectProvider<SessionManagementService> sessions;
  private final ExternalIdentityService identities;
  private final ExternalLoginConfiguration.Registrations providers;

  public boolean canDisable(HttpServletRequest request) {
    return disableProblem(request) == DisableProblem.NONE;
  }

  public void beginReplacement(HttpServletRequest request) {
    enrollment.beginReplacement(request);
  }

  public DisableProblem disableProblem(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    TwoFactorPolicy policy = policies.current().policy();
    if (Arrays.stream(TwoFactorAction.values())
        .anyMatch(action -> policy.mode(owner.roles(), action) == TwoFactorMode.REQUIRED)) {
      return DisableProblem.REQUIRED;
    }
    if (!owner.hasPassword()
        && !(providers.providers().contains("microsoft")
            && identities.connected(owner.id()).contains("microsoft"))) {
      return DisableProblem.NO_INDEPENDENT_ACCESS;
    }
    return DisableProblem.NONE;
  }

  public long remainingCodes(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    twoFactorAuthorization.requireSession(request);
    return recoveryCodes.remaining(owner.id());
  }

  public UUID regenerate(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    UUID revision = twoFactorAuthorization.state(owner).revision();
    twoFactorAuthorization.authorizeFactorGrant(
        request, TwoFactorAction.MANAGE_RECOVERY_CODES, true);
    UUID renewedRevision = UUID.randomUUID();
    UUID output =
        factors.locked(
            owner.id(),
            owner.accessVersion(),
            account -> {
              if (!account.enrolled() || !Objects.equals(revision, account.getRevision())) {
                throw new AccessDeniedException("Two-factor account changed");
              }
              account.setRevision(renewedRevision);
              List<String> codes = factors.replaceRecoveryCodes(account);
              TwoFactorChallengeEntity document =
                  enrollment.recoveryOutput(request, account, codes);
              mail.securityChange(account.getUser(), TwoFactorMail.Event.RECOVERY_CODES);
              AfterCommitLog.info(log, "Recovery codes renewed: accountId={}", owner.id());
              return document.getId();
            });
    twoFactorAuthorization.retainAfterFactorChange(request, revision, renewedRevision);
    sessions.getObject().revokeOthers(request);
    return output;
  }

  public void disable(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    UUID revision = twoFactorAuthorization.state(owner).revision();
    twoFactorAuthorization.authorizeFactorGrant(request, TwoFactorAction.MANAGE_FACTOR, true);
    factors.locked(
        owner.id(),
        owner.accessVersion(),
        account -> {
          if (!canDisable(request)
              || !account.enrolled()
              || !Objects.equals(revision, account.getRevision())) {
            throw new AccessDeniedException("Required or changed authenticator cannot be removed");
          }
          account.setEncryptedSecret(null);
          account.setAcceptedStep(-1);
          account.setRevision(UUID.randomUUID());
          recoveryCodes.deleteForUser(owner.id());
          challenges.invalidateOwner(owner.id());
          mail.securityChange(account.getUser(), TwoFactorMail.Event.DISABLED);
          AfterCommitLog.info(log, "Authenticator removed: accountId={}", owner.id());
          return null;
        });
    twoFactorAuthorization.clearSessionProof(request);
    sessions.getObject().revokeOthers(request);
  }
}
