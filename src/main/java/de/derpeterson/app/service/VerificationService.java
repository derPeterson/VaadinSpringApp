package de.derpeterson.app.service;

import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
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
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final CustomI18NProvider i18nProvider;
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigService configService;

    private final EmailQueueService emailQueueService;

    @Transactional
    public String createToken(UserEntity user) {
        setInactiveTokensForUser(user);

        String token = UUID.randomUUID().toString();
        VerificationTokenEntity verificationToken = new VerificationTokenEntity();
        verificationToken.setToken(token);
        verificationToken.setUserEntity(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plus(Duration.parse(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION))));
        verificationToken.setStatus(TokenStatus.ACTIVE);
        tokenRepository.save(verificationToken);
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

    @Transactional
    public boolean validateToken(String token) {
        Optional<VerificationTokenEntity> tokenEntity = tokenRepository.findByTokenAndStatus(token, TokenStatus.ACTIVE);
        if (tokenEntity.isPresent() && tokenEntity.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            setTokenStatus(token, TokenStatus.EXPIRED);
            return false;
        }
        if (tokenEntity.isPresent() && tokenEntity.get().getStatus() == TokenStatus.INACTIVE) {
            return false;
        }
        if (tokenEntity.isPresent() && tokenEntity.get().getUserEntity().isEnabled()) {
            return false;
        }

        if (tokenEntity.isPresent()) {
            UserEntity user = tokenEntity.get().getUserEntity();
            user.setEnabled(true);
            userRepository.save(user);
            setTokenStatus(token, TokenStatus.USED);
            return true;
        }

        return false;
    }

    @Transactional
    public boolean existsToken(String token) {
        return tokenRepository.findByToken(token).orElse(null) != null;
    }

    public int deleteToken(String token) {
        return tokenRepository.deleteByToken(token);
    }

    @Transactional
    public boolean sendVerificationEmailByToken(String token) throws IOException {
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

    @Transactional
    public boolean sendVerificationEmailByEmail(String email) throws IOException {
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

    @Transactional
    public boolean sendVerificationEmailByUser(UserEntity user) throws IOException {
        if (!user.isEnabled()) {
            String newToken = createToken(user);
            sendVerificationEmail(user, newToken);
            return true;
        }

        return false;
    }

    private void sendVerificationEmail(UserEntity user, String token) throws IOException {
        emailQueueService.addEmailToQueue(
                user,
                i18nProvider.getTranslation("email.verification.subject"),
                loadEmailTemplate(user, token),
                EmailType.VERIFICATION
        );
        logger.info("Verification link for {} sent.", user.getEmail());
    }

    private String loadEmailTemplate(UserEntity user, String token) throws IOException {
        Map<String, String> placeholders = Map.of(
                "SERVICE_LOGO", ImageHelper.convertImageToBase64("src/main/frontend/themes/custom-theme/service_logo.png"),
                "SERVICE_NAME", configService.getString(ConfigEntry.SERVICE_NAME),
                "FIRST_NAME", user.getFirstName(),
                "LAST_NAME", user.getLastName(),
                "VERIFICACTION_LINK", UriComponentsBuilder.fromUriString(configService.getString(ConfigEntry.BASE_URL))
                        .pathSegment("verification", token).toUriString());

        ClassPathResource resource = new ClassPathResource("email/welcome_" + CustomI18NProvider.getCurrentLocale().getLanguage() + ".html");
        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            content = StringUtils.replace(content, "{{" + entry.getKey() + "}}", entry.getValue());
        }

        return content;
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
