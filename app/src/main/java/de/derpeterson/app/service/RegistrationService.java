package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.security.MaintenanceService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public registration admission, account and initial mail queue entry share one transaction. */
@Service
@RequiredArgsConstructor
public class RegistrationService {
  private final UserService userService;
  private final VerificationService verificationService;
  private final ConfigService configService;

  @Transactional(rollbackFor = IOException.class)
  public boolean register(UserEntity registeredUserEntity) throws IOException {
    MaintenanceService.requirePublicAccess(configService);

    registeredUserEntity.setRegistrationChannel(RegistrationChannel.PORTAL);
    userService.saveUser(registeredUserEntity);

    return verificationService.sendVerificationEmailByUser(registeredUserEntity);
  }
}
