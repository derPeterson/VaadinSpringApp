package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

class UserNotificationPolicyTest {
  static Stream<Arguments> settings() {
    return Stream.of(
        Arguments.of(Map.of(), 180),
        Arguments.of(Map.of("app.user-notifications.retention-days", "14"), 14));
  }

  @ParameterizedTest
  @MethodSource("settings")
  void configuredRetentionOrDefaultIsUsed(Map<String, String> properties, int expected) {
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource("notification-test", new HashMap<String, Object>(properties)));
      context.registerBean(PropertySourcesPlaceholderConfigurer.class);
      context.register(UserNotificationPolicy.class);
      context.refresh();
      assertEquals(expected, context.getBean(UserNotificationPolicy.class).retentionDays());
    }
  }
}
