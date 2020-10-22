package com.sferion.whitewater.ui.views.pigging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.admin.PipelineRunAdmin;
import com.sferion.whitewater.backend.admin.PlantsAdmin;
import com.sferion.whitewater.backend.domain.PipelineRun;
import com.sferion.whitewater.backend.domain.Plants;
import com.sferion.whitewater.backend.domain.User;
import com.sferion.whitewater.ui.MainLayout;
import com.sferion.whitewater.ui.SessionData;
import com.sferion.whitewater.ui.components.FlexBoxLayout;
import com.sferion.whitewater.ui.components.SearchBar;
import com.sferion.whitewater.ui.components.navigation.bar.AppBar;
import com.sferion.whitewater.ui.views.ViewFrame;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.sferion.whitewater.backend.domain.enums.PipelineRunStatus.*;
import static com.sferion.whitewater.ui.views.pigging.SessionVariable.CURRENTLY_SELECTED_PLANT;
import static com.sferion.whitewater.ui.views.pigging.SetupPipelineRunsView.PAGE_TITLE;
import static com.sferion.whitewater.ui.views.pigging.SetupPipelineRunsView.PAGE_NAME;

@PageTitle(PAGE_TITLE)
@Route(value = PAGE_NAME, layout = MainLayout.class)
public class SetupPipelineRunsView extends ViewFrame implements AfterNavigationObserver {
    public static final String PAGE_NAME = "setupPipelineRuns";
    public static final String PAGE_TITLE = "Setup Pipeline Runs";
    private final PlantsAdmin plantsAdmin;
    private final PipelineRunAdmin pipelineRunAdmin;
    private final Provider<SessionData> sessionDataProvider;
    private Select<Plants> plants;
    private Grid<PipelineRun> grid;
    private ListDataProvider<PipelineRun> dataProvider;
    private final List<PipelineRun> pipelineRunList;
    private VerticalLayout pipelineRunInput;
    private TextField pipelineRun;
    private Button save;
    private Button remove;
    private Dialog confirmDialog;
    private Button cancel;
    private PipelineRun selectedPipelineRun;
    private Binder<PipelineRun> binder;
    private enum Mode {NEW,EDIT}
    private Mode mode;

    @Inject
    public SetupPipelineRunsView(PlantsAdmin plantsAdmin, PipelineRunAdmin pipelineRunAdmin, Provider<SessionData> sessionDataProvider) {
        this.plantsAdmin = plantsAdmin;
        this.pipelineRunAdmin = pipelineRunAdmin;
        this.sessionDataProvider = sessionDataProvider;
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        pipelineRunList = pipelineRunAdmin.getActivePipelineRunsByPlantId(currentlySelectedPlant.getId());
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        plants.setValue(currentlySelectedPlant);
        getGridItems();
        updateGrid();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();
        AppBar appBar = Objects.requireNonNull(MainLayout.get()).getAppBar();
        appBar.setTitle("Setup Pipeline Runs");
        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL);
        appBar.addContextIconClickListener(e -> UI.getCurrent().navigate(PiggingView.class)); //TODO back to previous
    }

    private void init() {
        setViewHeader(createHeader());
        createGrid();
        createPipelineRunInput();
        setViewContent(new VerticalLayout(grid, pipelineRunInput));
        mode = Mode.EDIT;
    }

    private Component createHeader() {
        List<Plants> items = plantsAdmin.listAll().stream().sorted(Comparator.comparing(Plants::getPlantDisplayName)).collect(Collectors.toList());
        plants = new Select<>();
        plants.setItems(items);
        plants.setItemLabelGenerator(Plants::getPlantDisplayName);
        plants.addValueChangeListener(e -> {
            VaadinSession.getCurrent().setAttribute(CURRENTLY_SELECTED_PLANT, plants.getValue());
            updateGrid();
        });

        Select<Span> actions = new Select<>();
        actions.setPlaceholder("Actions");
        Span setupPipelines = new Span("Setup Pipelines");
        setupPipelines.addClickListener(e -> UI.getCurrent().navigate(SetupPipelinesView.class));
        actions.add(setupPipelines);
        Span reviewPiggingRecords = new Span("Review Pigging Records");
        reviewPiggingRecords.addClickListener(e -> UI.getCurrent().navigate(PiggingRecordsView.class));
        actions.add(reviewPiggingRecords);
        Span exportData = new Span("Export Data");
        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(new StreamResource(getExportFilename(), () -> new ByteArrayInputStream(getExportFileData())));
        buttonWrapper.wrapComponent(exportData);
        actions.add(buttonWrapper);

        HorizontalLayout selectLayout = new HorizontalLayout(plants, actions);
        selectLayout.setWidthFull();
        selectLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        SearchBar searchBar = new SearchBar();
        TextField searchText = searchBar.getTextField();
        searchText.addValueChangeListener(event -> dataProvider.addFilter(run ->  StringUtils.containsIgnoreCase(run.getName(), searchText.getValue())));
        searchText.setValueChangeMode(ValueChangeMode.EAGER);
        searchBar.setPlaceHolder("Search");
        searchBar.setActionText("New Run" );
        searchBar.getActionButton().getElement().setAttribute("new-button", true);
        searchBar.addActionClickListener(e -> {
            mode = Mode.NEW;
            cancel.setVisible(true);
            remove.setVisible(false);
            pipelineRun.clear();
            grid.asSingleSelect().setValue(null);
            selectedPipelineRun = null;
            pipelineRunInput.setVisible(true);
        });
        FlexBoxLayout searchContainer = new FlexBoxLayout(searchBar);
        searchContainer.setWidthFull();
        return new VerticalLayout(selectLayout, searchContainer);
    }

    private void createGrid() {
        grid = new Grid<>();
        dataProvider = new ListDataProvider<>(pipelineRunList);
        grid.setDataProvider(dataProvider);
        grid.setHeightByRows(true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addColumn(TemplateRenderer.<PipelineRun> of("<div title='[[item.name]]'>[[item.name]]</div>")
                .withProperty("name", PipelineRun::getName))
                .setHeader("Pipeline Run Name");
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (!grid.getSelectedItems().isEmpty() && mode == Mode.EDIT) {
                selectedPipelineRun = List.copyOf(grid.getSelectedItems()).get(0);
                pipelineRun.setValue(selectedPipelineRun.getName());
            }
        });
    }

    private void updateGrid() {
        getGridItems();
        grid.getDataProvider().refreshAll();
        if (!pipelineRunList.isEmpty()) {
            pipelineRunInput.setVisible(true);
            grid.asSingleSelect().setValue(pipelineRunList.get(0));
        } else
            pipelineRunInput.setVisible(false);
    }

    private void getGridItems() {
        pipelineRunList.clear();
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        //NOTE: plants.getValue() inexplicably returns null
        pipelineRunList.addAll(pipelineRunAdmin.getActivePipelineRunsByPlantId(currentlySelectedPlant.getId()));
    }

    private void createPipelineRunInput() {
        pipelineRun = new TextField("Pipeline Run Name");
        pipelineRun.setRequiredIndicatorVisible(true);
        pipelineRun.setWidth("400px");
        pipelineRun.addValueChangeListener(event -> {
            if (mode == Mode.NEW)
                save.setEnabled(binder.hasChanges() && binder.isValid());
            else
                save.setEnabled(binder.hasChanges() && binder.isValid() && !pipelineRun.getValue().equals(selectedPipelineRun.getName()));
        });
        pipelineRun.setValueChangeMode(ValueChangeMode.EAGER);
        binder = new Binder<>();
        binder.forField(pipelineRun).asRequired("").bind(PipelineRun::getName,PipelineRun::setName);

        save = new Button("Save");
        save.setEnabled(false);
        save.addClickListener(e -> handleSaveButtonClick());
        remove = new Button("Remove");
        remove.addClickListener(e -> confirmDialog.open());
        confirmDialog = new Dialog();
        confirmDialog.add(new Text("Are you sure you want to remove pipeline run?"));
        confirmDialog.setCloseOnEsc(false);
        confirmDialog.setCloseOnOutsideClick(false);
        confirmDialog.setDraggable(true);
        confirmDialog.setResizable(true);
        Button confirm = new Button("Yes", event -> {
            handleRemove();
            confirmDialog.close();
        });
        confirmDialog.add(new HorizontalLayout(confirm, new Button("No", event -> confirmDialog.close())));
        cancel = new Button("Cancel");
        cancel.addClickListener(e -> handleCancelButtonClick());
        cancel.setVisible(false);
        HorizontalLayout buttonLayout = new HorizontalLayout(save,remove,cancel);
        buttonLayout.setWidth("400px");
        pipelineRunInput = new VerticalLayout();
        pipelineRunInput.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER,pipelineRun);
        pipelineRunInput.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER,buttonLayout);
        pipelineRunInput.add(pipelineRun,buttonLayout);
        pipelineRunInput.setVisible(false);
    }

    private void handleSaveButtonClick() {
        Notification notification;
        if (cancel.isVisible()) {
            PipelineRun pipelineRun = new PipelineRun();
            if (binder.writeBeanIfValid(pipelineRun)) {
                pipelineRun.setPlantId(plants.getValue().getId());
                pipelineRun.setStatus(ACTIVE);
                pipelineRunAdmin.save(pipelineRun, sessionDataProvider.get());
                notification = new Notification("Pipeline Run has been saved", 5000);
                notification.open();
            }
        } else {
            if (binder.writeBeanIfValid(selectedPipelineRun)) {
                selectedPipelineRun.markUpdated();
                pipelineRunAdmin.save(selectedPipelineRun, sessionDataProvider.get());
                this.pipelineRun.clear();
                notification = new Notification("Pipeline Run has been updated", 5000);
                notification.open();
            }
        }
        mode = Mode.EDIT;
        save.setEnabled(false);
        remove.setVisible(true);
        cancel.setVisible(false);
        updateGrid();
    }

    private void handleRemove() {
        selectedPipelineRun.setStatus(INACTIVE);
        selectedPipelineRun.setDateInactive(LocalDateTime.now());
        User currentUser = sessionDataProvider.get().getCurrentUser();
        selectedPipelineRun.setRemovedBy(currentUser.getName() + " " + currentUser.getLastName());
        pipelineRunAdmin.save(selectedPipelineRun, sessionDataProvider.get());
        updateGrid();
    }

    private void handleCancelButtonClick(){
        mode = Mode.EDIT;
        pipelineRun.clear();
        remove.setVisible(true);
        cancel.setVisible(false);
        if (!grid.getSelectedItems().isEmpty()) {
            selectedPipelineRun = List.copyOf(grid.getSelectedItems()).get(0);
            pipelineRun.setValue(selectedPipelineRun.getName());
        }
    }

    private String getExportFilename() {
        return "PipelineRunExport-" + sessionDataProvider.get().getCurrentUser().getCompany().getName() + "-" + LocalDate.now();
    }

    private byte[] getExportFileData() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlanGlobal Systems Export" + "\t" + "www.planglobal.ca" + "\n")
                .append("Company:" + "\t").append(sessionDataProvider.get().getCurrentUser().getCompany().getName()).append("\n");
        stringBuilder.append("Plant" + "\t" + "Pipeline Run" + "\n");
        for(PipelineRun pipelineRun : pipelineRunList)
            stringBuilder.append(plants.getValue().getPlantDisplayName()).append(",").append(pipelineRun.getName()).append("\n");
        return stringBuilder.toString().getBytes();
    }
}

