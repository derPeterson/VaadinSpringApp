package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.MaintenanceService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public registration admission, account and initial mail queue entry share one transaction. */
@Service
@RequiredArgsConstructor
public class RegistrationService {
  private final UserService users;
  private final VerificationService verification;
  private final ConfigService config;

  @Transactional(rollbackFor = IOException.class)
  public boolean register(UserEntity user) throws IOException {
    MaintenanceService.requirePublicAccess(config);
    users.saveUser(user);
    return verification.sendVerificationEmailByUser(user);
  }
}
