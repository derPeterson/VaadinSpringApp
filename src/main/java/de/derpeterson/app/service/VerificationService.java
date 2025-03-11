package de.derpeterson.app.service;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final CustomI18NProvider i18nProvider;
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigService configService;

    private final EmailQueueService emailQueueService;

    public String createToken(UserEntity user) {
        String token = UUID.randomUUID().toString();
        VerificationTokenEntity verificationToken = new VerificationTokenEntity();
        verificationToken.setToken(token);
        verificationToken.setUserEntity(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plus(Duration.parse(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION))));
        tokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public boolean validateToken(String token) {
        VerificationTokenEntity verificationToken = tokenRepository.findByToken(token).orElse(null);
        if (verificationToken == null || verificationToken.getExpiryDate().isBefore(LocalDateTime.now()) || verificationToken.getUserEntity().isEnabled()) {
            return false;
        }
        UserEntity user = verificationToken.getUserEntity();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
        return true;
    }

    @Transactional
    public boolean existsToken(String token) {
        return tokenRepository.findByToken(token).orElse(null) != null;
    }

    public int deleteToken(String token) {
        return tokenRepository.deleteByToken(token);
    }

    public boolean sendVerificationEmailByToken(String token) {
        Optional<VerificationTokenEntity> tokenOptional = tokenRepository.findByToken(token);
        if (tokenOptional.isPresent()) {
            UserEntity user = tokenOptional.get().getUserEntity();
            if (!user.isEnabled()) {
                String newToken = createToken(user);
                sendVerificationEmail(user, newToken);
                return true;
            }
        }
        return false;
    }

    public boolean sendVerificationEmailByEmail(String email) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            if (!user.isEnabled()) {
                String newToken = createToken(user);
                sendVerificationEmail(user, newToken);
                return true;
            }
        }
        return false;
    }

    public boolean sendVerificationEmailByUser(UserEntity user) {
        if (!user.isEnabled()) {
            String newToken = createToken(user);
            sendVerificationEmail(user, newToken);
            return true;
        }

        return false;
    }

    private void sendVerificationEmail(UserEntity user, String token) {
        String verificationLink = UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
                .pathSegment("verification", token).toUriString();
        emailQueueService.addEmailToQueue(
                user,
                i18nProvider.getTranslation("email.verification.subject"),
                MessageFormat.format(i18nProvider.getTranslation("email.verification.body"), verificationLink),
                EmailType.VERIFICATION
        );
        logger.info("Verification link for {} sent.", user.getEmail());
    }

    @Transactional
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void deleteExpiredTokens() {
        LocalDateTime liveDateTime = LocalDateTime.now().minus(Duration.parse(configService.getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION)));
        int deleted = tokenRepository.deleteByExpiryDateBefore(liveDateTime);
        var formattedLiveDateTime = liveDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE);
        logger.info("{} expired verification tokens that are older than '{}' have been deleted.", deleted, formattedLiveDateTime);
    }

}
