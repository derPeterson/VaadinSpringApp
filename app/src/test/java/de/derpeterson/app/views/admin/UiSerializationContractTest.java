package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.sidenav.SideNavItem;
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
import de.derpeterson.app.ui.components.CosmosPagination;
import de.derpeterson.app.ui.components.PageNavigationComponent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private GenericWebApplicationContext context;
  private VaadinSession vaadinSession;

  @BeforeEach
  void setup() {
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    when(vaadinSession.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(vaadinSession);
    UI.setCurrent(new UI());
  }

  @AfterEach
  void cleanup() {
    if (context != null) {
      context.close();
    }
    com.vaadin.flow.internal.CurrentInstance.clearAll();
    vaadinSession = null;
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
    localeReference.set(fixedLocale);

    assertEquals(fixed.getNotificationsTitle(), dynamic.getNotificationsTitle());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    MessageProperties other =
        MessageProperties.forLocale(new CustomI18NProvider(), localeReference.get());

    assertEquals(other.getNotificationsTitle(), dynamic.getNotificationsTitle());
    assertEquals(
        MessageProperties.forLocale(new CustomI18NProvider(), fixedLocale).getNotificationsTitle(),
        fixed.getNotificationsTitle());
  }

  private record Navigation(PageNavigationComponent component, Probe probe)
      implements Serializable {}

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void nestedNavigationKeepsLabelsSelectionAndCallbacksAfterReading(String language)
      throws Exception {
    MessageProperties messages =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    Probe probe = new Probe();
    PageNavigationComponent navigation = new PageNavigationComponent();
    SideNavItem account = new SideNavItem(messages.getAccountSettingsTitle());
    SideNavItem notifications = new SideNavItem(messages.getNotificationsTitle(), "/notifications");
    notifications.setId("notifications");
    account.addItem(notifications);
    navigation.addItem(account);
    navigation.registerAction(notifications, probe::run);
    PageNavigationComponent.setItemActive(notifications, true);

    Navigation restored = copy(new Navigation(navigation, probe));
    SideNavItem restoredAccount = restored.component().getItems().getFirst();
    SideNavItem restoredNotifications = restoredAccount.getItems().getFirst();

    assertEquals(messages.getAccountSettingsTitle(), restoredAccount.getLabel());
    assertEquals(messages.getNotificationsTitle(), restoredNotifications.getLabel());
    assertEquals("page", restoredNotifications.getElement().getAttribute("aria-current"));
    restored.component().activate(restoredNotifications);

    assertEquals(1, restored.probe().actions);
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

  private record Paging(CosmosPagination component, Probe probe) implements Serializable {}

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void paginationKeepsBothCallbacksInsteadOfLosingTransientActions(String language)
      throws Exception {
    Probe probe = new Probe();
    MessageProperties messages =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    CosmosPagination pagination = new CosmosPagination(messages, probe::page, probe::size);
    pagination.showPage(2, 100, 10);
    Paging restored = copy(new Paging(pagination, probe));
    restored.component().refreshTexts();
    Button last = (Button) ReflectionTestUtils.getField(restored.component(), "lastPageButton");
    last.click();

    assertEquals(9, restored.probe().page);
    ComboBox<?> size =
        (ComboBox<?>) ReflectionTestUtils.getField(restored.component(), "pageSizeSelector");
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
  void filterCallbacksAndDraftResetStillWorkAfterReading() throws Exception {
    Probe probe = new Probe();
    MessageProperties messages = new MessageProperties(new CustomI18NProvider());
    AdminUserFilterBar filter =
        new AdminUserFilterBar(
            messages,
            new TextField(),
            new Button(),
            new Button(),
            new AdminUserFilterBar.FilterControls(
                new de.derpeterson.app.ui.components.CosmosSegmentedTabs<>(),
                new com.vaadin.flow.component.checkbox.CheckboxGroup<>(),
                new com.vaadin.flow.component.checkbox.CheckboxGroup<>(),
                new AdminUserActivityFilter()),
            probe::run);
    filter.showActiveCount(1);
    Filters restored = copy(new Filters(filter, probe));
    restored.component().refreshTexts();
    ((Button) ReflectionTestUtils.getField(restored.component(), "applyFiltersButton")).click();

    assertEquals(1, restored.probe().actions);
    ((Button) ReflectionTestUtils.getField(restored.component(), "clearFiltersButton")).click();

    assertEquals(1, restored.probe().actions);
  }

  @Test
  void notificationAndFilterValueGraphsAreSerializableWithoutEntitiesOrServices() throws Exception {
    Target target = new Target(7L, "Fixture", "fixture@example.test");
    Preview preview = new Preview(new Selection(false, Set.of(7L)), List.of(7L));
    UserNotificationService.Item item =
        new UserNotificationService.Item(8L, null, "Title", "Body", Instant.now(), false);
    AdminUserFilters filters =
        new AdminUserFilters(
            "query", true, Set.of(UserStatus.AVAILABLE), Set.of(RoleType.ROLE_USER), Instant.now());

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
    VaadinServletService vaadinServletService = mock(VaadinServletService.class);
    when(vaadinServletService.getServlet()).thenReturn(servlet);
    VaadinService.setCurrent(vaadinServletService);
    return vaadinServletService;
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
    VaadinServletService vaadinServletService = liveContext(fresh, mock(MaintenanceService.class));
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    VaadinServletRequest servletRequest =
        new VaadinServletRequest(mockHttpRequest, vaadinServletService);

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
    org.springframework.mock.web.MockHttpSession restoredSession =
        new org.springframework.mock.web.MockHttpSession(servlet);
    restored.valueUnbound(
        new jakarta.servlet.http.HttpSessionBindingEvent(restoredSession, "fixture"));

    verify(fresh).removeSessionInformation(restoredSession.getId());
    org.mockito.Mockito.verifyNoInteractions(old);
  }

  @Test
  void entityBackedEditorsExplicitlyRejectJavaPassivationAndTheDurableCodecRejectsAllUi()
      throws Exception {
    MessageProperties messages = new MessageProperties(new CustomI18NProvider());
    AccountAvatarDialog avatar =
        new AccountAvatarDialog(messages, UserEntity.builder().firstName("Fixture").build());
    AdminUserManagementSection users =
        new AdminUserManagementSection(
            messages,
            mock(UserService.class),
            mock(RoleService.class),
            mock(SecurityService.class));
    // Der Frameworkgraph darf bereits vor unseren Hooks ablehnen; beides separat nachweisen.
    assertThrows(NotSerializableException.class, () -> copy(avatar));
    assertThrows(NotSerializableException.class, () -> copy(users));
    assertOwnSerializationHooksReject(avatar);
    assertOwnSerializationHooksReject(users);
    SessionAttributeCodec codec = new SessionAttributeCodec();

    assertThrows(IllegalArgumentException.class, () -> codec.encode(avatar));
    assertThrows(IllegalArgumentException.class, () -> codec.encode(users));
  }

  private void assertOwnSerializationHooksReject(Object editor) throws Exception {
    Method write = editor.getClass().getDeclaredMethod("writeObject", ObjectOutputStream.class);
    write.setAccessible(true);
    byte[] header;
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      InvocationTargetException rejected =
          assertThrows(InvocationTargetException.class, () -> write.invoke(editor, output));

      assertInstanceOf(NotSerializableException.class, rejected.getCause());
      assertEquals(
          "Runtime UI must be rebuilt from the authenticated session",
          rejected.getCause().getMessage());
      output.flush();
      header = bytes.toByteArray();
    }
    Method read = editor.getClass().getDeclaredMethod("readObject", ObjectInputStream.class);
    read.setAccessible(true);
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(header))) {
      InvocationTargetException rejected =
          assertThrows(InvocationTargetException.class, () -> read.invoke(editor, input));

      assertInstanceOf(InvalidObjectException.class, rejected.getCause());
      assertEquals(
          "Runtime UI cannot be restored from a Java object graph",
          rejected.getCause().getMessage());
    }
  }
}
