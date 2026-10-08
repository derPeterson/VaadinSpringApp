package de.derpeterson.app.service;

import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.validation.UserInputRules;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final MessageProperties messageProperties;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;

    private final EmailQueueService emailQueueService;

    @Transactional
    public String createToken(UserEntity user) {
        UserEntity stored = userRepository.lockVerificationUser(user.getId()).orElseThrow(
                () -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
        if (!stored.isEnabled()) {
            throw new IllegalStateException("Das Konto ist gesperrt.");
        }
        if (emailQueueService.hasOpenEmailForUserAndType(stored, EmailType.PASSWORD_RESET)) {
            throw new IllegalStateException("Eine Passwort-Reset-Mail ist bereits in Bearbeitung.");
        }
        return createTokenLocked(stored);
    }

    private String createTokenLocked(UserEntity user) {
        invalidateTokensLocked(user);

        String token = UUID.randomUUID().toString();
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setToken(token);
        resetToken.setUserEntity(user);
        resetToken.setExpiryDate(LocalDateTime.now().plus(Duration.parse(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))));
        resetToken.setStatus(TokenStatus.ACTIVE);
        tokenRepository.save(resetToken);
        return token;
    }

    /** ACTIVE is an initial state, never a transition; terminal states are irreversible. */
    @Transactional
    public void setTokenStatus(String token, TokenStatus status) {
        if (status == null || status == TokenStatus.ACTIVE) {
            throw new IllegalArgumentException("Tokens dürfen nicht reaktiviert werden.");
        }
        if (lockTokenUser(token).isEmpty()) {
            return;
        }
        tokenRepository.findByToken(token).ifPresent(tokenEntity -> {
            if (tokenEntity.getStatus() == status) {
                return;
            }
            if (tokenEntity.getStatus() != TokenStatus.ACTIVE) {
                throw new IllegalStateException("Der Token ist bereits abgeschlossen.");
            }
            tokenEntity.setStatus(status);
            tokenRepository.save(tokenEntity);
        });
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

    private Optional<UserEntity> lockTokenUser(String token) {
        return tokenRepository.findUserIdByToken(token).flatMap(userRepository::lockVerificationUser);
    }

    @Transactional
    public boolean validateToken(String token) {
        Optional<UserEntity> user = lockTokenUser(token);
        if (user.isEmpty()) {
            return false;
        }
        Optional<PasswordResetTokenEntity> tokenEntity = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        if (tokenEntity.isPresent() && tokenEntity.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenEntity.get().setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(tokenEntity.get());
            return false;
        }
        if (tokenEntity.isPresent() && tokenEntity.get().getStatus() == TokenStatus.INACTIVE) {
            return false;
        }
        return tokenEntity.isPresent() && user.get().isEnabled();
    }

    /** Invalid raw passwords raise IllegalArgumentException before encoding or token consumption. */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Assert.isTrue(UserInputRules.isPasswordSecure(newPassword), "Invalid password");
        Optional<UserEntity> lockedUser = lockTokenUser(token);
        if (lockedUser.isEmpty()) {
            return false;
        }
        Optional<PasswordResetTokenEntity> tokenEntity = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        if (tokenEntity.isPresent() && tokenEntity.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenEntity.get().setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(tokenEntity.get());
            return false;
        }
        if (tokenEntity.isPresent() && tokenEntity.get().getStatus() == TokenStatus.INACTIVE) {
            return false;
        }
        if (tokenEntity.isPresent() && !lockedUser.get().isEnabled()) {
            return false;
        }
        if (tokenEntity.isPresent()) {
            UserEntity user = lockedUser.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            tokenEntity.get().setStatus(TokenStatus.USED);
            tokenRepository.save(tokenEntity.get());
            return true;
        }
        return false;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 1/3 * ?")
    public void deleteExpiredTokens() {
        LocalDateTime liveDateTime = LocalDateTime.now().minus(Duration.parse(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)));
        int deleted = tokenRepository.deleteByExpiryDateBefore(liveDateTime);
        var formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        logger.info("✅ {} expired password reset tokens that are older than '{}' have been deleted.", deleted, formattedLiveDateTime);
    }

    @Transactional
    public boolean existsToken(String token) {
        return tokenRepository.findByToken(token).orElse(null) != null;
    }

    @Transactional(rollbackFor = IOException.class)
    public boolean sendPasswordResetEmail(String email) throws IOException {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        userOptional = userOptional.flatMap(user -> userRepository.lockVerificationUser(user.getId()));
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            if (user.isEnabled()) {
                if (emailQueueService.hasOpenEmailForUserAndType(user, EmailType.PASSWORD_RESET)) {
                    logger.warn("⚠️ Password reset email for {} is already queued or in progress. Duplicate request skipped.", user.getEmail());
                    return true;
                }

                String token = createTokenLocked(user);

                emailQueueService.addEmailToQueue(
                        user,
                        messageProperties.getEmailResetPasswordSubject(),
                        loadEmailTemplate(token),
                        EmailType.PASSWORD_RESET
                );
                logger.info("✅ Password reset link for {} sent.", user.getEmail());
                return true;
            }
        }
        return false;
    }

    private String loadEmailTemplate(String token) throws IOException {
        Map<String, String> placeholders = Map.of(
                "SERVICE_LOGO", ImageHelper.convertImageToBase64("META-INF/resources/custom-theme/service_logo.png"),
                "SERVICE_NAME", configService.getString(ConfigEntry.SERVICE_NAME),
                "RESET_PASSWORD_LINK", UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
                        .pathSegment("reset-password", token).toUriString());

        ClassPathResource resource = new ClassPathResource("email/reset_password_" + CustomI18NProvider.getCurrentLocale().getLanguage() + ".html");
        String content;
        try (InputStream input = resource.getInputStream()) {
            content = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            content = Strings.CS.replace(content, "{{" + entry.getKey() + "}}", entry.getValue());
        }

        return content;
    }
}
