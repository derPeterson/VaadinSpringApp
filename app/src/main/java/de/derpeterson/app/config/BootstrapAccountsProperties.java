package de.derpeterson.app.config;

import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.validation.UserInputRules;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Keine Zugangsdaten in toString/Logs; jede Gruppe besitzt genau eine gespeicherte Rolle. */
@ConfigurationProperties("app.bootstrap")
@Getter
@Setter
public class BootstrapAccountsProperties {
  private boolean enabled;
  private List<Account> superadmins = new ArrayList<>();
  private List<Account> admins = new ArrayList<>();
  private List<Account> users = new ArrayList<>();
  private List<Account> guests = new ArrayList<>();

  @Getter
  @Setter
  public static class Account {
    private String email;
    private String password;
    private String firstName = "John";
    private String lastName = "Doe";
    private LocalDate birthDate;
    private Gender gender;
    private Locale preferredLocale = Locale.ENGLISH;
  }

  public record Entry(RoleType role, Account account) {
    @Override
    public String toString() {
      return "Bootstrap entry (" + role + ")";
    }
  }

  public List<Entry> validatedEntries() {
    if (!enabled) {
      return List.of();
    }

    ArrayList<Entry> validatedBootstrapEntries = new ArrayList<Entry>();
    HashSet<String> seenEmailAddresses = new HashSet<String>();
    appendValidatedAccountsForRole(
        validatedBootstrapEntries, seenEmailAddresses, RoleType.ROLE_SUPERADMIN, superadmins);
    appendValidatedAccountsForRole(
        validatedBootstrapEntries, seenEmailAddresses, RoleType.ROLE_ADMIN, admins);
    appendValidatedAccountsForRole(
        validatedBootstrapEntries, seenEmailAddresses, RoleType.ROLE_USER, users);
    appendValidatedAccountsForRole(
        validatedBootstrapEntries, seenEmailAddresses, RoleType.ROLE_GUEST, guests);

    return List.copyOf(validatedBootstrapEntries);
  }

  private void appendValidatedAccountsForRole(
      List<Entry> validatedBootstrapEntries,
      Set<String> seenEmailAddresses,
      RoleType roleType,
      List<Account> accountsForRole) {
    if (accountsForRole == null) {
      throw new IllegalArgumentException("Bootstrap list required: " + roleType);
    }

    for (int bootstrapAccountIndex = 0;
        bootstrapAccountIndex < accountsForRole.size();
        bootstrapAccountIndex++) {
      Account bootstrapAccount = accountsForRole.get(bootstrapAccountIndex);
      if (bootstrapAccount == null
          || !UserInputRules.isEmailValid(bootstrapAccount.email)
          || !UserInputRules.isPasswordSecure(bootstrapAccount.password)
          || !UserInputRules.isRequiredTextValid(bootstrapAccount.firstName)
          || !UserInputRules.isRequiredTextValid(bootstrapAccount.lastName)
          || !UserInputRules.isBirthDateValid(bootstrapAccount.birthDate)
          || bootstrapAccount.preferredLocale == null
          || !Set.of("de", "en").contains(bootstrapAccount.preferredLocale.getLanguage())) {
        throw new IllegalArgumentException(
            "Invalid bootstrap account in " + roleType + " at index " + bootstrapAccountIndex);
      }

      String canonicalEmailAddress = EmailIdentity.canonicalize(bootstrapAccount.email);
      if (!seenEmailAddresses.add(canonicalEmailAddress)) {
        throw new IllegalArgumentException(
            "Duplicate bootstrap account in " + roleType + " at index " + bootstrapAccountIndex);
      }

      validatedBootstrapEntries.add(new Entry(roleType, bootstrapAccount));
    }
  }
}
