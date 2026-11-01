package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.ui.components.ExternalConnectionsComponent;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Route(AppRouteConstants.ACCOUNT_ROUTE)
@PageTitle(AppRouteConstants.ACCOUNT_PAGE_TITLE)
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-external-login.css")
@UIScope
@PermitAll
public class AccountView extends IsAuthenticatedBaseView implements AfterNavigationObserver {
  private final AccountSettingsComponent accountSettings;

  public AccountView(
      MessageProperties messages,
      SecurityService security,
      HttpServletRequest request,
      ConfigService config,
      UserService users,
      AccountProfileService profiles,
      AccountSecurityService accountSecurity,
      SessionManagementService sessions,
      UserStatusBroadcaster broadcaster,
      UserAvatarService avatars,
      ExternalLoginFlow externalLogin,
      ExternalIdentityService identities) {
    super(messages, security, users, broadcaster, request, avatars);
    addClassNames(
        "cosmos-page", "cosmos-theme", "cosmos-app-shell", "cosmos-account", REFERENCE_PORTAL);
    HorizontalLayout header = createApplicationHeader(config);

    accountSettings =
        new AccountSettingsComponent(
            messages,
            currentUser,
            changes -> profiles.save(request, changes),
            config.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER));
    accountSettings.configureNavigationToggle(accountNavigationToggle);
    accountHeaderActions.setUnreadCountListener(accountSettings::updateUnreadCount);
    accountSettings.configureAvatars(avatars, request);
    accountSettings.configureStatusSelection(userPopoverMenu.createStatusSelect());
    accountSettings.configureSecurityActions(
        change -> {
          PasswordChanged changed = accountSecurity.changePassword(request, change);
          if (Objects.equals(currentUser.getVersion(), changed.previousVersion())) {
            currentUser.setVersion(changed.version());
          }
          return changed;
        },
        change -> accountSecurity.requestEmailChange(request, change));
    accountSettings.configureSessionActions(
        () -> sessions.list(request),
        handle -> sessions.revoke(request, handle),
        () -> sessions.revokeOthers(request));
    accountSettings.addSectionChangeListener(
        section ->
            UI.getCurrent()
                .navigate(
                    AccountView.class,
                    QueryParameters.simple(
                        Map.of("tab", section.name().toLowerCase(Locale.ROOT)))));
    // Erst nach bestätigtem Commit den sichtbaren UI-Zustand und die Sitzungssprache ändern.
    accountSettings.addProfileSavedListener(
        saved -> {
          currentUser.setFirstName(saved.firstName());
          currentUser.setLastName(saved.lastName());
          currentUser.setBirthDate(saved.birthDate());
          currentUser.setGender(saved.gender());
          currentUser.setPreferredLocale(saved.language());
          currentUser.setVersion(saved.version());
          userPopoverMenu.refreshProfileName();
          refreshTrackedUserAvatar();
          if (!saved.language().equals(VaadinSession.getCurrent().getLocale())) {
            LanguageChangeEvent.fire(UI.getCurrent(), saved.language());
          }
        });
    setPortalPage(header, accountSettings, config);
    configureExternalLogins(externalLogin, identities, request, messages);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    super.beforeEnter(event);
    if (event.hasForwardTarget()) {
      return;
    }
    List<String> values = event.getLocation().getQueryParameters().getParameters().get("tab");
    AccountSection section = AccountSection.PROFILE;
    // Fehlende, unbekannte und mehrdeutige Tabwerte öffnen immer das Profil.
    if (values != null && values.size() == 1) {
      try {
        section = AccountSection.valueOf(values.getFirst().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException _) {
        section = AccountSection.PROFILE;
      }
    }
    accountSettings.selectSection(section);
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    // Erst der angehängte Zielrahmen liefert die Palette für ausgelagerte Meldungen.
    List<String> values =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .get(ExternalLoginFlow.ERROR_PARAMETER);
    if (values == null) {
      return;
    }
    ExternalLoginFlow.ProofProblem problem = ExternalLoginFlow.ProofProblem.UNAVAILABLE;
    if (values.size() == 1) {
      try {
        problem = ExternalLoginFlow.ProofProblem.valueOf(values.getFirst());
      } catch (IllegalArgumentException _) {
        // Nur bekannte Fehlerarten auswählen, nie Queryinhalte als Meldung ausgeben.
      }
    }
    Supplier<String> message =
        switch (problem) {
          case FRESH_AUTHENTICATION_REQUIRED -> messageProperties::getExternalProofFreshRequired;
          case IDENTITY_MISMATCH -> messageProperties::getExternalProofIdentityMismatch;
          case UNAVAILABLE -> messageProperties::getExternalError;
        };
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            message,
            NotificationHelper.NotificationType.ERROR);
  }

  @Override
  protected void onTrackedUserProfileChanged(String first, String last) {
    if (accountSettings != null) {
      accountSettings.refreshSummaryName(first, last);
    }
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus status) {
    super.updateHeaderStatusIcon(status);
    if (accountSettings != null) {
      accountSettings.updateUserStatus(status);
    }
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    if (accountSettings != null) {
      accountSettings.refreshTexts();
    }
  }

  private void configureExternalLogins(
      ExternalLoginFlow flow,
      ExternalIdentityService identities,
      HttpServletRequest request,
      MessageProperties messages) {
    if (currentUser == null
        || flow.providers().isEmpty() && identities.connected(currentUser.getId()).isEmpty()) {
      return;
    }
    accountSettings.configureExternalLogins(
        new ExternalConnectionsComponent(flow, identities, request, messages),
        () ->
            flow.owner(request).getPassword() != null
                && flow.confirmedPurpose(request)
                    .filter(purpose -> purpose == ExternalLoginFlow.Purpose.PASSWORD)
                    .isEmpty(),
        () ->
            flow.owner(request).getPassword() != null
                && flow.confirmedPurpose(request)
                    .filter(purpose -> purpose == ExternalLoginFlow.Purpose.EMAIL)
                    .isEmpty());
  }
}
