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

  public boolean canDisable(HttpServletRequest httpRequest) {
    return disableProblem(httpRequest) == DisableProblem.NONE;
  }

  public void beginReplacement(HttpServletRequest httpRequest) {
    twoFactorEnrollment.beginReplacement(httpRequest);
  }

  public DisableProblem disableProblem(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner factorOwner = twoFactorAuthorization.owner(httpRequest);
    TwoFactorPolicy twoFactorPolicy = twoFactorPolicies.current().policy();

    if (Arrays.stream(TwoFactorAction.values())
        .anyMatch(twoFactorAction ->
            twoFactorPolicy.mode(factorOwner.roles(), twoFactorAction) == TwoFactorMode.REQUIRED)) {
      return DisableProblem.REQUIRED;
    }

    if (!factorOwner.hasPassword()
        && !(providerRegistrations.providers().contains("microsoft")
            && externalIdentityService.connected(factorOwner.id()).contains("microsoft"))) {
      return DisableProblem.NO_INDEPENDENT_ACCESS;
    }

    return DisableProblem.NONE;
  }

  public long remainingCodes(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner factorOwner = twoFactorAuthorization.owner(httpRequest);
    twoFactorAuthorization.requireSession(httpRequest);
    return recoveryCodeRepository.remaining(factorOwner.id());
  }

  public UUID regenerate(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner factorOwner = twoFactorAuthorization.owner(httpRequest);
    UUID originalFactorRevision = twoFactorAuthorization.state(factorOwner).revision();
    twoFactorAuthorization.authorizeFactorGrant(
        httpRequest, TwoFactorAction.MANAGE_RECOVERY_CODES, true);

    UUID renewedFactorRevision = UUID.randomUUID();
    UUID recoveryOutputId =
        twoFactorStore.locked(
            factorOwner.id(),
            factorOwner.accessVersion(),
            factorAccountEntity -> {
              if (!factorAccountEntity.enrolled()
                  || !Objects.equals(originalFactorRevision, factorAccountEntity.getRevision())) {
                throw new AccessDeniedException("Two-factor account changed");
              }
              factorAccountEntity.setRevision(renewedFactorRevision);
              List<String> recoveryCodes = twoFactorStore.replaceRecoveryCodes(factorAccountEntity);
              TwoFactorChallengeEntity recoveryOutputEntity =
                  twoFactorEnrollment.recoveryOutput(
                      httpRequest, factorAccountEntity, recoveryCodes);
              twoFactorMail.securityChange(
                  factorAccountEntity.getUser(), TwoFactorMail.Event.RECOVERY_CODES);
              AfterCommitLog.info(log, "Recovery codes renewed: accountId={}", factorOwner.id());
              return recoveryOutputEntity.getId();
            });

    twoFactorAuthorization.retainAfterFactorChange(
        httpRequest, originalFactorRevision, renewedFactorRevision);
    sessionManagementServiceProvider.getObject().revokeOthers(httpRequest);

    return recoveryOutputId;
  }

  public void disable(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner factorOwner = twoFactorAuthorization.owner(httpRequest);
    UUID originalFactorRevision = twoFactorAuthorization.state(factorOwner).revision();
    twoFactorAuthorization.authorizeFactorGrant(httpRequest, TwoFactorAction.MANAGE_FACTOR, true);

    twoFactorStore.locked(
        factorOwner.id(),
        factorOwner.accessVersion(),
        factorAccountEntity -> {
          if (!canDisable(httpRequest)
              || !factorAccountEntity.enrolled()
              || !Objects.equals(originalFactorRevision, factorAccountEntity.getRevision())) {
            throw new AccessDeniedException("Required or changed authenticator cannot be removed");
          }
          factorAccountEntity.setEncryptedSecret(null);
          factorAccountEntity.setAcceptedStep(-1);
          factorAccountEntity.setRevision(UUID.randomUUID());
          recoveryCodeRepository.deleteForUser(factorOwner.id());
          twoFactorChallengeRepository.invalidateOwner(factorOwner.id());
          twoFactorMail.securityChange(factorAccountEntity.getUser(), TwoFactorMail.Event.DISABLED);
          AfterCommitLog.info(log, "Authenticator removed: accountId={}", factorOwner.id());
          return null;
        });

    twoFactorAuthorization.clearSessionProof(httpRequest);
    sessionManagementServiceProvider.getObject().revokeOthers(httpRequest);
  }
}
