package de.derpeterson.app.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.SvgIcon;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserStatusResourceTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @EnumSource(IconSize.class)
  void outlineSymbolsHaveShapeColorAndBothSizeVariantsWithLocalLucideResources(IconSize size) {
    for (UserStatus status : UserStatus.values()) {
      check(status.getOutlineComponent(size, false), status, size, false);
      check(status.getOutlineComponent(size, true), status, size, true);
    }
  }

  @Test
  void outlineRendererWorksFromAPackedJarWithoutLegacySvg() throws Exception {
    Path jar = temporary.resolve("status-resource.jar");
    String className = UserStatus.class.getName();
    try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
      add(output, className.replace('.', '/') + ".class");
      // Compiler-dependent enum switch helper, if present.
      String helper = className.replace('.', '/') + "$1.class";
      if (UserStatus.class.getResource("/" + helper) != null) {
        add(output, helper);
      }
    }
    try (var loader =
        new URLClassLoader(new URL[] {jar.toUri().toURL()}, UserStatus.class.getClassLoader()) {
          @Override
          protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(className) || name.startsWith(className + "$")) {
              Class<?> type = findLoadedClass(name);
              if (type == null) {
                type = findClass(name);
              }
              if (resolve) {
                resolveClass(type);
              }
              return type;
            }
            return super.loadClass(name, resolve);
          }
        }) {
      Class<?> packed = loader.loadClass(className);
      assertEquals(jar.toUri(), packed.getProtectionDomain().getCodeSource().getLocation().toURI());
      Object absent = packed.getField("ABSENT").get(null);
      Component rendered =
          (Component)
              packed
                  .getMethod("getOutlineComponent", IconSize.class, boolean.class)
                  .invoke(absent, IconSize.PIXEL_48, true);
      check(rendered, UserStatus.ABSENT, IconSize.PIXEL_48, true);
    }
  }

  private void add(JarOutputStream jar, String name) throws Exception {
    try (InputStream stream = UserStatus.class.getResourceAsStream("/" + name)) {
      assertNotNull(stream, name);
      jar.putNextEntry(new JarEntry(name));
      stream.transferTo(jar);
      jar.closeEntry();
    }
  }

  private void check(Component component, UserStatus status, IconSize size, boolean overlay) {
    SvgIcon icon = (SvgIcon) component.getChildren().findFirst().orElseThrow();
    String name =
        switch (status) {
          case AVAILABLE -> "circle-check";
          case EMPLOYED -> "circle-minus";
          case ABSENT -> "clock";
          case OFFLINE -> "circle-x";
        };
    assertEquals("icons/lucide/" + name + ".svg", icon.getSrc());
    assertNotNull(UserStatus.class.getResource("/META-INF/resources/" + icon.getSrc()));
    double expectedWidth = overlay ? Math.round(size.getWidth() * .3) : size.getWidth();
    String width = Double.toString(expectedWidth) + size.getUnit().getSymbol();
    assertEquals(width, component.getElement().getStyle().get("width"));
    assertEquals(status.getColor(), component.getElement().getStyle().get("color"));
    assertEquals("50%", component.getElement().getStyle().get("border-radius"));
    if (overlay) {
      assertEquals("absolute", component.getElement().getStyle().get("position"));
      assertEquals(
          "var(--cosmos-avatar-status-anchor)", component.getElement().getStyle().get("bottom"));
      assertEquals("translate(50%, 50%)", component.getElement().getStyle().get("transform"));
    }
  }
}
