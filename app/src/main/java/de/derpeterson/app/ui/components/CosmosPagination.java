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
  private final SerializableConsumer<Integer> selectPageConsumer;
  private final ComboBox<Integer> pageSizeSelector = new ComboBox<>();
  private final Span pageSizeLabelSpan = new Span();
  private final Span totalItemsSpan = new Span();
  private final HorizontalLayout pageNumberButtonsLayout = new HorizontalLayout();
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
      SerializableConsumer<Integer> selectPageConsumer,
      SerializableConsumer<Integer> selectPageSize) {
    this(messageProperties, selectPageConsumer, selectPageSize, List.of(10, 20, 50));
  }

  public CosmosPagination(
      MessageProperties messageProperties,
      SerializableConsumer<Integer> selectPageConsumer,
      SerializableConsumer<Integer> selectPageSize,
      List<Integer> pageSizes) {
    this(messageProperties, selectPageConsumer, selectPageSize, pageSizes, Mode.NUMBERED);
  }

  public CosmosPagination(
      MessageProperties messageProperties,
      SerializableConsumer<Integer> selectPageConsumer,
      SerializableConsumer<Integer> selectPageSize,
      List<Integer> pageSizes,
      Mode mode) {
    this.messageProperties = messageProperties;
    this.selectPageConsumer = selectPageConsumer;
    this.mode = mode;
    addClassName("admin-user-pagination");
    setWidthFull();
    setAlignItems(Alignment.CENTER);
    setPadding(false);
    pageSizeSelector.setItems(pageSizes);
    pageSizeSelector.setValue(pageSizes.getFirst());
    pageSizeSelector.setAllowCustomValue(false);
    CosmosComboBoxStyling.styleChoice(pageSizeSelector, Object::toString, true);
    pageSizeSelector.addClassName("portal-pagination-size");
    pageSizeSelector.setWidth("56px");
    pageSizeSelector.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        selectPageSize.accept(event.getValue());
      }
    });

    HorizontalLayout paginationSummaryLayout =
        new HorizontalLayout(pageSizeLabelSpan, pageSizeSelector, totalItemsSpan);
    paginationSummaryLayout.addClassName("admin-user-pagination-summary");
    paginationSummaryLayout.setAlignItems(Alignment.CENTER);
    pageNumberButtonsLayout.addClassName("admin-user-pagination-numbers");
    pageNumberButtonsLayout.setSpacing(false);
    pageNumberButtonsLayout.setAlignItems(Alignment.CENTER);

    HorizontalLayout pageNavigationLayout =
        new HorizontalLayout(
            firstPageButton,
            previousPageButton,
            pageNumberButtonsLayout,
            nextPageButton,
            lastPageButton);
    pageNavigationLayout.addClassName("admin-user-pagination-nav");
    pageNavigationLayout.setSpacing(false);
    pageNavigationLayout.setAlignItems(Alignment.CENTER);
    firstPageButton.setVisible(mode == Mode.NUMBERED);
    lastPageButton.setVisible(mode == Mode.NUMBERED);
    pageNumberButtonsLayout.setVisible(mode == Mode.NUMBERED);

    for (Button button :
        List.of(firstPageButton, previousPageButton, nextPageButton, lastPageButton)) {
      button.addClassName("admin-user-pagination-arrow");
    }
    firstPageButton.addClickListener(event -> selectPageConsumer.accept(0));
    previousPageButton.addClickListener(
        event -> selectPageConsumer.accept(Math.max(0, currentPage - 1)));
    previousPageButton.addClassName("cosmos-pagination-previous");
    nextPageButton.addClassName("cosmos-pagination-next");
    nextPageButton.addClickListener(
        event -> selectPageConsumer.accept(Math.min(totalPages - 1, currentPage + 1)));
    lastPageButton.addClickListener(event -> selectPageConsumer.accept(totalPages - 1));
    paginationSummaryLayout.remove(totalItemsSpan);

    if (mode == Mode.PREVIOUS_NEXT) {
      paginationSummaryLayout.remove(pageSizeLabelSpan);
      add(totalItemsSpan, paginationSummaryLayout, pageNavigationLayout);
    } else {
      add(paginationSummaryLayout, totalItemsSpan, pageNavigationLayout);
    }
    refreshTexts();
  }

  public void showPage(int pageIndex, long totalItems, int pageSize) {
    this.totalItems = totalItems;
    totalPages = Math.clamp(Math.ceilDiv(totalItems, (long) pageSize), 1, Integer.MAX_VALUE);
    currentPage = Math.clamp(pageIndex, 0, totalPages - 1);

    refreshTexts();
  }

  public void refreshTexts() {
    pageSizeLabelSpan.setText(messageProperties.getAdminUsersPaginationPerPage());
    pageSizeSelector.setAriaLabel(messageProperties.getAdminUsersPaginationPerPage());

    if (mode == Mode.PREVIOUS_NEXT) {
      pageSizeSelector.setTooltipText(messageProperties.getAdminUsersPaginationPerPage());
    }
    int selectedPageSize = pageSizeSelector.getValue();
    long firstDisplayedItemNumber = totalItems == 0 ? 0 : (long) currentPage * selectedPageSize + 1;
    long lastDisplayedItemNumber =
        Math.min(((long) currentPage + 1) * selectedPageSize, totalItems);
    totalItemsSpan.setText(
        messageProperties.getPortalPaginationRange(
            firstDisplayedItemNumber, lastDisplayedItemNumber, totalItems));
    totalItemsSpan.addClassName("admin-user-pagination-total");

    labelPageButton(firstPageButton, messageProperties.getAdminUsersPaginationFirstPage());
    labelPageButton(previousPageButton, messageProperties.getAdminUsersPaginationPreviousPage());
    labelPageButton(nextPageButton, messageProperties.getAdminUsersPaginationNextPage());
    labelPageButton(lastPageButton, messageProperties.getAdminUsersPaginationLastPage());

    firstPageButton.setEnabled(currentPage > 0);
    previousPageButton.setEnabled(currentPage > 0);
    nextPageButton.setEnabled(currentPage < totalPages - 1);
    lastPageButton.setEnabled(currentPage < totalPages - 1);

    pageNumberButtonsLayout.removeAll();

    if (mode == Mode.PREVIOUS_NEXT) {
      return;
    }
    int previousPageIndex = -1;

    for (int visiblePageIndex : pageIndices(currentPage, totalPages)) {
      if (previousPageIndex >= 0 && visiblePageIndex - previousPageIndex > 1) {
        Span pageGapSpan = new Span("…");
        pageGapSpan.addClassName("admin-user-pagination-ellipsis");
        pageNumberButtonsLayout.add(pageGapSpan);
      }
      Button pageNumberButton =
          new Button(
              Integer.toString(visiblePageIndex + 1),
              event -> selectPageConsumer.accept(visiblePageIndex));
      pageNumberButton.addClassName("admin-user-pagination-number");
      pageNumberButton.setAriaLabel(
          messageProperties.getAdminUsersPaginationPage(visiblePageIndex + 1));
      if (visiblePageIndex == currentPage) {
        pageNumberButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        pageNumberButton.addClassName("admin-user-pagination-current");
        pageNumberButton.getElement().setAttribute("aria-current", "page");
      }
      pageNumberButtonsLayout.add(pageNumberButton);
      previousPageIndex = visiblePageIndex;
    }
  }

  private void labelPageButton(Button pageNavigationButton, String pageButtonLabel) {
    pageNavigationButton.setAriaLabel(pageButtonLabel);
    pageNavigationButton.setTooltipText(pageButtonLabel);
  }

  public static List<Integer> pageIndices(int currentPageIndex, int totalPageCount) {
    TreeSet<Integer> visiblePageIndices = new TreeSet<>();
    visiblePageIndices.add(0);
    visiblePageIndices.add(totalPageCount - 1);
    int firstVisiblePageIndex =
        totalPageCount <= 7 ? 0 : Math.clamp(currentPageIndex - 1L, 1, totalPageCount - 4);
    int lastVisiblePageIndex =
        totalPageCount <= 7
            ? totalPageCount - 1
            : Math.clamp(currentPageIndex + 1L, 3, totalPageCount - 2);

    for (int pageIndex = firstVisiblePageIndex; pageIndex <= lastVisiblePageIndex; pageIndex++) {
      visiblePageIndices.add(pageIndex);
    }

    return List.copyOf(visiblePageIndices);
  }
}
