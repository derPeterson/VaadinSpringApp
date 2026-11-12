package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.AccountLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountLifecycleComponentTest {
  @ParameterizedTest
  @CsvSource({"de,false", "de,true", "en,false", "en,true"})
  void actionDescriptionsRespectTheAccountTypeAndLanguage(
      String languageTag, boolean developmentAccount) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(languageTag));
    SecurityService securityService = mock(SecurityService.class);
    when(securityService.getCurrentUser())
        .thenReturn(
            Optional.of(UserEntity.builder().developmentAccount(developmentAccount).build()));
    AccountLifecycleService accountLifecycleService = mock(AccountLifecycleService.class);
    AccountLifecycleComponent accountLifecycleComponent =
        new AccountLifecycleComponent(
            messageProperties,
            accountLifecycleService,
            securityService,
            mock(HttpServletRequest.class));

    assertEquals(
        developmentAccount
            ? messageProperties.getAccountDevelopmentDeactivateDescription()
            : messageProperties.getAccountDeactivateDescription(),
        findDescriptionSpan(accountLifecycleComponent, "account-deactivate-description").getText());
    assertEquals(
        messageProperties.getAccountDeleteDescription(),
        findDescriptionSpan(accountLifecycleComponent, "account-delete-description").getText());
    verifyNoInteractions(accountLifecycleService);
  }

  private Span findDescriptionSpan(Component rootComponent, String descriptionId) {
    return streamComponents(rootComponent)
        .filter(component -> component.getId().filter(descriptionId::equals).isPresent())
        .map(Span.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private Stream<Component> streamComponents(Component rootComponent) {
    return Stream.concat(
        Stream.of(rootComponent), rootComponent.getChildren().flatMap(this::streamComponents));
  }
}
