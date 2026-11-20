package de.derpeterson.app.ui.components;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.PortalReleaseNotes;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class PortalReleaseNotesComponentTest {
  private final Locale language = Locale.GERMAN;
  private final PortalReleaseNotesComponent notes =
      new PortalReleaseNotesComponent(
          MessageProperties.forLocale(new CustomI18NProvider(), language));

  @Test
  void archiveIsHiddenWhenOnlyTheFirstReleaseIsVisible() {
    notes.showReleases(new PortalReleaseNotes("0.2.0").getReleases(), language);

    assertThat(notes.getChildren().filter(Details.class::isInstance)).isEmpty();
  }

  @Test
  void olderReleaseStartsCollapsedAndShowsItsTitleVersionAndDate() {
    notes.showReleases(new PortalReleaseNotes("0.2.1-SNAPSHOT").getReleases(), language);

    CosmosCardComponent latestCard =
        (CosmosCardComponent) notes.getChildren().findFirst().orElseThrow();
    assertThat(latestCard.getChildren().filter(H2.class::isInstance).map(H2.class::cast))
        .extracting(H2::getText)
        .containsExactly("Ein frischer Look fürs Portal");

    List<Details> archiveEntries =
        notes.getChildren().filter(Details.class::isInstance).map(Details.class::cast).toList();
    assertThat(archiveEntries).hasSize(1);
    Details firstReleaseDetails = archiveEntries.getFirst();
    assertThat(firstReleaseDetails.isOpened()).isFalse();
    assertThat(firstReleaseDetails.getSummary().getChildren().map(Span.class::cast))
        .extracting(Span::getText)
        .containsExactly("Der erste Portal-Release", "Version 0.1.0 · 18. November 2026");
  }
}
