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
    private LocalDate birthDate = LocalDate.of(1990, 1, 1);
    private Gender gender = Gender.OTHER;
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
    ArrayList<Entry> entries = new ArrayList<Entry>();
    HashSet<String> emails = new HashSet<String>();
    add(entries, emails, RoleType.ROLE_SUPERADMIN, superadmins);
    add(entries, emails, RoleType.ROLE_ADMIN, admins);
    add(entries, emails, RoleType.ROLE_USER, users);
    add(entries, emails, RoleType.ROLE_GUEST, guests);
    return List.copyOf(entries);
  }

  private void add(List<Entry> entries, Set<String> emails, RoleType role, List<Account> group) {
    if (group == null) {
      throw new IllegalArgumentException("Bootstrap list required: " + role);
    }
    for (int i = 0; i < group.size(); i++) {
      Account account = group.get(i);
      if (account == null
          || !UserInputRules.isEmailValid(account.email)
          || !UserInputRules.isPasswordSecure(account.password)
          || !UserInputRules.isRequiredTextValid(account.firstName)
          || !UserInputRules.isRequiredTextValid(account.lastName)
          || !UserInputRules.isBirthDateValid(account.birthDate)
          || account.gender == null
          || account.preferredLocale == null
          || !Set.of("de", "en").contains(account.preferredLocale.getLanguage())) {
        throw new IllegalArgumentException(
            "Invalid bootstrap account in " + role + " at index " + i);
      }
      String email = EmailIdentity.canonicalize(account.email);
      if (!emails.add(email)) {
        throw new IllegalArgumentException(
            "Duplicate bootstrap account in " + role + " at index " + i);
      }
      entries.add(new Entry(role, account));
    }
  }
}
