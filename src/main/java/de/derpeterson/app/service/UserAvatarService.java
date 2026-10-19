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
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
public class UserAvatarService {
  public record State(Long version, AvatarMode mode, boolean hasImage, UUID imageRevision)
      implements Serializable {
    public static State initial() {
      return new State(null, AvatarMode.INITIALS, false, null);
    }
  }

  public record Open(State state, UUID draft) {}

  public record Change(
      Long version, AvatarMode mode, boolean deleteImage, UUID draft, UUID imageId, Crop crop) {}

  private final UserAvatarRepository avatars;
  private final UserRepository users;
  private final SecurityService security;
  private final ConfigService config;
  private final AvatarImageProcessor processor;
  private final AvatarDraftStore drafts;
  private final UserProfileBroadcaster events;
  private final TransactionTemplate transaction;

  public UserAvatarService(
      UserAvatarRepository avatars,
      UserRepository users,
      SecurityService security,
      ConfigService config,
      AvatarImageProcessor processor,
      AvatarDraftStore drafts,
      UserProfileBroadcaster events,
      PlatformTransactionManager manager) {
    this.avatars = avatars;
    this.users = users;
    this.security = security;
    this.config = config;
    this.processor = processor;
    this.drafts = drafts;
    this.events = events;
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  // Interne UI-Metadaten, ausdrücklich ohne BLOBs; Grid lädt eine sichtbare Seite gesammelt.
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Map<Long, State> states(Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    HashMap<Long, State> result = new HashMap<Long, State>();
    for (Metadata row : avatars.metadata(ids)) {
      result.put(
          row.getUserId(),
          new State(row.getVersion(), row.getMode(), row.getHasImage(), row.getImageRevision()));
    }
    return result;
  }

  public State state(Long userId) {
    return userId == null
        ? State.initial()
        : states(List.of(userId)).getOrDefault(userId, State.initial());
  }

  public Open open(HttpServletRequest request) {
    return transaction.execute(
        status -> {
          UserEntity user = requireUser(request, false);
          return new Open(state(user.getId()), drafts.begin(user.getId()));
        });
  }

  public UUID upload(HttpServletRequest request, UUID token, InputStream input) throws IOException {
    Long owner = transaction.execute(status -> requireUser(request, false).getId());
    return drafts.upload(token, owner, input);
  }

  public State save(HttpServletRequest request, Change change) {
    if (change == null || change.mode() == null) {
      throw new InvalidImage(Problem.MISSING);
    }
    Long owner = transaction.execute(status -> requireUser(request, false).getId());
    // Original/Decode niemals unter der späteren Kontosperre halten.
    byte[] normalized =
        change.imageId() == null
            ? null
            : processor.normalize(
                drafts.image(change.draft(), owner, change.imageId()).bytes(), change.crop());
    State saved =
        transaction.execute(
            status -> {
              UserEntity user = requireUser(request, true);
              if (!owner.equals(user.getId())) {
                throw new AccessDeniedException("Identity changed");
              }
              UserAvatarEntity row = avatars.findById(owner).orElse(null);
              if (!Objects.equals(change.version(), row == null ? null : row.getVersion())) {
                throw new OptimisticLockingFailureException("Avatar changed; reopen dialog");
              }
              if (row == null) {
                row = new UserAvatarEntity();
                row.setUser(user);
              }
              if (change.deleteImage()) {
                if (normalized != null || change.mode() != AvatarMode.INITIALS) {
                  throw new InvalidImage(Problem.MISSING);
                }
                row.setImage(null);
                row.setHasImage(false);
                row.setImageRevision(null);
              } else if (normalized != null) {
                row.setImage(normalized);
                row.setHasImage(true);
                row.setImageRevision(UUID.randomUUID());
              }
              if (change.mode() == AvatarMode.CUSTOM && !row.isHasImage()) {
                throw new InvalidImage(Problem.MISSING);
              }
              row.setMode(change.mode());
              avatars.saveAndFlush(row);
              events.afterCommit(user);
              return new State(
                  row.getVersion(), row.getMode(), row.isHasImage(), row.getImageRevision());
            });
    drafts.release(change.draft());
    return saved;
  }

  public void release(UUID token) {
    drafts.release(token);
  }

  public UserProfileBroadcaster events() {
    return events;
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public Optional<byte[]> image(
      HttpServletRequest request, Long target, UUID revision, boolean editor) {
    UserEntity user = requireUser(request, false);
    boolean own = Objects.equals(target, user.getId());
    if (!own && !user.hasAdministrativeRole()) {
      throw new AccessDeniedException("Avatar unavailable");
    }
    UserAvatarEntity row = avatars.findById(target).orElse(null);
    if (row == null
        || !row.isHasImage()
        || !Objects.equals(revision, row.getImageRevision())
        || row.getMode() != AvatarMode.CUSTOM && !(editor && own)) {
      return Optional.empty();
    }
    return Optional.of(row.getImage());
  }

  private UserEntity requireUser(HttpServletRequest request, boolean lock) {
    if (!security.hasCurrentSessionPrivileges(request)) {
      throw new AuthenticationCredentialsNotFoundException("Session revoked");
    }
    UserDetails principal =
        security
            .getAuthenticatedUser(request)
            .orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    MaintenanceService.requirePublicAccess(config);
    UserEntity user =
        (lock
                ? users.findByEmailForUpdate(principal.getUsername())
                : users.findByEmail(principal.getUsername()))
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    if (!user.isEnabled()) {
      throw new AccessDeniedException("Account disabled");
    }
    return user;
  }
}
