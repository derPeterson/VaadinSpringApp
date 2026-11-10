package de.derpeterson.app.views;

import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_SECTION_PARAMETER;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_TAB_PARAMETER;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
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

@Route(AppRouteConstants.ACCOUNT_ROUTE)
@PageTitle(AppRouteConstants.ACCOUNT_PAGE_TITLE)
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-external-login.css")
@UIScope
@PermitAll
public class AccountView extends IsAuthenticatedBaseView implements AfterNavigationObserver {
  private final AccountSettingsComponent accountSettings;

  @org.springframework.beans.factory.annotation.Autowired
  void configureTwoFactor(
      de.derpeterson.app.security.mfa.TwoFactorAuthorization twoFactorAuthorization,
      de.derpeterson.app.security.mfa.TwoFactorManagement twoFactorManagement,
      HttpServletRequest request) {
    accountSettings.configureTwoFactor(
        new de.derpeterson.app.ui.components.TwoFactorAccountComponent(
            messageProperties, twoFactorAuthorization, twoFactorManagement, request));
  }

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
        section -> UI.getCurrent().navigate(AccountView.class, section.queryParameters()));
    // Erst nach bestätigtem Commit den sichtbaren UI-Zustand und die Sitzungssprache ändern.
    accountSettings.addProfileSavedListener(saved -> {
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
    Map<String, List<String>> query = event.getLocation().getQueryParameters().getParameters();
    List<String> tabValues = query.getOrDefault(ACCOUNT_TAB_PARAMETER, List.of());
    // Alte Direktlinks verwenden „tab“ noch für den Seitenbereich.
    AccountSection section =
        accountSection(query.getOrDefault(ACCOUNT_SECTION_PARAMETER, tabValues));
    accountSettings.selectSection(section);
    accountSettings.selectSecurityTab(
        section == AccountSection.SECURITY
                && query.containsKey(ACCOUNT_SECTION_PARAMETER)
                && tabValues.size() == 1
            ? tabValues.getFirst()
            : AppRouteConstants.ACCOUNT_PORTAL_TAB);
  }

  private AccountSection accountSection(List<String> values) {
    if (values.size() == 1) {
      try {
        return AccountSection.valueOf(values.getFirst().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException _) {
        // Unbekannte Bereiche wie eine fehlende Auswahl behandeln.
      }
    }
    return AccountSection.PROFILE;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    ExternalProofNotice.show(event, messageProperties);
    ExternalProofNotice.showDisconnected(event, messageProperties, false);
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
