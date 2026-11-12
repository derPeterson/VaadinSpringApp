package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CosmosBreadcrumbsComponentTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void parentPathsUseStableLocationsAndLocalizedLabels(String languageTag) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(languageTag));
    Map<ParentPath, List<String>> locationsByPath =
        Map.of(
            ParentPath.PORTAL, List.of("/home"),
            ParentPath.ACCOUNT, List.of("/home", "/account?section=profile"),
            ParentPath.ACCOUNT_SECURITY,
                List.of(
                    "/home", "/account?section=profile", "/account?section=security&tab=portal"),
            ParentPath.ADMINISTRATION, List.of("/admin"),
            ParentPath.CONFIGURATION,
                List.of("/admin", "/admin?section=configuration&group=general"));

    for (ParentPath parentPath : ParentPath.values()) {
      CosmosBreadcrumbsComponent pageBreadcrumbsComponent =
          new CosmosBreadcrumbsComponent(messageProperties, parentPath, true);
      List<BreadcrumbsItem> breadcrumbItems = breadcrumbItems(pageBreadcrumbsComponent);

      assertEquals(Breadcrumbs.Mode.MANUAL, pageBreadcrumbsComponent.getMode());
      assertEquals(
          locationsByPath.get(parentPath),
          breadcrumbItems.stream().map(BreadcrumbsItem::getPath).toList());
      assertEquals(
          messageProperties.getPageBreadcrumbsLabel(),
          pageBreadcrumbsComponent.getAriaLabel().orElseThrow());
      assertEquals(
          messageProperties.getPageBreadcrumbsMore(),
          pageBreadcrumbsComponent.getI18n().getMoreItems());
    }

    CosmosBreadcrumbsComponent securityBreadcrumbsComponent =
        new CosmosBreadcrumbsComponent(messageProperties, ParentPath.ACCOUNT_SECURITY, true);
    assertEquals(
        List.of(
            messageProperties.getAccountNavigationPortal(),
            messageProperties.getAccountSettingsOverview(),
            messageProperties.getAccountSettingsSecurityTab()),
        breadcrumbItems(securityBreadcrumbsComponent).stream()
            .map(BreadcrumbsItem::getText)
            .toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void lockedFlowsRemoveEveryLocationEvenWhenTheirContextChanges(String languageTag) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(languageTag));
    CosmosBreadcrumbsComponent pageBreadcrumbsComponent =
        new CosmosBreadcrumbsComponent(messageProperties, ParentPath.PORTAL, true);

    for (ParentPath parentPath : ParentPath.values()) {
      pageBreadcrumbsComponent.setParentPath(parentPath, false);

      for (BreadcrumbsItem breadcrumbItem : breadcrumbItems(pageBreadcrumbsComponent)) {
        assertNull(breadcrumbItem.getPath());
      }
    }

    pageBreadcrumbsComponent.setParentPath(ParentPath.PORTAL, true);
    assertEquals(
        List.of("/home"),
        breadcrumbItems(pageBreadcrumbsComponent).stream().map(BreadcrumbsItem::getPath).toList());
  }

  private List<BreadcrumbsItem> breadcrumbItems(
      CosmosBreadcrumbsComponent pageBreadcrumbsComponent) {
    return pageBreadcrumbsComponent.getChildren().map(BreadcrumbsItem.class::cast).toList();
  }
}
