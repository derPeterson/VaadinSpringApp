package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.PushSecurityInterceptor;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.session.ServiceReference;
import de.derpeterson.app.security.session.SessionAttributeCodec;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Preview;
import de.derpeterson.app.service.UserNotificationService.Selection;
import de.derpeterson.app.service.UserNotificationService.Target;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountAvatarDialog;
import de.derpeterson.app.ui.components.AccountNavigationTabs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/** Round-trips nur eigener Testbytes; keine Fremdstreams und keine vollständige UI-Passivierung. */
class UiSerializationContractTest {
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private GenericWebApplicationContext context;

  @BeforeEach
  void setup() {
    VaadinSession session = mock(VaadinSession.class);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    when(session.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(session);
    UI.setCurrent(new UI());
  }

  @AfterEach
  void cleanup() {
    if (context != null) {
      context.close();
    }
    com.vaadin.flow.internal.CurrentInstance.clearAll();
  }

  @SuppressWarnings("unchecked")
  private <T> T copy(T object) throws Exception {
    byte[] ownBytes;
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
      stream.writeObject(object);
      stream.flush();
      ownBytes = bytes.toByteArray();
    }
    try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(ownBytes))) {
      return (T) stream.readObject();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void fixedAndDynamicMessagesKeepTheirLocaleContractAfterReading(String language)
      throws Exception {
    Locale fixedLocale = Locale.forLanguageTag(language);
    MessageProperties dynamic = copy(new MessageProperties(new CustomI18NProvider()));
    MessageProperties fixed =
        copy(MessageProperties.forLocale(new CustomI18NProvider(), fixedLocale));
    locale.set(fixedLocale);
    assertEquals(fixed.getNotificationsTitle(), dynamic.getNotificationsTitle());
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    MessageProperties other = MessageProperties.forLocale(new CustomI18NProvider(), locale.get());
    assertEquals(other.getNotificationsTitle(), dynamic.getNotificationsTitle());
    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), fixedLocale).getNotificationsTitle(),
        fixed.getNotificationsTitle());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void accountTabsCanRefreshTextsAfterAComponentRoundTrip(String language) throws Exception {
    MessageProperties messages =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    AccountNavigationTabs tabs = copy(new AccountNavigationTabs(messages, true));
    tabs.refreshTexts();
    assertEquals(messages.getAccountSettingsTitle(), tabs.getElement().getAttribute("aria-label"));
    assertEquals("account-navigation-notifications", tabs.getSelectedTab().getId().orElseThrow());
  }

  private static final class Probe implements Serializable {
    private int page;
    private int size;
    private int actions;

    private void page(int value) {
      page = value;
    }

    private void size(int value) {
      size = value;
    }

    private void run() {
      actions++;
    }
  }

  private record Paging(AdminUserPagination component, Probe probe) implements Serializable {}

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void paginationKeepsBothCallbacksInsteadOfLosingTransientActions(String language)
      throws Exception {
    Probe probe = new Probe();
    MessageProperties messages =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    AdminUserPagination pagination = new AdminUserPagination(messages, probe::page, probe::size);
    pagination.showPage(2, 100, 10);
    Paging restored = copy(new Paging(pagination, probe));
    restored.component().refreshTexts();
    Button last = (Button) ReflectionTestUtils.getField(restored.component(), "last");
    last.click();
    assertEquals(9, restored.probe().page);
    ComboBox<?> size = (ComboBox<?>) ReflectionTestUtils.getField(restored.component(), "pageSize");
    setPageSize(size);
    assertEquals(20, restored.probe().size);
    assertEquals(messages.getAdminUsersPaginationLastPage(), last.getAriaLabel().orElseThrow());
  }

  @SuppressWarnings("unchecked")
  private void setPageSize(ComboBox<?> size) {
    ((ComboBox<Integer>) size).setValue(20);
  }

  private record Filters(AdminUserFilterBar component, Probe probe) implements Serializable {}

  @Test
  void filterCallbacksAndActiveFilterChipsStillWorkAfterReading() throws Exception {
    Probe probe = new Probe();
    MessageProperties messages = new MessageProperties(new CustomI18NProvider());
    AdminUserFilterBar filter =
        new AdminUserFilterBar(
            messages,
            new TextField(),
            new Button(),
            new Button(),
            new ComboBox<>(),
            new ComboBox<>(),
            new ComboBox<>(),
            new AdminUserActivityFilter(),
            probe::run);
    filter.showActiveFilters(
        List.of(new AdminUserFilterBar.ActiveFilter("Role", "User", probe::run)));
    Filters restored = copy(new Filters(filter, probe));
    restored.component().refreshTexts();
    ((Button) ReflectionTestUtils.getField(restored.component(), "applyFiltersButton")).click();
    assertEquals(1, restored.probe().actions);
    ((Button) ReflectionTestUtils.getField(restored.component(), "clearFilters")).click();
    assertEquals(2, restored.probe().actions);
  }

  @Test
  void notificationAndFilterValueGraphsAreSerializableWithoutEntitiesOrServices() throws Exception {
    Target target = new Target(7L, "Fixture", "fixture@example.test");
    Preview preview = new Preview(new Selection(false, Set.of(7L)), List.of(7L));
    UserNotificationService.Item item =
        new UserNotificationService.Item(8L, null, "Title", "Body", Instant.now(), false);
    AdminUserFilters filters =
        new AdminUserFilters(
            "query", true, UserStatus.AVAILABLE, RoleType.ROLE_USER, Instant.now());
    assertEquals(
        List.of(target, preview, item, filters), copy(List.of(target, preview, item, filters)));
  }

  private VaadinServletService liveContext(
      SecurityService security, MaintenanceService maintenance) {
    MockServletContext servletContext = new MockServletContext();
    context = new GenericWebApplicationContext(servletContext);
    context.registerBean(SecurityService.class, () -> security);
    context.registerBean(MaintenanceService.class, () -> maintenance);
    context.refresh();
    servletContext.setAttribute(
        WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    VaadinServlet servlet = mock(VaadinServlet.class);
    when(servlet.getServletContext()).thenReturn(servletContext);
    VaadinServletService service = mock(VaadinServletService.class);
    when(service.getServlet()).thenReturn(servlet);
    VaadinService.setCurrent(service);
    return service;
  }

  @Test
  void beanReferenceResolvesTheCurrentManagedServiceAndNeverSerializesTheOldService()
      throws Exception {
    SecurityService old = mock(SecurityService.class);
    ServiceReference<SecurityService> restored =
        copy(new ServiceReference<>(SecurityService.class, old));
    assertThrows(IllegalStateException.class, restored::get);
    SecurityService fresh = mock(SecurityService.class);
    liveContext(fresh, mock(MaintenanceService.class));
    assertSame(fresh, restored.get());
    assertNotSame(old, restored.get());
  }

  @Test
  void restoredPushGuardRejectsPrivilegesUsingTheNewApplicationService() throws Exception {
    PushSecurityInterceptor guard =
        copy(
            new PushSecurityInterceptor(
                mock(SecurityService.class), mock(MaintenanceService.class)));
    SecurityService fresh = mock(SecurityService.class);
    VaadinServletService service = liveContext(fresh, mock(MaintenanceService.class));
    MockHttpServletRequest request = new MockHttpServletRequest();
    VaadinServletRequest servletRequest = new VaadinServletRequest(request, service);
    assertThrows(AccessDeniedException.class, () -> guard.requestStart(servletRequest, null));
    verify(fresh).hasCurrentSessionPrivileges(servletRequest.getHttpServletRequest());
  }

  @Test
  void restoredBindingListenerReleasesTheCurrentRegistry() throws Exception {
    org.springframework.security.core.session.SessionRegistry old =
        mock(org.springframework.security.core.session.SessionRegistry.class);
    java.lang.reflect.Constructor<?> constructor =
        Class.forName("de.derpeterson.app.security.SessionAdmissionService$Registration")
            .getDeclaredConstructor(
                org.springframework.security.core.session.SessionRegistry.class,
                java.util.Map.class);
    constructor.setAccessible(true);
    jakarta.servlet.http.HttpSessionBindingListener restored =
        copy(
            (jakarta.servlet.http.HttpSessionBindingListener)
                constructor.newInstance(old, new java.util.HashMap<>()));
    org.springframework.security.core.session.SessionRegistry fresh =
        mock(org.springframework.security.core.session.SessionRegistry.class);
    de.derpeterson.app.security.SessionAdmissionService admission =
        new de.derpeterson.app.security.SessionAdmissionService(
            mock(de.derpeterson.app.service.ConfigService.class), fresh);
    MockServletContext servlet = new MockServletContext();
    context = new GenericWebApplicationContext(servlet);
    context.registerBean(
        de.derpeterson.app.security.SessionAdmissionService.class, () -> admission);
    context.refresh();
    servlet.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    org.springframework.mock.web.MockHttpSession session =
        new org.springframework.mock.web.MockHttpSession(servlet);
    restored.valueUnbound(new jakarta.servlet.http.HttpSessionBindingEvent(session, "fixture"));
    verify(fresh).removeSessionInformation(session.getId());
    org.mockito.Mockito.verifyNoInteractions(old);
  }

  @Test
  void entityBackedEditorsExplicitlyRejectJavaPassivationAndTheDurableCodecRejectsAllUi() {
    MessageProperties messages = new MessageProperties(new CustomI18NProvider());
    AccountAvatarDialog avatar =
        new AccountAvatarDialog(messages, UserEntity.builder().firstName("Fixture").build());
    AdminUserManagementSection users =
        new AdminUserManagementSection(
            messages,
            mock(UserService.class),
            mock(RoleService.class),
            mock(SecurityService.class));
    assertThrows(NotSerializableException.class, () -> copy(avatar));
    assertThrows(NotSerializableException.class, () -> copy(users));
    SessionAttributeCodec codec = new SessionAttributeCodec();
    assertThrows(IllegalArgumentException.class, () -> codec.encode(avatar));
    assertThrows(IllegalArgumentException.class, () -> codec.encode(users));
  }
}
