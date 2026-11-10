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
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorManagement;
import de.derpeterson.app.service.AccountProfileService;
import de.derpeterson.app.service.AccountProfileService.Profile;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.AccountSecurityService.PasswordChanged;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountSettingsComponent;
import de.derpeterson.app.ui.components.AccountSettingsComponent.AccountSection;
import de.derpeterson.app.ui.components.ExternalConnectionsComponent;
import de.derpeterson.app.ui.components.TwoFactorAccountComponent;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;

@Route(AppRouteConstants.ACCOUNT_ROUTE)
@PageTitle(AppRouteConstants.ACCOUNT_PAGE_TITLE)
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-external-login.css")
@UIScope
@PermitAll
public class AccountView extends IsAuthenticatedBaseView implements AfterNavigationObserver {
  private final AccountSettingsComponent accountSettingsComponent;

  @Autowired
  void configureTwoFactor(
      TwoFactorAuthorization twoFactorAuthorization,
      TwoFactorManagement twoFactorManagement,
      HttpServletRequest httpServletRequest) {
    accountSettingsComponent.configureTwoFactor(
        new TwoFactorAccountComponent(
            messageProperties, twoFactorAuthorization, twoFactorManagement, httpServletRequest));
  }

  public AccountView(
      MessageProperties messageProperties,
      SecurityService securityService,
      HttpServletRequest httpServletRequest,
      ConfigService configService,
      UserService userService,
      AccountProfileService accountProfileService,
      AccountSecurityService accountSecurityService,
      SessionManagementService sessionManagementService,
      UserStatusBroadcaster userStatusBroadcaster,
      UserAvatarService userAvatarService,
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService) {
    super(
        messageProperties,
        securityService,
        userService,
        userStatusBroadcaster,
        httpServletRequest,
        userAvatarService);

    addClassNames(
        "cosmos-page", "cosmos-theme", "cosmos-app-shell", "cosmos-account", REFERENCE_PORTAL);
    HorizontalLayout applicationHeaderHorizontalLayout = createApplicationHeader(configService);

    accountSettingsComponent =
        new AccountSettingsComponent(
            messageProperties,
            currentUser,
            profileChanges -> accountProfileService.save(httpServletRequest, profileChanges),
            configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER));

    accountSettingsComponent.configureNavigationToggle(accountNavigationToggle);
    accountHeaderActions.setUnreadCountListener(accountSettingsComponent::updateUnreadCount);
    accountSettingsComponent.configureAvatars(userAvatarService, httpServletRequest);
    accountSettingsComponent.configureStatusSelection(userPopoverMenu.createStatusSelect());

    accountSettingsComponent.configureSecurityActions(
        passwordChange -> {
          PasswordChanged passwordChangeResult =
              accountSecurityService.changePassword(httpServletRequest, passwordChange);
          if (Objects.equals(currentUser.getVersion(), passwordChangeResult.previousVersion())) {
            currentUser.setVersion(passwordChangeResult.version());
          }
          return passwordChangeResult;
        },
        emailChange -> accountSecurityService.requestEmailChange(httpServletRequest, emailChange));

    accountSettingsComponent.configureSessionActions(
        () -> sessionManagementService.list(httpServletRequest),
        sessionHandle -> sessionManagementService.revoke(httpServletRequest, sessionHandle),
        () -> sessionManagementService.revokeOthers(httpServletRequest));

    accountSettingsComponent.addSectionChangeListener(selectedAccountSection ->
        UI.getCurrent().navigate(AccountView.class, selectedAccountSection.queryParameters()));
    accountSettingsComponent.addProfileSavedListener(this::applySavedProfileToPortal);

    setPortalPage(applicationHeaderHorizontalLayout, accountSettingsComponent, configService);
    configureExternalLogins(
        externalLoginFlow, externalIdentityService, httpServletRequest, messageProperties);
  }

  private void applySavedProfileToPortal(Profile savedProfile) {
    // Erst nach bestätigtem Commit den sichtbaren UI-Zustand und die Sitzungssprache ändern.
    currentUser.setFirstName(savedProfile.firstName());
    currentUser.setLastName(savedProfile.lastName());
    currentUser.setBirthDate(savedProfile.birthDate());
    currentUser.setGender(savedProfile.gender());
    currentUser.setPreferredLocale(savedProfile.language());
    currentUser.setVersion(savedProfile.version());

    userPopoverMenu.refreshProfileName();
    refreshTrackedUserAvatar();

    if (!savedProfile.language().equals(VaadinSession.getCurrent().getLocale())) {
      LanguageChangeEvent.fire(UI.getCurrent(), savedProfile.language());
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent navigationEvent) {
    super.beforeEnter(navigationEvent);
    if (navigationEvent.hasForwardTarget()) {
      return;
    }

    Map<String, List<String>> accountQueryParameters =
        navigationEvent.getLocation().getQueryParameters().getParameters();
    List<String> accountTabParameterValues =
        accountQueryParameters.getOrDefault(ACCOUNT_TAB_PARAMETER, List.of());

    // Alte Direktlinks verwenden „tab“ noch für den Seitenbereich.
    AccountSection selectedAccountSection =
        parseAccountSection(
            accountQueryParameters.getOrDefault(
                ACCOUNT_SECTION_PARAMETER, accountTabParameterValues));

    accountSettingsComponent.selectSection(selectedAccountSection);
    accountSettingsComponent.selectSecurityTab(
        selectedAccountSection == AccountSection.SECURITY
                && accountQueryParameters.containsKey(ACCOUNT_SECTION_PARAMETER)
                && accountTabParameterValues.size() == 1
            ? accountTabParameterValues.getFirst()
            : AppRouteConstants.ACCOUNT_PORTAL_TAB);
  }

  private AccountSection parseAccountSection(List<String> accountSectionParameterValues) {
    if (accountSectionParameterValues.size() == 1) {
      try {
        return AccountSection.valueOf(
            accountSectionParameterValues.getFirst().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException _) {
        // Unbekannte Bereiche wie eine fehlende Auswahl behandeln.
      }
    }
    return AccountSection.PROFILE;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent navigationEvent) {
    ExternalProofNotice.showExternalProofFailure(navigationEvent, messageProperties);
    ExternalProofNotice.showExternalDisconnectNotice(navigationEvent, messageProperties, false);
  }

  @Override
  protected void onTrackedUserProfileChanged(String firstName, String lastName) {
    if (accountSettingsComponent != null) {
      accountSettingsComponent.refreshSummaryName(firstName, lastName);
    }
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus userStatus) {
    super.updateHeaderStatusIcon(userStatus);
    if (accountSettingsComponent != null) {
      accountSettingsComponent.updateUserStatus(userStatus);
    }
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    if (accountSettingsComponent != null) {
      accountSettingsComponent.refreshTexts();
    }
  }

  private void configureExternalLogins(
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      HttpServletRequest httpServletRequest,
      MessageProperties messageProperties) {
    if (currentUser == null
        || externalLoginFlow.providers().isEmpty()
            && externalIdentityService.connected(currentUser.getId()).isEmpty()) {
      return;
    }

    accountSettingsComponent.configureExternalLogins(
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, httpServletRequest, messageProperties),
        () ->
            externalLoginFlow.owner(httpServletRequest).getPassword() != null
                && externalLoginFlow
                    .confirmedPurpose(httpServletRequest)
                    .filter(
                        confirmedPurpose -> confirmedPurpose == ExternalLoginFlow.Purpose.PASSWORD)
                    .isEmpty(),
        () ->
            externalLoginFlow.owner(httpServletRequest).getPassword() != null
                && externalLoginFlow
                    .confirmedPurpose(httpServletRequest)
                    .filter(confirmedPurpose -> confirmedPurpose == ExternalLoginFlow.Purpose.EMAIL)
                    .isEmpty());
  }
}
