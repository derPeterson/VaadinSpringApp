package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.List;
import java.util.TreeSet;

/** Gemeinsame native Pagination mit vollständiger oder auf zwei Pfeile verkürzter Navigation. */
@StyleSheet("custom-theme/admin-users.css")
public final class CosmosPagination extends HorizontalLayout {
  public enum Mode {
    NUMBERED,
    PREVIOUS_NEXT
  }

  private final MessageProperties messageProperties;
  private final SerializableConsumer<Integer> selectPage;
  private final ComboBox<Integer> pageSizeSelector = new ComboBox<>();
  private final Span rowsLabel = new Span();
  private final Span totalLabel = new Span();
  private final HorizontalLayout pageNumberButtons = new HorizontalLayout();
  private final Button firstPageButton = new Button(AppIcon.ANGLE_DOUBLE_LEFT.create());
  private final Button previousPageButton = new Button(AppIcon.ANGLE_LEFT.create());
  private final Button nextPageButton = new Button(AppIcon.ANGLE_RIGHT.create());
  private final Button lastPageButton = new Button(AppIcon.ANGLE_DOUBLE_RIGHT.create());
  private int currentPage;
  private int totalPages = 1;
  private long totalItems;
  private final Mode mode;

  public CosmosPagination(
      MessageProperties messageProperties,
      SerializableConsumer<Integer> selectPage,
      SerializableConsumer<Integer> selectPageSize) {
    this(messageProperties, selectPage, selectPageSize, List.of(10, 20, 50));
  }

  public CosmosPagination(
      MessageProperties messageProperties,
      SerializableConsumer<Integer> selectPage,
      SerializableConsumer<Integer> selectPageSize,
      List<Integer> pageSizes) {
    this(messageProperties, selectPage, selectPageSize, pageSizes, Mode.NUMBERED);
  }

  public CosmosPagination(
      MessageProperties messageProperties,
      SerializableConsumer<Integer> selectPage,
      SerializableConsumer<Integer> selectPageSize,
      List<Integer> pageSizes,
      Mode mode) {
    this.messageProperties = messageProperties;
    this.selectPage = selectPage;
    this.mode = mode;
    addClassName("admin-user-pagination");
    setWidthFull();
    setAlignItems(Alignment.CENTER);
    setPadding(false);
    pageSizeSelector.setItems(pageSizes);
    pageSizeSelector.setValue(pageSizes.getFirst());
    pageSizeSelector.setAllowCustomValue(false);
    PortalFields.styleChoice(pageSizeSelector, Object::toString, false);
    pageSizeSelector.addClassName("portal-pagination-size");
    pageSizeSelector.setWidth("56px");
    pageSizeSelector.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        selectPageSize.accept(event.getValue());
      }
    });
    HorizontalLayout summary = new HorizontalLayout(rowsLabel, pageSizeSelector, totalLabel);
    summary.addClassName("admin-user-pagination-summary");
    summary.setAlignItems(Alignment.CENTER);
    pageNumberButtons.addClassName("admin-user-pagination-numbers");
    pageNumberButtons.setSpacing(false);
    pageNumberButtons.setAlignItems(Alignment.CENTER);
    HorizontalLayout navigation =
        new HorizontalLayout(
            firstPageButton, previousPageButton, pageNumberButtons, nextPageButton, lastPageButton);
    navigation.addClassName("admin-user-pagination-nav");
    navigation.setSpacing(false);
    navigation.setAlignItems(Alignment.CENTER);
    firstPageButton.setVisible(mode == Mode.NUMBERED);
    lastPageButton.setVisible(mode == Mode.NUMBERED);
    pageNumberButtons.setVisible(mode == Mode.NUMBERED);
    for (Button button :
        List.of(firstPageButton, previousPageButton, nextPageButton, lastPageButton)) {
      button.addClassName("admin-user-pagination-arrow");
    }
    firstPageButton.addClickListener(event -> selectPage.accept(0));
    previousPageButton.addClickListener(event -> selectPage.accept(Math.max(0, currentPage - 1)));
    previousPageButton.addClassName("cosmos-pagination-previous");
    nextPageButton.addClassName("cosmos-pagination-next");
    nextPageButton.addClickListener(
        event -> selectPage.accept(Math.min(totalPages - 1, currentPage + 1)));
    lastPageButton.addClickListener(event -> selectPage.accept(totalPages - 1));
    summary.remove(totalLabel);
    if (mode == Mode.PREVIOUS_NEXT) {
      summary.remove(rowsLabel);
      add(totalLabel, summary, navigation);
    } else {
      add(summary, totalLabel, navigation);
    }
    refreshTexts();
  }

  public void showPage(int page, long totalItems, int size) {
    this.totalItems = totalItems;
    totalPages = Math.clamp(Math.ceilDiv(totalItems, (long) size), 1, Integer.MAX_VALUE);
    currentPage = Math.clamp(page, 0, totalPages - 1);
    refreshTexts();
  }

  public void refreshTexts() {
    rowsLabel.setText(messageProperties.getAdminUsersPaginationPerPage());
    pageSizeSelector.setAriaLabel(messageProperties.getAdminUsersPaginationPerPage());
    if (mode == Mode.PREVIOUS_NEXT) {
      pageSizeSelector.setTooltipText(messageProperties.getAdminUsersPaginationPerPage());
    }
    int size = pageSizeSelector.getValue();
    long from = totalItems == 0 ? 0 : (long) currentPage * size + 1;
    long to = Math.min(((long) currentPage + 1) * size, totalItems);
    totalLabel.setText(messageProperties.getPortalPaginationRange(from, to, totalItems));
    totalLabel.addClassName("admin-user-pagination-total");
    label(firstPageButton, messageProperties.getAdminUsersPaginationFirstPage());
    label(previousPageButton, messageProperties.getAdminUsersPaginationPreviousPage());
    label(nextPageButton, messageProperties.getAdminUsersPaginationNextPage());
    label(lastPageButton, messageProperties.getAdminUsersPaginationLastPage());
    firstPageButton.setEnabled(currentPage > 0);
    previousPageButton.setEnabled(currentPage > 0);
    nextPageButton.setEnabled(currentPage < totalPages - 1);
    lastPageButton.setEnabled(currentPage < totalPages - 1);
    pageNumberButtons.removeAll();
    if (mode == Mode.PREVIOUS_NEXT) {
      return;
    }
    int preceding = -1;
    for (int page : pageIndices(currentPage, totalPages)) {
      if (preceding >= 0 && page - preceding > 1) {
        Span gap = new Span("…");
        gap.addClassName("admin-user-pagination-ellipsis");
        pageNumberButtons.add(gap);
      }
      Button button = new Button(Integer.toString(page + 1), event -> selectPage.accept(page));
      button.addClassName("admin-user-pagination-number");
      button.setAriaLabel(messageProperties.getAdminUsersPaginationPage(page + 1));
      if (page == currentPage) {
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClassName("admin-user-pagination-current");
        button.getElement().setAttribute("aria-current", "page");
      }
      pageNumberButtons.add(button);
      preceding = page;
    }
  }

  private void label(Button button, String text) {
    button.setAriaLabel(text);
    button.setTooltipText(text);
  }

  public static List<Integer> pageIndices(int current, int total) {
    TreeSet<Integer> pages = new TreeSet<>();
    pages.add(0);
    pages.add(total - 1);
    int start = total <= 7 ? 0 : Math.clamp(current - 1L, 1, total - 4);
    int end = total <= 7 ? total - 1 : Math.clamp(current + 1L, 3, total - 2);
    for (int page = start; page <= end; page++) {
      pages.add(page);
    }
    return List.copyOf(pages);
  }
}
