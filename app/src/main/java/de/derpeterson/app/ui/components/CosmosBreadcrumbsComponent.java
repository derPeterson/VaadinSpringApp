package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.MessageTexts;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Einheitlicher Elternpfad; aktuelle Seite und Sicherheitsfreigabe bleiben Aufgabe der View. */
public class CosmosBreadcrumbsComponent extends Breadcrumbs {
  public enum ParentPath {
    PORTAL(List.of(PathItem.PORTAL)),
    ACCOUNT(List.of(PathItem.PORTAL, PathItem.ACCOUNT)),
    ACCOUNT_SECURITY(List.of(PathItem.PORTAL, PathItem.ACCOUNT, PathItem.SECURITY)),
    ADMINISTRATION(List.of(PathItem.ADMINISTRATION)),
    CONFIGURATION(List.of(PathItem.ADMINISTRATION, PathItem.CONFIGURATION));

    private final List<PathItem> pathItems;

    ParentPath(List<PathItem> pathItems) {
      this.pathItems = pathItems;
    }
  }

  private enum PathItem {
    PORTAL(MessageTexts::getAccountNavigationPortal, AppRouteConstants.HOME_SECURITY_ROUTE),
    ACCOUNT(
        MessageTexts::getAccountSettingsOverview,
        AppRouteConstants.ACCOUNT_SECURITY_ROUTE + "?section=profile"),
    SECURITY(
        MessageTexts::getAccountSettingsSecurityTab, AppRouteConstants.ACCOUNT_PORTAL_LOCATION),
    ADMINISTRATION(MessageTexts::getAdminHeaderTitle, AppRouteConstants.ADMIN_SECURITY_ROUTE),
    CONFIGURATION(MessageTexts::getAdminConfigTitle, firstAdminConfigurationLocation());

    private final Function<MessageTexts, String> labelFunction;
    private final String location;

    PathItem(Function<MessageTexts, String> labelFunction, String location) {
      this.labelFunction = labelFunction;
      this.location = location;
    }
  }

  private final MessageProperties messageProperties;
  private boolean navigationAllowed;
  private ParentPath parentPath;
  private Registration languageChangeRegistration;

  public CosmosBreadcrumbsComponent(
      MessageProperties messageProperties, ParentPath parentPath, boolean navigationAllowed) {
    super(Mode.MANUAL);
    this.messageProperties = messageProperties;
    this.parentPath = parentPath;
    this.navigationAllowed = navigationAllowed;

    addClassName("cosmos-breadcrumbs");
    refreshTexts();
  }

  public void setParentPath(ParentPath parentPath, boolean navigationAllowed) {
    this.parentPath = parentPath;
    this.navigationAllowed = navigationAllowed;

    refreshTexts();
  }

  public void refreshTexts() {
    removeAll();
    for (PathItem pathItem : parentPath.pathItems) {
      BreadcrumbsItem breadcrumbItem =
          new BreadcrumbsItem(pathItem.labelFunction.apply(messageProperties));
      if (navigationAllowed) {
        breadcrumbItem.setPath(pathItem.location);
      }
      add(breadcrumbItem);
    }

    setAriaLabel(messageProperties.getPageBreadcrumbsLabel());
    setI18n(new BreadcrumbsI18n().setMoreItems(messageProperties.getPageBreadcrumbsMore()));
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    refreshTexts();
    languageChangeRegistration =
        ComponentUtil.addListener(
            attachEvent.getUI(), LanguageChangeEvent.class, event -> refreshTexts());
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    if (languageChangeRegistration != null) {
      languageChangeRegistration.remove();
      languageChangeRegistration = null;
    }
    super.onDetach(detachEvent);
  }

  private static String firstAdminConfigurationLocation() {
    // Allgemein zugängliche Gruppen sind für Admin und Superadmin dasselbe sichere Elternziel.
    AdminConfigGroup firstConfigurationGroup =
        Arrays.stream(AdminConfigGroup.values())
            .filter(configGroup -> !configGroup.isSuperadminOnly())
            .findFirst()
            .orElseThrow();
    return AppRouteConstants.ADMIN_SECURITY_ROUTE
        + "?section=configuration&group="
        + firstConfigurationGroup.name().toLowerCase(Locale.ROOT);
  }
}
