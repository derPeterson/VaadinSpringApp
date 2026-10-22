package de.derpeterson.app.helper.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class LoggingPolicyTest {
  private LoggerContext context;

  @AfterEach
  void stopContext() {
    if (context != null) {
      context.stop();
    }
  }

  @Test
  void normalApplicationLoggingIsInfoInBothConfigurations() throws Exception {
    context = isolatedConfiguration();
    assertEquals(Level.INFO, context.getLogger("de.derpeterson.app").getLevel());
    assertFalse(context.getLogger("de.derpeterson.app.service.UserService").isDebugEnabled());
    assertEquals("INFO", properties().getProperty("logging.level.de.derpeterson.app"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "de.derpeterson.app.security.AdmittedSecurityContextRepository",
        "de.derpeterson.app.security.DeviceRememberMeServices"
      })
  void inheritedSensitiveDebugRemainsOffWhenParentDebugIsEnabled(String name) throws Exception {
    context = isolatedConfiguration();
    context.getLogger("de.derpeterson.app").setLevel(Level.DEBUG);
    Logger logger = context.getLogger(name);
    assertEquals(Level.INFO, logger.getLevel());
    assertEquals("INFO", properties().getProperty("logging.level." + name));
    ListAppender<ILoggingEvent> logs = new ListAppender<>();
    logs.start();
    context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(logs);
    for (int i = 0; i < 20; i++) {
      logger.debug("Retrieved SecurityContext: {}", "private credentials token");
    }
    logger.warn("Fixture operational warning");
    logger.error("Fixture operational failure");
    context
        .getLogger("de.derpeterson.app.service.UserService")
        .debug("Fixture targeted diagnostic");
    assertEquals(3, logs.list.size());
    assertTrue(
        logs.list.stream().noneMatch(event -> event.getFormattedMessage().contains("private")));
    assertEquals(Level.WARN, logs.list.getFirst().getLevel());
    assertEquals(Level.ERROR, logs.list.get(1).getLevel());
    assertEquals(Level.DEBUG, logs.list.getLast().getLevel());
  }

  private Properties properties() throws Exception {
    Properties properties = new Properties();
    try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
      assertNotNull(input);
      properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
    return properties;
  }

  private LoggerContext isolatedConfiguration() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    Document document;
    try (InputStream input = getClass().getResourceAsStream("/logback-spring.xml")) {
      assertNotNull(input);
      document = factory.newDocumentBuilder().parse(input);
    }
    // Originale Loggerregeln prüfen, aber niemals echte Logdateien im Test öffnen.
    for (String tag : new String[] {"appender", "appender-ref"}) {
      NodeList nodes = document.getElementsByTagName(tag);
      while (nodes.getLength() > 0) {
        nodes.item(0).getParentNode().removeChild(nodes.item(0));
      }
    }
    ByteArrayOutputStream xml = new ByteArrayOutputStream();
    TransformerFactory.newInstance()
        .newTransformer()
        .transform(new DOMSource(document), new StreamResult(xml));
    LoggerContext isolatedContext = new LoggerContext();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(isolatedContext);
    configurator.doConfigure(new ByteArrayInputStream(xml.toByteArray()));
    return isolatedContext;
  }
}
