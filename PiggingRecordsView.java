package com.sferion.whitewater.ui.views.pigging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.admin.*;
import com.sferion.whitewater.backend.domain.*;
import com.sferion.whitewater.backend.domain.enums.Acknowledgment;
import com.sferion.whitewater.backend.domain.enums.PiggingEventStatus;
import com.sferion.whitewater.backend.domain.enums.UnitOfMeasure;
import com.sferion.whitewater.ui.MainLayout;
import com.sferion.whitewater.ui.SessionData;
import com.sferion.whitewater.ui.components.FlexBoxLayout;
import com.sferion.whitewater.ui.components.SearchBar;
import com.sferion.whitewater.ui.components.navigation.bar.AppBar;
import com.sferion.whitewater.ui.views.ViewFrame;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
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

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.NO;
import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.YES;
import static com.sferion.whitewater.ui.views.pigging.PiggingRecordsView.PAGE_NAME;
import static com.sferion.whitewater.ui.views.pigging.PiggingRecordsView.PAGE_TITLE;
import static com.sferion.whitewater.ui.views.pigging.SessionVariable.CURRENTLY_SELECTED_PLANT;

@PageTitle(PAGE_TITLE)
@Route(value = PAGE_NAME, layout = MainLayout.class)
public class PiggingRecordsView extends ViewFrame {
    public static final String PAGE_TITLE = "Pigging Records";
    public static final String PAGE_NAME = "piggingRecords";
    private final PlantsAdmin plantsAdmin;
    private final PiggingEventAdmin piggingEventAdmin;
    private final PipelineAdmin pipelineAdmin;
    private final PiggingUserAdmin piggingUserAdmin;
    private final Provider<SessionData> sessionDataProvider;
    private Select<Plants> plants;
    private DatePicker fromDate;
    private DatePicker toDate;
    private Grid<PiggingEvent> grid;
    private ListDataProvider<PiggingEvent> dataProvider;
    private List<PiggingEvent> piggingEventList;
    private PiggingEventDetails piggingEventDetails;

    @Inject
    public PiggingRecordsView(PlantsAdmin plantsAdmin, PiggingEventAdmin piggingEventAdmin, PipelineAdmin pipelineAdmin, PiggingUserAdmin piggingUserAdmin, Provider<SessionData> sessionDataProvider) {
        this.plantsAdmin = plantsAdmin;
        this.piggingEventAdmin = piggingEventAdmin;
        this.pipelineAdmin = pipelineAdmin;
        this.piggingUserAdmin = piggingUserAdmin;
        this.sessionDataProvider = sessionDataProvider;
        piggingEventList = new ArrayList<>();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        AppBar appBar = Objects.requireNonNull(MainLayout.get()).getAppBar();
        appBar.setTitle("Pigging Records");
        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL);
        appBar.addContextIconClickListener(e -> UI.getCurrent().navigate(PiggingView.class)); //TODO back to previous
        init();
    }

    private void init() {
        setViewHeader(createHeader());
        piggingEventDetails = new PiggingEventDetails();
        getGridItems();
        createGrid();
        Plants currentlySelectedPlant = (Plants)VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        plants.setValue(currentlySelectedPlant);
        VerticalLayout layout = new VerticalLayout(grid, piggingEventDetails.getLayout());
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, piggingEventDetails.getLayout());
        setViewContent(layout);
        updateGrid();
    }

    private Component createHeader() {
        Span fromLabel = new Span("From Date");
        fromLabel.getElement().getStyle().set("font-size","var(--lumo-font-size-s)");
        fromLabel.getElement().getStyle().set("color","var(--lumo-secondary-text-color)");
        fromDate = new DatePicker();
        fromDate.getElement().getStyle().set("margin-left","4px");
        fromDate.setValue(LocalDate.now().minusMonths(3));
        fromDate.addValueChangeListener(e -> handleDateValueChange());
        Span toLabel = new Span("To Date");
        toLabel.getElement().getStyle().set("font-size","var(--lumo-font-size-s)");
        toLabel.getElement().getStyle().set("color","var(--lumo-secondary-text-color)");
        toDate = new DatePicker();
        toDate.getElement().getStyle().set("margin-left","4px");
        toDate.setValue(LocalDate.now());
        fromDate.addValueChangeListener(e -> handleDateValueChange());

        plants = new Select<>();
        List<Plants> items = plantsAdmin.listAll().stream().sorted(Comparator.comparing(Plants::getPlantDisplayName)).collect(Collectors.toList());
        plants.setItems(items);
        plants.setItemLabelGenerator(Plants::getPlantDisplayName);
        plants.addValueChangeListener(e -> {
            VaadinSession.getCurrent().setAttribute(CURRENTLY_SELECTED_PLANT, plants.getValue());
            piggingEventDetails.handlePlantValueChange();
            updateGrid();
        });

        Select<Span> actions = new Select<>();
        actions.setPlaceholder("Actions");
        Span setupPipelineRuns = new Span("Setup Pipeline Runs");
        setupPipelineRuns.addClickListener(e -> UI.getCurrent().navigate(SetupPipelineRunsView.class));
        actions.add(setupPipelineRuns);
        Span setupPipelines = new Span("Setup Pipelines");
        setupPipelines.addClickListener(e -> UI.getCurrent().navigate(SetupPipelinesView.class));
        actions.add(setupPipelines);
        Span exportData = new Span("Export Data");
        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(new StreamResource(getExportFilename(), () -> new ByteArrayInputStream(getExportFileData())));
        buttonWrapper.wrapComponent(exportData);
        actions.add(buttonWrapper);

        HorizontalLayout controlsLayout = new HorizontalLayout(fromLabel,fromDate,toLabel,toDate,plants, actions);
        controlsLayout.setWidthFull();
        controlsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        controlsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER,fromLabel,toLabel);
        SearchBar searchBar = new SearchBar();
        TextField searchText = searchBar.getTextField();
        searchText.addValueChangeListener(event -> dataProvider.addFilter(piggingEvent -> StringUtils.containsIgnoreCase(piggingEvent.getPipeline().getPipelineRunName(), searchText.getValue())));
        searchText.setValueChangeMode(ValueChangeMode.EAGER);
        searchBar.setPlaceHolder("Search");
        searchBar.getActionButton().setVisible(false);
        FlexBoxLayout searchContainer = new FlexBoxLayout(searchBar);
        searchContainer.setWidthFull();
        return new VerticalLayout(controlsLayout, searchContainer);
    }

    private void handleDateValueChange() {
        Plants currentlySelectedPlant = (Plants)VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        piggingEventList = piggingEventAdmin.getActivePiggingEventsByPlantIdBetweenFromDateAndToDate(currentlySelectedPlant.getId(), fromDate.getValue(), toDate.getValue());
        updateGrid();
    }

    private void createGrid() {
        grid = new Grid<>();
        dataProvider = new ListDataProvider<>(piggingEventList);
        grid.setDataProvider(dataProvider);
        grid.asSingleSelect().addValueChangeListener(e -> handleGridValueChange());
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.name]]'>[[item.name]]</div>")
                .withProperty("name", PiggingEvent::getPipelineRunName))
                .setHeader("Pipeline Run")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.fromLocation]]'>[[item.fromLocation]]</div>")
                .withProperty("fromLocation", PiggingEvent::getFromLocation)).setHeader("From Location")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.toLocation]]'>[[item.toLocation]]</div>")
                .withProperty("toLocation", PiggingEvent::getToLocation))
                .setHeader("To Location")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.dateSent]]'>[[item.dateSent]]</div>")
                .withProperty("dateSent", PiggingEvent::getDateSentString))
                .setHeader("Date Pig Sent")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.sentBy]]'>[[item.sentBy]]</div>")
                .withProperty("sentBy", PiggingEvent::getSentBy))
                .setHeader("Sent By")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.pigType]]'>[[item.pigType]]</div>")
                .withProperty("pigType", PiggingEvent::getPigType))
                .setHeader("Pig Type")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.pigSize]]'>[[item.pigSize]]</div>")
                .withProperty("pigSize", PiggingEvent::getPigSize))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Pig Size", "(inch)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.dateRetrieved]]'>[[item.dateRetrieved]]</div>")
                .withProperty("dateRetrieved", PiggingEvent::getDateRetrievedString))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Date Pig", "Retrieved"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.retrievedBy]]'>[[item.retrievedBy]]</div>")
                .withProperty("retrievedBy", PiggingEvent::getRetrievedBy))
                .setHeader("Retrieved By")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.]]'>[[item.]]</div>")
                .withProperty("fluidUsed", PiggingEvent::getBatchFluidUsed))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Batch Fluid", "Used"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.fluidUsed]]'>[[item.fluidUsed]]</div>")
                .withProperty("volume", PiggingEvent::getBatchVolume))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Batch Volume", "(m3)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<PiggingEvent> of("<div title='[[item.comment]]'>[[item.comment]]</div>")
                .withProperty("comment", PiggingEvent::getComment))
                .setHeader("Comment")
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    private void handleGridValueChange() {
        PiggingEvent selectedPiggingEvent = grid.asSingleSelect().getValue();
        if (selectedPiggingEvent == null) {
            //NOTE: object selected in grid is inexplicably null
            return;
        }
        piggingEventDetails.setPiggingEventDetails(selectedPiggingEvent);
    }

    private void updateGrid() {
        getGridItems();
        grid.getDataProvider().refreshAll();
        if (!piggingEventList.isEmpty()) {
            grid.asSingleSelect().setValue(piggingEventList.get(0));
            piggingEventDetails.getLayout().setVisible(true);
        } else
            piggingEventDetails.getLayout().setVisible(false);
    }

    private void getGridItems() {
        piggingEventList.clear();
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        piggingEventList.addAll(piggingEventAdmin.getActivePiggingEventsByPlantIdBetweenFromDateAndToDate(currentlySelectedPlant.getId(), fromDate.getValue(), toDate.getValue()));
    }

    private String getExportFilename() {
        return "HistoryExport-" + sessionDataProvider.get().getCurrentUser().getCompany().getName() + "-" + LocalDate.now();
    }

    private byte[] getExportFileData() {
        //todo
        /*
        PlanGlobal Systems Export       www.planglobal.ca
        Company:                        <Clients Company Name>
        Pipeline Run    From Location    To Location    Date Pig Sent Sent By   Sizing Ring Used    Pig Type    Pig Size (inches)   Durometer   Date Pig Retrieved  Retrieved By    Pigging Time (minutes)  Batch Fluid Used    Batch Volume    Units   Comment
        */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlanGlobal Systems Export" + "\t" + "www.planglobal.ca" + "\n")
                .append("Company:" + "\t").append(sessionDataProvider.get().getCurrentUser().getCompany().getName()).append("\n");
        stringBuilder.append("Pipeline Run" + "\t" + "From Location" + "\t" + "To Location" + "\t" + "Date Pig Sent" + "\t" + "Sent By" + "\t" + "Sizing Ring Used" + "\t" + "Pig Type" + "\t" + "Pig Size (inches)" + "\t"  + "Durometer" + "\t"  + "Date Pig Retrieved" + "\t"  + "Retrieved By" + "\t"  + "Pigging Time (minutes)" + "\t"  + "Batch Fluid Used" + "\t"  + "Batch Volume" + "\t"  + "Units" + "\t"  + "Comment" + "\n");
        for (PiggingEvent piggingEvent : piggingEventList)
            stringBuilder.append(piggingEvent.getPipeline().getPipelineRunName()).append(",").append(piggingEvent.getFromLocation()).append(",").append(piggingEvent.getToLocation()).append(",").append(piggingEvent.getDateSent()).append(",").append(piggingEvent.getSentBy()).append(",").append(piggingEvent.getSizingRingUsed()).append(",").append(piggingEvent.getPigType()).append(",").append(piggingEvent.getPigSize()).append(",").append(piggingEvent.getDurometer()).append(",").append(piggingEvent.getDateRetrieved()).append(",").append(piggingEvent.getRetrievedBy()).append(",").append(piggingEvent.getPiggingTime()).append(",").append(piggingEvent.getBatchFluidUsed()).append(",").append(piggingEvent.getBatchVolume()).append(",").append(piggingEvent.getUnits()).append(",").append(piggingEvent.getComment()).append("\n");
        return stringBuilder.toString().getBytes();
    }

    private class PiggingEventDetails {
        private final VerticalLayout layout;
        private Select<Pipeline> pipeline;
        private Select<String> fromLocation;
        private Select<String> toLocation;
        private DatePicker dateSent;
        private Select<String> sentBy;
        private Select<Acknowledgment> sizingRingUsed;
        private TextField pigType;
        private NumberField pigSize;
        private NumberField durometer;
        private DatePicker dateRetrieved;
        private Select<String> retrievedBy;
        private IntegerField piggingTime;
        private Select<Acknowledgment> batchFluidUsed;
        private NumberField batchVolume;
        private Select<UnitOfMeasure> units;
        private TextField comment;
        private Button save;
        private Dialog confirmDialog;
        private Binder<PiggingEvent> binder;
        private PiggingEvent selectedPiggingEvent;

        public PiggingEventDetails() {
            layout = new VerticalLayout();
            layout.add(createPipelineDetails(),createButtonLayout());
            layout.setWidth("50%");
        }

        protected VerticalLayout getLayout() {
            return layout;
        }

        private void handlePlantValueChange() {
            pipeline.setItems(getPipelines());
            Set<String> locations = getPipelineLocations();
            fromLocation.setItems(locations);
            toLocation.setItems(locations);
        }

        private List<Pipeline> getPipelines() {
            Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
            return pipelineAdmin.getActivePipelinesByPlant(currentlySelectedPlant.getId());
        }

        private FormLayout createPipelineDetails() {
            FormLayout columnLayout = new FormLayout();
            columnLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("25em", 1),
                    new FormLayout.ResponsiveStep("32em", 2),
                    new FormLayout.ResponsiveStep("40em", 3));
            pipeline = new Select<>();
            pipeline.setLabel("Pipeline Run");
            pipeline.setRequiredIndicatorVisible(true);
            pipeline.setEmptySelectionAllowed(false);
            pipeline.setItems(getPipelines());
            columnLayout.add(pipeline,1);
            Set<String> locations = getPipelineLocations();
            fromLocation = new Select<>();
            fromLocation.setLabel("From Location");
            fromLocation.setRequiredIndicatorVisible(true);
            fromLocation.setEmptySelectionAllowed(false);
            fromLocation.setItems(locations);
            columnLayout.add(fromLocation,1);
            toLocation = new Select<>();
            toLocation.setLabel("To Location");
            toLocation.setRequiredIndicatorVisible(true);
            toLocation.setEmptySelectionAllowed(false);
            toLocation.setItems(locations);
            columnLayout.add(toLocation,1);
            dateSent = new DatePicker("Date Pig Sent");
            dateSent.setRequiredIndicatorVisible(true);
            dateSent.setValue(LocalDate.now());
            columnLayout.add(dateSent,1);
            sentBy = new Select<>();
            sentBy.setLabel("Sent By");
            sentBy.setRequiredIndicatorVisible(true);
            sentBy.setEmptySelectionAllowed(false);
            List<String> userNames = piggingUserAdmin.getUserNames();
            sentBy.setItems(userNames);
            columnLayout.add(sentBy,2);
            sizingRingUsed = new Select<>(YES, NO);
            sizingRingUsed.setLabel("Was a Sizing Ring Used?");
            sizingRingUsed.setRequiredIndicatorVisible(true);
            sizingRingUsed.setEmptySelectionAllowed(false);
            sizingRingUsed.setValue(YES);
            columnLayout.add(sizingRingUsed,1);
            columnLayout.add(new Html("<div> </div>"),2);
            pigType = new TextField("Pig Type");
            pigType.setRequiredIndicatorVisible(true);
            columnLayout.add(pigType,1);
            pigSize = new NumberField("Pig Size (inches)");
            pigSize.setRequiredIndicatorVisible(true);
            columnLayout.add(pigSize,1);
            durometer = new NumberField("Durometer");
            columnLayout.add(durometer,1);
            dateRetrieved = new DatePicker("Date Pig Retrieved");
            columnLayout.add(dateRetrieved,1);
            retrievedBy = new Select<>();
            retrievedBy.setLabel("Retrieved By");
            retrievedBy.setEmptySelectionAllowed(true);
            retrievedBy.setItems(userNames);
            columnLayout.add(retrievedBy,2);
            piggingTime = new IntegerField("Pigging Time (minutes)");
            columnLayout.add(piggingTime,1);
            columnLayout.add(new Html("<div> </div>"),2);
            batchFluidUsed = new Select<>(YES,NO);
            batchFluidUsed.setLabel("Batch Fluid Used?");
            batchFluidUsed.addValueChangeListener(e -> handleBatchFluidUsedValueChanged());
            batchFluidUsed.setEmptySelectionAllowed(true);
            columnLayout.add(batchFluidUsed,1);
            batchVolume = new NumberField("Batch Volume");
            batchVolume.setRequiredIndicatorVisible(true);
            batchVolume.setVisible(false);
            columnLayout.add(batchVolume,1);
            units = new Select<>();
            units.setLabel("Units");
            units.setRequiredIndicatorVisible(true);
            units.setItems(UnitOfMeasure.getVolumeUnitsOfMeasure());
            units.setEmptySelectionAllowed(true);
            units.setVisible(false);
            columnLayout.add(units,1);
            comment = new TextField("Comment");
            columnLayout.add(comment,3);

            binder = new Binder<>();
            binder.forField(pipeline).asRequired().bind(PiggingEvent::getPipeline,PiggingEvent::setPipeline);
            binder.forField(fromLocation).asRequired().bind(PiggingEvent::getFromLocation,PiggingEvent::setFromLocation);
            binder.forField(toLocation).asRequired().bind(PiggingEvent::getToLocation,PiggingEvent::setToLocation);
            binder.forField(dateSent).withValidator(date -> !date.isAfter(LocalDate.now()),"Date sent cannot be in the future").bind(PiggingEvent::getDateSent,PiggingEvent::setDateSent);
            binder.forField(sentBy).asRequired().bind(PiggingEvent::getSentBy,PiggingEvent::setSentBy);
            binder.forField(sizingRingUsed).bind(PiggingEvent::getSizingRingUsed,PiggingEvent::setSizingRingUsed);
            binder.forField(pigType).asRequired().bind(PiggingEvent::getPigType,PiggingEvent::setPigType);
            binder.forField(pigSize).asRequired().bind(PiggingEvent::getPigSize,PiggingEvent::setPigSize);
            binder.forField(durometer).bind(PiggingEvent::getDurometer,PiggingEvent::setDurometer);
            binder.forField(dateRetrieved).withValidator(date -> date == null || (!date.isAfter(LocalDate.now()) && !date.isBefore(dateSent.getValue())), "Date Retrieved cannot be in the future or before the date sent").bind(PiggingEvent::getDateRetrieved,PiggingEvent::setDateRetrieved);
            binder.forField(retrievedBy).bind(PiggingEvent::getRetrievedBy,PiggingEvent::setRetrievedBy);
            binder.forField(piggingTime).bind(PiggingEvent::getPiggingTime,PiggingEvent::setPiggingTime);
            binder.forField(batchFluidUsed).bind(PiggingEvent::getBatchFluidUsed,PiggingEvent::setBatchFluidUsed);
            Binder.Binding<PiggingEvent, Double> batchVolumeBinding = binder.forField(batchVolume).withValidator(value -> batchFluidUsed.getValue() == null || batchFluidUsed.getValue() == NO || value != null, "").bind(PiggingEvent::getBatchVolume, PiggingEvent::setBatchVolume);
            Binder.Binding<PiggingEvent, UnitOfMeasure> unitsBinding = binder.forField(units).withValidator(value -> batchFluidUsed.getValue() == null || batchFluidUsed.getValue() == NO || value != null, "").bind(PiggingEvent::getUnits, PiggingEvent::setUnits);
            batchFluidUsed.addValueChangeListener(event -> {
                batchVolumeBinding.validate();
                unitsBinding.validate();
            });
            binder.forField(comment).bind(PiggingEvent::getComment,PiggingEvent::setComment);
            binder.addValueChangeListener(e -> save.setEnabled(binder.validate().isOk()));
            return columnLayout;
        }

        public void setPiggingEventDetails(PiggingEvent piggingEvent) {
            selectedPiggingEvent = piggingEvent;
            pipeline.setValue(piggingEvent.getPipeline());
            fromLocation.setValue(piggingEvent.getFromLocation());
            toLocation.setValue(piggingEvent.getToLocation());
            dateSent.setValue(piggingEvent.getDateSent());
            sentBy.setValue(piggingEvent.getSentBy());
            sizingRingUsed.setValue(piggingEvent.getSizingRingUsed());
            pigType.setValue(piggingEvent.getPigType());
            pigSize.setValue(piggingEvent.getPigSize());
            durometer.setValue(piggingEvent.getDurometer());
            dateRetrieved.setValue(piggingEvent.getDateRetrieved());
            retrievedBy.setValue(piggingEvent.getRetrievedBy());
            piggingTime.setValue(piggingEvent.getPiggingTime());
            batchFluidUsed.setValue(piggingEvent.getBatchFluidUsed());
            batchVolume.setValue(piggingEvent.getBatchVolume());
            units.setValue(piggingEvent.getUnits());
            comment.setValue(piggingEvent.getComment());
            handleBatchFluidUsedValueChanged();
            save.setEnabled(false);
        }

        private Set<String> getPipelineLocations() {
            Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
            Set<String> locations = new HashSet<>();
            List<Pipeline> pipelines = pipelineAdmin.getActivePipelinesByPlant(currentlySelectedPlant.getId());
            for (Pipeline pipeline : pipelines) {
                locations.add(pipeline.getFromLocation());
                locations.add(pipeline.getToLocation());
            }
            return locations;
        }

        private HorizontalLayout createButtonLayout() {
            save = new Button("Save");
            save.addClickListener(e -> handleSaveButtonClick());
            Button remove = new Button("Remove");
            remove.addClickListener(e -> confirmDialog.open());
            confirmDialog = new Dialog();
            confirmDialog.add(new Text("Are you sure you want to remove pigging event?"));
            confirmDialog.setCloseOnEsc(false);
            confirmDialog.setCloseOnOutsideClick(false);
            confirmDialog.setDraggable(true);
            confirmDialog.setResizable(true);
            Button confirm = new Button("Yes", event -> {
                confirmDialog.close();
                handleRemoveButtonClick();
            });
            confirmDialog.add(new HorizontalLayout(confirm, new Button("No", event -> confirmDialog.close())));
            return new HorizontalLayout(save,remove);
        }

        private void handleBatchFluidUsedValueChanged() {
            Acknowledgment acknowledgment = batchFluidUsed.getValue();
            batchVolume.setVisible(acknowledgment == YES);
            units.setVisible(acknowledgment == YES);
        }

        private void handleSaveButtonClick() {
            PiggingEvent piggingEvent = grid.asSingleSelect().getValue();
            if (binder.writeBeanIfValid(piggingEvent)) {
                piggingEvent.markUpdated();
                piggingEventAdmin.save(piggingEvent, sessionDataProvider.get());
                Notification notification = new Notification("Pigging Event has been updated", 3000);
                notification.open();
            }
            save.setEnabled(false);
            updateGrid();
        }

        private void handleRemoveButtonClick() {
            selectedPiggingEvent.setStatus(PiggingEventStatus.INACTIVE);
            selectedPiggingEvent.setDateInactive(LocalDateTime.now());
            User currentUser = sessionDataProvider.get().getCurrentUser();
            selectedPiggingEvent.setRemovedBy(currentUser.getName() + " " + currentUser.getLastName());
            piggingEventAdmin.save(selectedPiggingEvent, sessionDataProvider.get());
            updateGrid();
        }
    }
}
