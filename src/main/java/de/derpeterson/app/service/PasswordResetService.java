package de.derpeterson.app.service;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final CustomI18NProvider i18nProvider;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;

    private final EmailQueueService emailQueueService;

    private final PasswordResetService self;

    @Transactional
    public String createToken(UserEntity user) {
        self.setInactiveTokensForUser(user);

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
        logger.info("{} expired password reset tokens that are older than '{}' have been deleted.", deleted, formattedLiveDateTime);
    }

    @Transactional
    public boolean existsToken(String token) {
        return tokenRepository.findByToken(token).orElse(null) != null;
    }

    @Transactional
    public boolean sendPasswordResetEmail(String email) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            if (user.isEnabled()) {
                String token = self.createToken(user);

                String passwordResetLink = UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
                        .pathSegment("reset-password", token).toUriString();
                emailQueueService.addEmailToQueue(
                        user,
                        i18nProvider.getTranslation("email.password_reset.subject"),
                        MessageFormat.format(i18nProvider.getTranslation("email.password_reset.body"), passwordResetLink),
                        EmailType.PASSWORD_RESET
                );
                logger.info("Password reset link for {} sent.", user.getEmail());
                return true;
            }
        }
        return false;
    }
}
