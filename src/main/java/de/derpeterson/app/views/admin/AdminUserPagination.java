package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.List;
import java.util.TreeSet;
import java.util.function.IntConsumer;

/** Rows-per-page summary and compact numbered navigation, separate from CRUD. */
final class AdminUserPagination extends HorizontalLayout {
  private final MessageProperties texts;
  private final IntConsumer selectPage;
  private final ComboBox<Integer> pageSize = new ComboBox<>();
  private final Span rowsLabel = new Span();
  private final Span totalLabel = new Span();
  private final HorizontalLayout numbers = new HorizontalLayout();
  private final Button first = new Button(VaadinIcon.ANGLE_DOUBLE_LEFT.create());
  private final Button previous = new Button(VaadinIcon.ANGLE_LEFT.create());
  private final Button next = new Button(VaadinIcon.ANGLE_RIGHT.create());
  private final Button last = new Button(VaadinIcon.ANGLE_DOUBLE_RIGHT.create());
  private int currentPage;
  private int totalPages = 1;
  private int totalItems;

  AdminUserPagination(MessageProperties texts, IntConsumer selectPage, IntConsumer selectPageSize) {
    this(texts, selectPage, selectPageSize, List.of(10, 20, 50));
  }

  AdminUserPagination(
      MessageProperties texts,
      IntConsumer selectPage,
      IntConsumer selectPageSize,
      List<Integer> pageSizes) {
    this.texts = texts;
    this.selectPage = selectPage;
    addClassName("admin-user-pagination");
    setWidthFull();
    setAlignItems(Alignment.CENTER);
    setPadding(false);
    pageSize.setItems(pageSizes);
    pageSize.setValue(pageSizes.getFirst());
    pageSize.setAllowCustomValue(false);
    pageSize.setWidth("80px");
    pageSize.addValueChangeListener(
        event -> {
          if (event.getValue() != null) selectPageSize.accept(event.getValue());
        });
    HorizontalLayout summary = new HorizontalLayout(rowsLabel, pageSize, totalLabel);
    summary.addClassName("admin-user-pagination-summary");
    summary.setAlignItems(Alignment.CENTER);
    numbers.addClassName("admin-user-pagination-numbers");
    numbers.setSpacing(false);
    numbers.setAlignItems(Alignment.CENTER);
    HorizontalLayout navigation = new HorizontalLayout(first, previous, numbers, next, last);
    navigation.addClassName("admin-user-pagination-nav");
    navigation.setSpacing(false);
    navigation.setAlignItems(Alignment.CENTER);
    for (Button button : List.of(first, previous, next, last)) {
      button.addClassName("admin-user-pagination-arrow");
    }
    first.addClickListener(event -> selectPage.accept(0));
    previous.addClickListener(event -> selectPage.accept(Math.max(0, currentPage - 1)));
    next.addClickListener(event -> selectPage.accept(Math.min(totalPages - 1, currentPage + 1)));
    last.addClickListener(event -> selectPage.accept(totalPages - 1));
    add(summary, navigation);
    refreshTexts();
  }

  void showPage(int page, int totalItems, int size) {
    this.totalItems = totalItems;
    totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
    currentPage = Math.min(Math.max(0, page), totalPages - 1);
    refreshTexts();
  }

  void refreshTexts() {
    rowsLabel.setText(texts.getAdminUsersPaginationPerPage());
    pageSize.setAriaLabel(texts.getAdminUsersPaginationPerPage());
    totalLabel.setText(texts.getAdminUsersPaginationTotal(totalItems));
    label(first, texts.getAdminUsersPaginationFirstPage());
    label(previous, texts.getAdminUsersPaginationPreviousPage());
    label(next, texts.getAdminUsersPaginationNextPage());
    label(last, texts.getAdminUsersPaginationLastPage());
    first.setEnabled(currentPage > 0);
    previous.setEnabled(currentPage > 0);
    next.setEnabled(currentPage < totalPages - 1);
    last.setEnabled(currentPage < totalPages - 1);
    numbers.removeAll();
    int preceding = -1;
    for (int page : pageIndices(currentPage, totalPages)) {
      if (preceding >= 0 && page - preceding > 1) {
        Span gap = new Span("…");
        gap.addClassName("admin-user-pagination-ellipsis");
        numbers.add(gap);
      }
      Button button = new Button(Integer.toString(page + 1), event -> selectPage.accept(page));
      button.addClassName("admin-user-pagination-number");
      button.setAriaLabel(texts.getAdminUsersPaginationPage(page + 1));
      if (page == currentPage) {
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClassName("admin-user-pagination-current");
        button.getElement().setAttribute("aria-current", "page");
      }
      numbers.add(button);
      preceding = page;
    }
  }

  private void label(Button button, String text) {
    button.setAriaLabel(text);
    button.setTooltipText(text);
  }

  static List<Integer> pageIndices(int current, int total) {
    TreeSet<Integer> pages = new TreeSet<>();
    pages.add(0);
    pages.add(total - 1);
    int start = total <= 7 ? 0 : Math.max(1, Math.min(current - 1, total - 4));
    int end = total <= 7 ? total - 1 : Math.min(total - 2, Math.max(current + 1, 3));
    for (int page = start; page <= end; page++) pages.add(page);
    return List.copyOf(pages);
  }
}
