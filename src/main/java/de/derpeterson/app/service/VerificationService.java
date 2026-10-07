package de.derpeterson.app.service;

import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final MessageProperties messageProperties;
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigService configService;

    private final EmailQueueService emailQueueService;
    private final Clock clock;

    @Autowired
    public VerificationService(MessageProperties messageProperties, VerificationTokenRepository tokenRepository,
                               UserRepository userRepository, ConfigService configService, EmailQueueService emailQueueService) {
        this(messageProperties, tokenRepository, userRepository, configService, emailQueueService, Clock.systemDefaultZone());
    }

    @Transactional
    public String createToken(UserEntity user) {
        UserEntity stored = userRepository.lockVerificationUser(user.getId()).orElseThrow(
                () -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
        if (!canVerify(stored)) {
            throw new IllegalStateException("Das Konto kann nicht per Verifikationslink aktiviert werden.");
        }
        if (emailQueueService.hasOpenEmailForUserAndType(stored, EmailType.VERIFICATION)) {
            throw new IllegalStateException("Eine Verifikationsmail ist bereits in Bearbeitung.");
        }
        return createTokenLocked(stored);
    }

    private String createTokenLocked(UserEntity user) {
        LocalDateTime expiryDate = LocalDateTime.now(clock).plus(positiveDuration(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION));
        invalidateTokensLocked(user);

        String token = UUID.randomUUID().toString();
        VerificationTokenEntity verificationToken = new VerificationTokenEntity();
        verificationToken.setToken(token);
        verificationToken.setUserEntity(user);
        verificationToken.setExpiryDate(expiryDate);
        verificationToken.setStatus(TokenStatus.ACTIVE);
        tokenRepository.save(verificationToken);
        return token;
    }

    private Duration positiveDuration(ConfigEntry entry) {
        Duration duration = Duration.parse(configService.getString(entry));
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(entry + " muss eine positive Dauer sein.");
        }
        return duration;
    }

    @Transactional
    public void setTokenStatus(String token, TokenStatus status) {
        if (status == null || status == TokenStatus.ACTIVE) {
            throw new IllegalArgumentException("Tokens dürfen nicht reaktiviert werden.");
        }
        Optional<Long> userId = tokenRepository.findUserIdByToken(token);
        if (userId.isEmpty() || userRepository.lockVerificationUser(userId.get()).isEmpty()) {
            return;
        }
        var active = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        if (active.isPresent()) {
            active.get().setStatus(status);
            tokenRepository.save(active.get());
        } else if (tokenRepository.findByToken(token).filter(value -> value.getStatus() != status).isPresent()) {
            throw new IllegalStateException("Der Token ist bereits abgeschlossen.");
        }
    }

    @Transactional
    public void setInactiveTokensForUser(UserEntity user) {
        userRepository.lockVerificationUser(user.getId()).ifPresent(this::invalidateTokensLocked);
    }

    private void invalidateTokensLocked(UserEntity user) {
        tokenRepository.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE).forEach(token -> {
            token.setStatus(TokenStatus.INACTIVE);
            tokenRepository.save(token);
        });
    }

    @Transactional
    public boolean validateToken(String token) {
        Optional<Long> userId = tokenRepository.findUserIdByToken(token);
        if (userId.isEmpty()) {
            return false;
        }
        UserEntity user = userRepository.lockVerificationUser(userId.get()).orElse(null);
        if (user == null) {
            return false;
        }
        Optional<VerificationTokenEntity> tokenEntity = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        // The validity interval is exclusive at expiryDate: equality is already expired.
        if (tokenEntity.isPresent() && !tokenEntity.get().getExpiryDate().isAfter(LocalDateTime.now(clock))) {
            tokenEntity.get().setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(tokenEntity.get());
            return false;
        }
        if (tokenEntity.isPresent() && !canVerify(user)) {
            return false;
        }

        if (tokenEntity.isPresent()) {
            user.setEnabled(true);
            userRepository.save(user);
            tokenEntity.get().setStatus(TokenStatus.USED);
            tokenRepository.save(tokenEntity.get());
            return true;
        }

        // Idempotent success only for a consumed link and a still-enabled account.
        // No catch: unrelated persistence/locking errors must remain visible.
        return user.isEnabled() && tokenRepository.findByTokenAndStatus(token, TokenStatus.USED).isPresent();
    }

    @Transactional
    public boolean existsToken(String token) {
        return tokenRepository.findByToken(token).orElse(null) != null;
    }

    @Transactional
    public int deleteToken(String token) {
        Optional<Long> userId = tokenRepository.findUserIdByToken(token);
        if (userId.isEmpty() || userRepository.lockVerificationUser(userId.get()).isEmpty()) {
            return 0;
        }
        return tokenRepository.deleteByToken(token);
    }

    @Transactional(rollbackFor = IOException.class)
    public boolean sendVerificationEmailByToken(String token) throws IOException {
        Optional<Long> userId = tokenRepository.findUserIdByToken(token);
        if (userId.isPresent()) {
            return sendVerificationEmailByUserId(userId.get());
        }
        return false;
    }

    @Transactional(rollbackFor = IOException.class)
    public boolean sendVerificationEmailByEmail(String email) throws IOException {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            return sendVerificationEmailByUserId(userOptional.get().getId());
        }
        return false;
    }

    @Transactional(rollbackFor = IOException.class)
    public boolean sendVerificationEmailByUser(UserEntity user) throws IOException {
        return user != null && user.getId() != null && sendVerificationEmailByUserId(user.getId());
    }

    private boolean canVerify(UserEntity user) {
        return !user.isEnabled() && user.isVerificationPending();
    }

    private boolean sendVerificationEmailByUserId(Long id) throws IOException {
        UserEntity user = userRepository.lockVerificationUser(id).orElse(null);
        if (user != null && canVerify(user)) {
            if (emailQueueService.hasOpenEmailForUserAndType(user, EmailType.VERIFICATION)) {
                logger.warn("⚠️ Verification email for {} is already queued or in progress. Duplicate request skipped.", user.getEmail());
                return true;
            }

            String newToken = createTokenLocked(user);
            sendVerificationEmail(user, newToken);
            return true;
        }

        return false;
    }

    private void sendVerificationEmail(UserEntity user, String token) throws IOException {
        emailQueueService.addEmailToQueue(
                user,
                messageProperties.getEmailVerificationSubject(),
                loadEmailTemplate(user, token),
                EmailType.VERIFICATION
        );
        logger.info("✅ Verification email for {} added to the mail queue.", user.getEmail());
    }

    private String loadEmailTemplate(UserEntity user, String token) throws IOException {
        Map<String, String> placeholders = Map.of(
                "SERVICE_LOGO", ImageHelper.convertImageToBase64("META-INF/resources/custom-theme/service_logo.png"),
                "SERVICE_NAME", configService.getString(ConfigEntry.SERVICE_NAME),
                "FIRST_NAME", user.getFirstName(),
                "LAST_NAME", user.getLastName(),
                "VERIFICATION_LINK", UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
                        .pathSegment("verification", token).toUriString());

        ClassPathResource resource = new ClassPathResource("email/welcome_" + CustomI18NProvider.getCurrentLocale().getLanguage() + ".html");
        String content;
        try (InputStream stream = resource.getInputStream()) {
            content = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            content = Strings.CS.replace(content, "{{" + entry.getKey() + "}}", entry.getValue());
        }

        return content;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deleteExpiredTokens() {
        LocalDateTime liveDateTime = LocalDateTime.now(clock).minus(positiveDuration(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION));
        int deleted = tokenRepository.deleteByExpiryDateBefore(liveDateTime);
        var formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        logger.info("✅ {} expired verification tokens that are older than '{}' have been deleted.", deleted, formattedLiveDateTime);
    }

}
