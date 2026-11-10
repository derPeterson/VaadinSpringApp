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
  private final TwoFactorPolicies twoFactorPolicies;
  private final TwoFactorStore twoFactorStore;
  private final TwoFactorEnrollment twoFactorEnrollment;
  private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;
  private final TwoFactorChallengeRepository twoFactorChallengeRepository;
  private final TwoFactorMail twoFactorMail;
  private final ObjectProvider<SessionManagementService> sessionManagementServiceProvider;
  private final ExternalIdentityService externalIdentityService;
  private final ExternalLoginConfiguration.Registrations providerRegistrations;

  public boolean canDisable(HttpServletRequest request) {
    return disableProblem(request) == DisableProblem.NONE;
  }

  public void beginReplacement(HttpServletRequest request) {
    twoFactorEnrollment.beginReplacement(request);
  }

  public DisableProblem disableProblem(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    TwoFactorPolicy policy = twoFactorPolicies.current().policy();
    if (Arrays.stream(TwoFactorAction.values())
        .anyMatch(action -> policy.mode(owner.roles(), action) == TwoFactorMode.REQUIRED)) {
      return DisableProblem.REQUIRED;
    }
    if (!owner.hasPassword()
        && !(providerRegistrations.providers().contains("microsoft")
            && externalIdentityService.connected(owner.id()).contains("microsoft"))) {
      return DisableProblem.NO_INDEPENDENT_ACCESS;
    }
    return DisableProblem.NONE;
  }

  public long remainingCodes(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    twoFactorAuthorization.requireSession(request);
    return recoveryCodeRepository.remaining(owner.id());
  }

  public UUID regenerate(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    UUID revision = twoFactorAuthorization.state(owner).revision();
    twoFactorAuthorization.authorizeFactorGrant(
        request, TwoFactorAction.MANAGE_RECOVERY_CODES, true);
    UUID renewedRevision = UUID.randomUUID();
    UUID output =
        twoFactorStore.locked(
            owner.id(),
            owner.accessVersion(),
            account -> {
              if (!account.enrolled() || !Objects.equals(revision, account.getRevision())) {
                throw new AccessDeniedException("Two-factor account changed");
              }
              account.setRevision(renewedRevision);
              List<String> codes = twoFactorStore.replaceRecoveryCodes(account);
              TwoFactorChallengeEntity document =
                  twoFactorEnrollment.recoveryOutput(request, account, codes);
              twoFactorMail.securityChange(account.getUser(), TwoFactorMail.Event.RECOVERY_CODES);
              AfterCommitLog.info(log, "Recovery codes renewed: accountId={}", owner.id());
              return document.getId();
            });
    twoFactorAuthorization.retainAfterFactorChange(request, revision, renewedRevision);
    sessionManagementServiceProvider.getObject().revokeOthers(request);
    return output;
  }

  public void disable(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(request);
    UUID revision = twoFactorAuthorization.state(owner).revision();
    twoFactorAuthorization.authorizeFactorGrant(request, TwoFactorAction.MANAGE_FACTOR, true);
    twoFactorStore.locked(
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
          recoveryCodeRepository.deleteForUser(owner.id());
          twoFactorChallengeRepository.invalidateOwner(owner.id());
          twoFactorMail.securityChange(account.getUser(), TwoFactorMail.Event.DISABLED);
          AfterCommitLog.info(log, "Authenticator removed: accountId={}", owner.id());
          return null;
        });
    twoFactorAuthorization.clearSessionProof(request);
    sessionManagementServiceProvider.getObject().revokeOthers(request);
  }
}
