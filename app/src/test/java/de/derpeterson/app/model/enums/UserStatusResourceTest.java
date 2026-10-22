package de.derpeterson.app.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.Component;
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
  void outlineSymbolsHaveShapeColorAndBothSizeVariantsWithoutExternalResources(IconSize size) {
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
    String svg = component.getElement().getProperty("innerHTML");
    assertNotNull(svg);
    assertTrue(svg.contains("<svg"));
    assertTrue(svg.contains("viewBox=\"0 0 24 24\""));
    assertTrue(svg.contains("fill=\"none\""));
    assertTrue(svg.contains("stroke=\"currentColor\""));
    assertTrue(svg.contains("<circle"));
    String mark =
        switch (status) {
          case AVAILABLE -> "m7.5 12 3 3 6-6";
          case EMPLOYED -> "M7.5 12h9";
          case ABSENT -> "M12 6.5V12l4 2.5";
          case OFFLINE -> "m8.5 8.5 7 7m0-7-7 7";
        };
    assertTrue(svg.contains(mark));
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
