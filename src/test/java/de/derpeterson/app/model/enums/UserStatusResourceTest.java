package de.derpeterson.app.model.enums;

import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UserStatusResourceTest {
    @TempDir
    Path temporary;

    @ParameterizedTest
    @EnumSource(IconSize.class)
    void absentSvgHasOriginalShapeColorAndBothSizeVariantsWithoutHttp(IconSize size) {
        check(UserStatus.ABSENT.getComponent(size, false), size, false);
        check(UserStatus.ABSENT.getComponent(size, true), size, true);
    }

    @Test
    void rendererLoadsItsResourceFromAPackedJar() throws Exception {
        Path jar = temporary.resolve("status-resource.jar");
        String className = UserStatus.class.getName();
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            add(output, className.replace('.', '/') + ".class");
            // Compiler-dependent enum switch helper, if present.
            String helper = className.replace('.', '/') + "$1.class";
            if (UserStatus.class.getResource("/" + helper) != null) {
                add(output, helper);
            }
            add(output, UserStatus.ABSENT.getFileUrl().substring(1));
        }
        try (var loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, UserStatus.class.getClassLoader()) {
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

            @Override
            public URL getResource(String name) {
                // Ensure the tested SVG cannot fall back to exploded target/classes.
                if (name.equals(UserStatus.ABSENT.getFileUrl().substring(1))) {
                    return findResource(name);
                }
                return super.getResource(name);
            }
        }) {
            Class<?> packed = loader.loadClass(className);
            assertEquals("jar", packed.getResource(UserStatus.ABSENT.getFileUrl()).getProtocol());
            Object absent = packed.getField("ABSENT").get(null);
            Component rendered = (Component) packed.getMethod("getComponent", IconSize.class, Boolean.class)
                    .invoke(absent, IconSize.PIXEL_48, true);
            check(rendered, IconSize.PIXEL_48, true);
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

    private void check(Component component, IconSize size, boolean overlay) {
        String svg = component.getElement().getProperty("innerHTML");
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("viewBox=\"0 0 24 24\""));
        assertTrue(svg.contains("M 12 24 C 5.383 24"));
        assertTrue(svg.contains("fill=\"#f4d03f\""));
        double width = overlay ? Math.round(size.getWidth() * 0.2) : size.getWidth();
        assertTrue(svg.contains("width=\"" + width + "\""));
        assertTrue(svg.contains("height=\"" + width + "\""));
        assertEquals((overlay ? Long.toString(Math.round(width)) : Double.toString(width)) + "px", component.getElement().getStyle().get("width"));
        assertEquals("50%", component.getElement().getStyle().get("border-radius"));
        if (overlay) {
            assertEquals("absolute", component.getElement().getStyle().get("position"));
        }
    }
}
