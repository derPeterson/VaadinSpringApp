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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
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
        setInactiveTokensForUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setToken(token);
        resetToken.setUserEntity(user);
        resetToken.setExpiryDate(LocalDateTime.now().plus(Duration.parse(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))));
        resetToken.setStatus(TokenStatus.ACTIVE);
        tokenRepository.save(resetToken);
        return token;
    }

    public void setTokenStatus(String token, TokenStatus status) {
        tokenRepository.findByToken(token).ifPresent(tokenEntity -> {
            tokenEntity.setStatus(status);
            tokenRepository.save(tokenEntity);
        });
    }

    @Transactional
    public void setInactiveTokensForUser(UserEntity user) {
        tokenRepository.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE).forEach(token -> {
            token.setStatus(TokenStatus.INACTIVE);
            tokenRepository.save(token);
        });
    }

    public boolean validateToken(String token) {
        Optional<PasswordResetTokenEntity> tokenEntity = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        if (tokenEntity.isPresent() && tokenEntity.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            setTokenStatus(token, TokenStatus.EXPIRED);
            return false;
        }
        if (tokenEntity.isPresent() && tokenEntity.get().getStatus() == TokenStatus.INACTIVE) {
            return false;
        }
        return tokenEntity.isPresent() && tokenEntity.get().getUserEntity().isEnabled();
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetTokenEntity> tokenEntity = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        if (tokenEntity.isPresent() && tokenEntity.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            setTokenStatus(token, TokenStatus.EXPIRED);
            return false;
        }
        if (tokenEntity.isPresent() && tokenEntity.get().getStatus() == TokenStatus.INACTIVE) {
            return false;
        }
        if (tokenEntity.isPresent() && !tokenEntity.get().getUserEntity().isEnabled()) {
            return false;
        }
        if (tokenEntity.isPresent()) {
            UserEntity user = tokenEntity.get().getUserEntity();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            setTokenStatus(token, TokenStatus.USED);
            return true;
        }
        return false;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 1/3 * ?")
    public void deleteExpiredTokens() {
        LocalDateTime liveDateTime = LocalDateTime.now().minus(Duration.parse(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)));
        int deleted = tokenRepository.deleteByExpiryDateBefore(liveDateTime);
        var formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE);
        logger.info("✅ {} expired password reset tokens that are older than '{}' have been deleted.", deleted, formattedLiveDateTime);
    }

    @Transactional
    public boolean existsToken(String token) {
        return tokenRepository.findByToken(token).orElse(null) != null;
    }

    @Transactional
    public boolean sendPasswordResetEmail(String email) throws IOException {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            if (user.isEnabled()) {
                if (emailQueueService.hasOpenEmailForUserAndType(user, EmailType.PASSWORD_RESET)) {
                    logger.warn("⚠️ Password reset email for {} is already queued or in progress. Duplicate request skipped.", user.getEmail());
                    return true;
                }

                String token = createToken(user);

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
                "SERVICE_LOGO", ImageHelper.convertImageToBase64("src/main/frontend/themes/custom-theme/service_logo.png"),
                "SERVICE_NAME", configService.getString(ConfigEntry.SERVICE_NAME),
                "RESET_PASSWORD_LINK", UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
                        .pathSegment("reset-password", token).toUriString());

        ClassPathResource resource = new ClassPathResource("email/reset_password_" + CustomI18NProvider.getCurrentLocale().getLanguage() + ".html");
        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            content = Strings.CS.replace(content, "{{" + entry.getKey() + "}}", entry.getValue());
        }

        return content;
    }
}
