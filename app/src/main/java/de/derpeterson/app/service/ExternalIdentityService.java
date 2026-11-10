package de.derpeterson.app.service;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.ExternalIdentityEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.ExternalIdentityRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.DeviceTokenRepository;
import de.derpeterson.app.security.ExternalIdentityEvidence;
import de.derpeterson.app.security.ExternalLoginConfiguration;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.PortalUserDetails;
import de.derpeterson.app.security.SignInMethod;
import de.derpeterson.app.security.session.DurableSessionRegistry;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/** Anbieteridentität und lokale Kontorechte bleiben getrennt; E-Mail verbindet niemals Konten. */
@Service
@RequiredArgsConstructor
public class ExternalIdentityService {
  private final ExternalIdentityRepository externalIdentityRepository;
  private final UserRepository userRepository;
  private final VerificationService verificationService;
  private final ConfigService configService;
  private final SessionRegistry sessionRegistry;
  private final DeviceTokenRepository deviceTokenRepository;
  private final ExternalLoginConfiguration.Registrations providerRegistrations;
  private final PlatformTransactionManager transactionManager;
  private final AdminRolePolicy rolePolicy;

  public record SignInSummary(RegistrationChannel registration, Set<SignInMethod> available) {}

  private record Unlinked(
      String identity, String previousVersion, String newVersion, Set<SignInMethod> remaining) {}

  @Transactional(readOnly = true)
  public Optional<UserEntity> resolve(ExternalIdentityEvidence evidence) {
    return externalIdentityRepository
        .findByProviderAndIssuerAndSubject(
            evidence.provider(), evidence.issuer(), evidence.subject())
        .map(identity -> {
          UserEntity user = identity.getUser();
          requireEnabled(user);
          return user;
        });
  }

  @Transactional(rollbackFor = IOException.class)
  public UserEntity register(
      ExternalIdentityEvidence evidence,
      String email,
      String firstName,
      String lastName,
      Locale locale)
      throws IOException {
    MaintenanceService.requirePublicAccess(configService);
    // Dieselbe globale Reihenfolge wie die bestehende Anlage. Eindeutige DB-Schlüssel sichern
    // zusätzlich gegen konkurrierende Anbieterzuordnungen und abweichende Aufrufer.
    userRepository.lockRole(RoleType.ROLE_SUPERADMIN);
    Assert.isTrue(UserInputRules.isEmailValid(email), "Invalid email");
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(firstName) && firstName.trim().length() <= 255,
        "Invalid first name");
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(lastName) && lastName.trim().length() <= 255,
        "Invalid last name");
    Assert.isTrue(
        Locale.GERMAN.equals(locale) || Locale.ENGLISH.equals(locale), "Unsupported language");
    String canonical = EmailIdentity.canonicalize(email);
    if (userRepository.findByEmail(canonical).isPresent()
        || externalIdentityRepository
            .findByProviderAndIssuerAndSubject(
                evidence.provider(), evidence.issuer(), evidence.subject())
            .isPresent()) {
      throw new AccessDeniedException("Sign in to the existing account before linking");
    }
    boolean verified =
        evidence.trustedEmail() && canonical.equals(EmailIdentity.canonicalize(evidence.email()));
    UserEntity user =
        UserEntity.builder()
            .email(canonical)
            .registrationChannel(
                RegistrationChannel.valueOf(evidence.provider().toUpperCase(Locale.ROOT)))
            .firstName(firstName.trim())
            .lastName(lastName.trim())
            .preferredLocale(locale)
            .enabled(verified)
            .verificationPending(!verified)
            .roleEntities(List.of(userRepository.lockRole(RoleType.ROLE_USER).orElseThrow()))
            .build();
    userRepository.saveAndFlush(user);
    add(user, evidence);
    if (!verified && !verificationService.sendVerificationEmailByUser(user)) {
      throw new IllegalStateException("Verification mail could not be queued");
    }
    return user;
  }

  @Transactional
  public void link(Long userId, String expectedVersion, ExternalIdentityEvidence evidence) {
    MaintenanceService.requirePublicAccess(configService);
    UserEntity user = locked(userId, expectedVersion);
    if (externalIdentityRepository.findByUserIdAndProvider(userId, evidence.provider()).isPresent()
        || externalIdentityRepository
            .findByProviderAndIssuerAndSubject(
                evidence.provider(), evidence.issuer(), evidence.subject())
            .isPresent()) {
      throw new AccessDeniedException("Provider already connected");
    }
    add(user, evidence);
  }

  /** Behält nur die aktuelle Sitzung mit nachweislich weiter nutzbarem Anmeldeweg. */
  public boolean unlink(
      HttpServletRequest request, Long userId, String expectedVersion, String provider) {
    HttpSession current = request.getSession(false);
    String sessionId = current == null ? null : current.getId();
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    Unlinked unlinked =
        Objects.requireNonNull(
            transaction.execute(status -> removeProvider(userId, expectedVersion, provider)));
    boolean kept =
        sessionId != null
            && sessionRegistry instanceof DurableSessionRegistry<?> durable
            && durable.retainAfterProviderUnlink(
                sessionId,
                unlinked.identity(),
                unlinked.previousVersion(),
                unlinked.newVersion(),
                unlinked.remaining());
    sessionRegistry.getAllSessions(unlinked.identity(), true).stream()
        .filter(session -> !kept || !sessionId.equals(session.getSessionId()))
        .forEach(SessionInformation::expireNow);
    return kept;
  }

  private Unlinked removeProvider(Long userId, String expectedVersion, String provider) {
    MaintenanceService.requirePublicAccess(configService);
    UserEntity user = locked(userId, expectedVersion);
    ExternalIdentityEntity identity =
        externalIdentityRepository
            .findByUserIdAndProvider(userId, provider)
            .orElseThrow(() -> new AccessDeniedException("Provider unavailable"));
    Set<SignInMethod> remaining = availableMethods(user);
    remaining.remove(SignInMethod.provider(provider));
    if (remaining.isEmpty()) {
      throw new AccessDeniedException("Last sign-in method must remain");
    }
    externalIdentityRepository.delete(identity);
    externalIdentityRepository.flush();
    String previousVersion =
        PortalUserDetails.credentialVersion(user.getAccessVersion(), user.getPassword());
    // Alte Sitzungen und Nachweise verlieren mit dem Commit ihre Zugangsversion.
    user.rotateAccessVersion();
    userRepository.saveAndFlush(user);
    deviceTokenRepository.removeUserTokens(user.getEmail());
    return new Unlinked(
        user.getEmail(),
        previousVersion,
        PortalUserDetails.credentialVersion(user.getAccessVersion(), user.getPassword()),
        remaining);
  }

  @Transactional(readOnly = true)
  public Set<SignInMethod> availableSignInMethods(Long userId) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    return Set.copyOf(availableMethods(user));
  }

  @Transactional(readOnly = true)
  public Optional<SignInSummary> adminSignInSummary(Long userId) {
    rolePolicy.authorizeMutation(userId, Set.of());
    return userRepository
        .findById(userId)
        .map(user ->
            new SignInSummary(user.getRegistrationChannel(), Set.copyOf(availableMethods(user))));
  }

  private Set<SignInMethod> availableMethods(UserEntity accountUserEntity) {
    Set<SignInMethod> availableSignInMethods = EnumSet.noneOf(SignInMethod.class);
    if (accountUserEntity.getPassword() != null) {
      availableSignInMethods.add(SignInMethod.PORTAL);
    }

    List<String> enabledProviderIds = providerRegistrations.providers();
    for (String connectedProviderId : connectedProviders(accountUserEntity.getId())) {
      if (enabledProviderIds.contains(connectedProviderId)) {
        availableSignInMethods.add(SignInMethod.provider(connectedProviderId));
      }
    }

    return availableSignInMethods;
  }

  @Transactional(readOnly = true)
  public List<String> connected(Long userId) {
    return connectedProviders(userId);
  }

  private List<String> connectedProviders(Long userId) {
    return externalIdentityRepository.findByUserIdOrderByProvider(userId).stream()
        .map(ExternalIdentityEntity::getProvider)
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean belongsTo(Long userId, ExternalIdentityEvidence evidence) {
    return externalIdentityRepository
        .findByProviderAndIssuerAndSubject(
            evidence.provider(), evidence.issuer(), evidence.subject())
        .map(identity -> Objects.equals(identity.getUser().getId(), userId))
        .orElse(false);
  }

  private UserEntity locked(Long id, String version) {
    UserEntity user =
        userRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    requireEnabled(user);
    if (!Objects.equals(version, user.getAccessVersion())) {
      throw new AccessDeniedException("Account access changed");
    }
    return user;
  }

  private void add(UserEntity user, ExternalIdentityEvidence evidence) {
    ExternalIdentityEntity identity = new ExternalIdentityEntity();
    identity.setUser(user);
    identity.setProvider(evidence.provider());
    identity.setIssuer(evidence.issuer());
    identity.setSubject(evidence.subject());
    externalIdentityRepository.saveAndFlush(identity);
  }

  private static void requireEnabled(UserEntity user) {
    if (!user.isEnabled() || user.isVerificationPending()) {
      throw new DisabledException("Account not available for sign-in");
    }
  }
}
