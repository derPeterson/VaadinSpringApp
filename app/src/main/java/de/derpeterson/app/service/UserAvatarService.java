package de.derpeterson.app.service;

import de.derpeterson.app.model.UserAvatarEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.repository.UserAvatarRepository;
import de.derpeterson.app.repository.UserAvatarRepository.Metadata;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AvatarImageProcessor.Crop;
import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.AvatarImageProcessor.Problem;
import de.derpeterson.app.service.AvatarImageProcessor.SourceMetadata;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class UserAvatarService {
  private record ImageReplacement(byte[] image, Instant uploadedAt, SourceMetadata original) {
    @Override
    public boolean equals(Object other) {
      return other
              instanceof
              ImageReplacement(
                  byte[] otherImage,
                  Instant otherUploadedAt,
                  SourceMetadata otherOriginal)
          && Arrays.equals(image, otherImage)
          && Objects.equals(uploadedAt, otherUploadedAt)
          && Objects.equals(original, otherOriginal);
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(image), uploadedAt, original);
    }

    @Override
    public String toString() {
      return "Avatar image replacement";
    }
  }

  public record State(Long version, AvatarMode mode, boolean hasImage, UUID imageRevision)
      implements Serializable {
    public static State initial() {
      return new State(null, AvatarMode.INITIALS, false, null);
    }
  }

  public record Open(State state, UUID draft) {}

  public record Change(
      Long version, AvatarMode mode, boolean deleteImage, UUID draft, UUID imageId, Crop crop) {}

  private final UserAvatarRepository userAvatarRepository;
  private final UserRepository userRepository;
  private final SecurityService securityService;
  private final ConfigService configService;
  private final AvatarImageProcessor avatarImageProcessor;
  private final AvatarDraftStore avatarDraftStore;
  private final UserProfileBroadcaster userProfileBroadcaster;
  private final TransactionTemplate transactionTemplate;

  public UserAvatarService(
      UserAvatarRepository userAvatarRepository,
      UserRepository userRepository,
      SecurityService securityService,
      ConfigService configService,
      AvatarImageProcessor avatarImageProcessor,
      AvatarDraftStore avatarDraftStore,
      UserProfileBroadcaster userProfileBroadcaster,
      PlatformTransactionManager manager) {
    this.userAvatarRepository = userAvatarRepository;
    this.userRepository = userRepository;
    this.securityService = securityService;
    this.configService = configService;
    this.avatarImageProcessor = avatarImageProcessor;
    this.avatarDraftStore = avatarDraftStore;
    this.userProfileBroadcaster = userProfileBroadcaster;
    transactionTemplate = new TransactionTemplate(manager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  // Interne UI-Metadaten, ausdrücklich ohne BLOBs; Grid lädt eine sichtbare Seite gesammelt.
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Map<Long, State> states(Collection<Long> ids) {
    return readStates(ids);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public State state(Long userId) {
    return readState(userId);
  }

  private State readState(Long userId) {
    return userId == null
        ? State.initial()
        : readStates(List.of(userId)).getOrDefault(userId, State.initial());
  }

  // Aufrufer besitzt die passende Transaktion. open verwendet seine laufende Transaktion,
  // öffentliche Einzel-/Sammelabfragen erhalten ihre unabhängige Lesetransaktion am Proxy.
  private Map<Long, State> readStates(Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    HashMap<Long, State> result = new HashMap<Long, State>();
    for (Metadata row : userAvatarRepository.metadata(ids)) {
      result.put(
          row.getUserId(),
          new State(row.getVersion(), row.getMode(), row.getHasImage(), row.getImageRevision()));
    }
    return result;
  }

  public Open open(HttpServletRequest request) {
    return transactionTemplate.execute(status -> {
      UserEntity user = requireUser(request, false);
      return new Open(readState(user.getId()), avatarDraftStore.begin(user.getId()));
    });
  }

  public UUID upload(
      HttpServletRequest httpRequest, UUID avatarDraftId, InputStream uploadInputStream)
      throws IOException {
    Long avatarOwnerUserId =
        transactionTemplate.execute(status -> requireUser(httpRequest, false).getId());

    return avatarDraftStore.upload(avatarDraftId, avatarOwnerUserId, uploadInputStream);
  }

  public State save(HttpServletRequest httpRequest, Change avatarChange) {
    if (avatarChange == null || avatarChange.mode() == null) {
      throw new InvalidImage(Problem.MISSING);
    }

    Long avatarOwnerUserId =
        transactionTemplate.execute(status -> requireUser(httpRequest, false).getId());
    // Original/Decode niemals unter der späteren Kontosperre halten.
    ImageReplacement imageReplacement = prepareReplacement(avatarOwnerUserId, avatarChange);

    State savedAvatarState =
        transactionTemplate.execute(status ->
            saveLockedAvatar(httpRequest, avatarOwnerUserId, avatarChange, imageReplacement));

    log.debug(
        "Account avatar updated: userId={}, mode={}, hasImage={}",
        avatarOwnerUserId,
        savedAvatarState.mode(),
        savedAvatarState.hasImage());

    avatarDraftStore.release(avatarChange.draft());
    return savedAvatarState;
  }

  private ImageReplacement prepareReplacement(Long avatarOwnerUserId, Change avatarChange) {
    if (avatarChange.imageId() == null) {
      return null;
    }

    AvatarDraftStore.Image originalAvatarImage =
        avatarDraftStore.image(avatarChange.draft(), avatarOwnerUserId, avatarChange.imageId());
    byte[] normalizedAvatarBytes =
        avatarImageProcessor.normalize(originalAvatarImage.bytes(), avatarChange.crop());
    return new ImageReplacement(
        normalizedAvatarBytes, originalAvatarImage.uploadedAt(), originalAvatarImage.metadata());
  }

  // Ausschließlich innerhalb der bestehenden Schreibtransaktion und Kontosperre aufrufen.
  private State saveLockedAvatar(
      HttpServletRequest request, Long owner, Change change, ImageReplacement replacement) {
    UserEntity user = requireUser(request, true);
    if (!owner.equals(user.getId())) {
      throw new AccessDeniedException("Identity changed");
    }
    UserAvatarEntity row = userAvatarRepository.findById(owner).orElse(null);
    if (!Objects.equals(change.version(), row == null ? null : row.getVersion())) {
      throw new OptimisticLockingFailureException("Avatar changed; reopen dialog");
    }
    if (row == null) {
      row = new UserAvatarEntity();
      row.setUser(user);
    }
    applyImageChange(row, change, replacement);
    if (change.mode() == AvatarMode.CUSTOM && !row.isHasImage()) {
      throw new InvalidImage(Problem.MISSING);
    }
    row.setMode(change.mode());
    userAvatarRepository.saveAndFlush(row);
    userProfileBroadcaster.afterCommit(user);
    return new State(row.getVersion(), row.getMode(), row.isHasImage(), row.getImageRevision());
  }

  private void applyImageChange(UserAvatarEntity row, Change change, ImageReplacement replacement) {
    if (change.deleteImage()) {
      if (replacement != null || change.mode() != AvatarMode.INITIALS) {
        throw new InvalidImage(Problem.MISSING);
      }
      row.setImage(null);
      row.setHasImage(false);
      row.setImageRevision(null);
      row.setUploadedAt(null);
      row.setOriginalFormat(null);
      row.setOriginalSizeBytes(null);
      row.setOriginalWidth(null);
      row.setOriginalHeight(null);
    } else if (replacement != null) {
      row.setImage(replacement.image());
      row.setHasImage(true);
      row.setImageRevision(UUID.randomUUID());
      row.setUploadedAt(replacement.uploadedAt());
      row.setOriginalFormat(replacement.original().format());
      row.setOriginalSizeBytes(replacement.original().sizeBytes());
      row.setOriginalWidth(replacement.original().width());
      row.setOriginalHeight(replacement.original().height());
    }
  }

  public void release(UUID token) {
    avatarDraftStore.release(token);
  }

  public UserProfileBroadcaster events() {
    return userProfileBroadcaster;
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Optional<byte[]> image(
      HttpServletRequest request, Long target, UUID revision, boolean editor) {
    UserEntity user = requireUser(request, false);
    boolean own = Objects.equals(target, user.getId());
    if (!own && !user.hasAdministrativeRole()) {
      throw new AccessDeniedException("Avatar unavailable");
    }
    UserAvatarEntity row = userAvatarRepository.findById(target).orElse(null);
    if (row == null
        || !row.isHasImage()
        || !Objects.equals(revision, row.getImageRevision())
        || row.getMode() != AvatarMode.CUSTOM && !(editor && own)) {
      return Optional.empty();
    }
    return Optional.of(row.getImage());
  }

  private UserEntity requireUser(HttpServletRequest request, boolean lock) {
    if (!securityService.hasCurrentSessionPrivileges(request)) {
      throw new AuthenticationCredentialsNotFoundException("Session revoked");
    }
    UserDetails principal =
        securityService
            .getAuthenticatedUser(request)
            .orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    MaintenanceService.requirePublicAccess(configService);
    UserEntity user =
        (lock
                ? userRepository.findByEmailForUpdate(principal.getUsername())
                : userRepository.findByEmail(principal.getUsername()))
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    if (!user.isEnabled()) {
      throw new AccessDeniedException("Account disabled");
    }
    return user;
  }
}
