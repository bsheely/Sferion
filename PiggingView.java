package com.sferion.whitewater.ui.views.pigging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.admin.PiggingEventAdmin;
import com.sferion.whitewater.backend.admin.PiggingUserAdmin;
import com.sferion.whitewater.backend.admin.PipelineAdmin;
import com.sferion.whitewater.backend.admin.PlantsAdmin;
import com.sferion.whitewater.backend.domain.*;
import com.sferion.whitewater.backend.domain.enums.Acknowledgment;
import com.sferion.whitewater.backend.domain.enums.UnitOfMeasure;
import com.sferion.whitewater.ui.MainLayout;
import com.sferion.whitewater.ui.SessionData;
import com.sferion.whitewater.ui.components.FlexBoxLayout;
import com.sferion.whitewater.ui.components.SearchBar;
import com.sferion.whitewater.ui.views.ViewFrame;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.NO;
import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.YES;
import static com.sferion.whitewater.backend.domain.enums.PiggingEventStatus.INACTIVE;
import static com.sferion.whitewater.ui.views.pigging.PiggingView.PAGE_NAME;
import static com.sferion.whitewater.ui.views.pigging.PiggingView.PAGE_TITLE;
import static com.sferion.whitewater.ui.views.pigging.SessionVariable.CURRENTLY_SELECTED_PLANT;

@PageTitle(PAGE_TITLE)
@Route(value = PAGE_NAME, layout = MainLayout.class)
public class PiggingView extends ViewFrame {
    public static final String PAGE_TITLE = "Pigging";
    public static final String PAGE_NAME = "pigging";
    private final PlantsAdmin plantsAdmin;
    private final PipelineAdmin pipelineAdmin;
    private final PiggingEventAdmin piggingEventAdmin;
    private final PiggingUserAdmin piggingUserAdmin;
    private final Provider<SessionData> sessionDataProvider;
    private Select<Plants> plants;
    private TreeGrid<HierarchicalPipeline> grid;
    private final List<HierarchicalPipeline> pipelineList;
    private PiggingEventDetails piggingEventDetails;

    @Inject
    public PiggingView(PlantsAdmin plantsAdmin, PipelineAdmin pipelineAdmin, PiggingEventAdmin piggingEventAdmin, PiggingUserAdmin piggingUserAdmin, Provider<SessionData> sessionDataProvider) {
        this.plantsAdmin = plantsAdmin;
        this.pipelineAdmin = pipelineAdmin;
        this.piggingEventAdmin = piggingEventAdmin;
        this.piggingUserAdmin = piggingUserAdmin;
        this.sessionDataProvider = sessionDataProvider;
        pipelineList = new ArrayList<>();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setViewHeader(createHeader());
        Plants currentlySelectedPlant = (Plants)VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        if (currentlySelectedPlant == null) {
            List<Plants> items = plantsAdmin.listAll().stream().sorted(Comparator.comparing(Plants::getPlantDisplayName)).collect(Collectors.toList());
            if (!items.isEmpty())
                currentlySelectedPlant = items.get(0);
        }
        plants.setValue(currentlySelectedPlant);
        getGridItems();
        createGrid();
        piggingEventDetails = new PiggingEventDetails();
        VerticalLayout layout = new VerticalLayout(grid, piggingEventDetails.getLayout());
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, piggingEventDetails.getLayout());
        setViewContent(layout);
        updateGrid();
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
        Span setupPipelineRuns = new Span("Setup Pipeline Runs");
        setupPipelineRuns.addClickListener(e -> UI.getCurrent().navigate(SetupPipelineRunsView.class));
        actions.add(setupPipelineRuns);
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
        searchText.addValueChangeListener(e -> {
            pipelineList.clear();
            if (searchText.getValue().isEmpty())
                pipelineList.addAll(pipelineAdmin.getHierarchicalPipelinesByPlant(plants.getValue().getId()));
            else
                pipelineList.addAll(pipelineAdmin.getHierarchicalPipelinesByPlantAndFilter(plants.getValue().getId(), searchText.getValue()));
            grid.getDataProvider().refreshAll();
            grid.expand(pipelineList);
        });
        searchText.setValueChangeMode(ValueChangeMode.EAGER);
        searchBar.setPlaceHolder("Search");
        searchBar.setActionText("Pigging Event" );
        searchBar.getActionButton().getElement().setAttribute("new-button", true);
        searchBar.addActionClickListener(e -> UI.getCurrent().navigate(NewPiggingEventView.class));
        FlexBoxLayout searchContainer = new FlexBoxLayout(searchBar);
        searchContainer.setWidthFull();
        return new VerticalLayout(selectLayout, searchContainer);
    }

    private void createGrid() {
        grid = new TreeGrid<>();
        HierarchicalDataProvider<HierarchicalPipeline, Void> dataProvider = new AbstractBackEndHierarchicalDataProvider<>() {
            @Override
            public int getChildCount(HierarchicalQuery<HierarchicalPipeline, Void> query) {
                return (int) pipelineList.stream().filter(i -> Objects.equals(query.getParent(), i.getParent())).count();
            }

            @Override
            public boolean hasChildren(HierarchicalPipeline item) {
                return pipelineList.stream().anyMatch(i -> Objects.equals(item, i.getParent()));
            }

            @Override
            protected Stream<HierarchicalPipeline> fetchChildrenFromBackEnd(HierarchicalQuery<HierarchicalPipeline, Void> query) {
                return pipelineList.stream().filter(i -> Objects.equals(query.getParent(), i.getParent()));
            }
        };
        grid.setDataProvider(dataProvider);
        grid.asSingleSelect().addValueChangeListener(e -> handleGridValueChange());
        grid.addComponentHierarchyColumn(e -> {
            Span column = new Span(e.getPipelineRunName());
            column.getStyle().set("font-weight", "bold");
            return column;
        });
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.from]]'>[[item.from]]</div>")
                .withProperty("from", HierarchicalPipeline::getFromLocation))
                .setHeader("From Location")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.to]]'>[[item.to]]</div>")
                .withProperty("to", HierarchicalPipeline::getToLocation))
                .setHeader("To Location")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.length]]'>[[item.length]]</div>")
                .withProperty("length", HierarchicalPipeline::getLength))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Length", "(km)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.diameter]]'>[[item.diameter]]</div>")
                .withProperty("diameter", HierarchicalPipeline::getDiameter))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Diameter", "(mm)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.substance]]'>[[item.substance]]</div>")
                .withProperty("substance", HierarchicalPipeline::getSubstance))
                .setHeader("Substance")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.status]]'>[[item.status]]</div>")
                .withProperty("status", HierarchicalPipeline::getStatus))
                .setHeader("Status")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.frequency]]'>[[item.frequency]]</div>")
                .withProperty("frequency", HierarchicalPipeline::getFrequency))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Pigging", "Frequency"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.last]]'>[[item.last]]</div>")
                .withProperty("last", HierarchicalPipeline::getLastPigDateString))
                .setHeader("Last Pigged Date")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<HierarchicalPipeline> of("<div title='[[item.next]]'>[[item.next]]</div>")
                .withProperty("next", HierarchicalPipeline::getNextPigDateString))
                .setHeader("Next Pig Date Target")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addComponentColumn(HierarchicalPipeline::getStatusFlag).setHeader("Status");
    }

    private void handleGridValueChange() {
        HierarchicalPipeline selectedPipeline = grid.asSingleSelect().getValue();
        if (selectedPipeline == null)
            return;
        piggingEventDetails.getLayout().setVisible(selectedPipeline.getLastPigDate() != null);
        piggingEventDetails.setPiggingEventDetails(selectedPipeline.getPipeline());
    }

    private void updateGrid() {
        if (grid != null) {
            getGridItems();
            grid.getDataProvider().refreshAll();
            if (pipelineList.size() >= 2) {
                piggingEventDetails.getLayout().setVisible(true);
                int index = 0;
                for (HierarchicalPipeline pipeline : pipelineList) {
                    grid.expand(pipeline);
                    if (grid.isExpanded(pipeline)) {
                        grid.asSingleSelect().setValue(pipelineList.get(++index));
                        break;
                    }
                    ++index;
                }
            } else
                piggingEventDetails.getLayout().setVisible(false);
        }
    }

    private void getGridItems() {
        pipelineList.clear();
        pipelineList.addAll(pipelineAdmin.getHierarchicalPipelinesByPlant(plants.getValue().getId()));
    }

    private String getExportFilename() {
        return "PiggingExport-" + sessionDataProvider.get().getCurrentUser().getCompany().getName() + "-" + LocalDate.now();
    }

    private byte[] getExportFileData() {
        /*
        PlanGlobal Systems Export   www.planglobal.ca
        Company:                    <Clients Company Name>
        Pipeline Run                From Location    To Location      Length (km)     Diameter (mm)     Substance   Status  Pigging Frequency   Last Pigged Date    Next Pig Date
         */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlanGlobal Systems Export" + "\t" + "www.planglobal.ca" + "\n")
                .append("Company:" + "\t").append(sessionDataProvider.get().getCurrentUser().getCompany().getName()).append("\n");
        stringBuilder.append("Pipeline Run" + "\t" + "From Location" + "\t" + "To Location" + "\t" + "Length (km)" + "\t" + "Diameter (mm)" + "\t" + "Substance" + "\t" + "Status" + "\t" + "Pigging Frequency" + "\t" + "Last Pigged Date" + "\t" + "Next Pig Date" + "\n");
        for(HierarchicalPipeline pipeline : pipelineList)
            stringBuilder.append(pipeline.getPipelineRunName()).append(",").append(pipeline.getFromLocation()).append(",").append(pipeline.getToLocation()).append(",").append(pipeline.getLength()).append(",").append(pipeline.getDiameter()).append(",").append(pipeline.getSubstance()).append(",").append(pipeline.getStatus()).append(",").append(pipeline.getFrequency()).append(",").append(pipeline.getLastPigDate()).append(",").append(pipeline.getNextPigDate()).append("\n");
        return stringBuilder.toString().getBytes();
    }

    private class PiggingEventDetails {
        private final VerticalLayout layout;
        private TextField pipelineRun;
        private TextField fromLocation;
        private TextField toLocation;
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
        private Dialog confirmDialog;
        private Button save;
        private Binder<PiggingEvent> binder;
        private PiggingEvent currentPiggingEvent;

        public PiggingEventDetails() {
            binder = new Binder<>();
            layout = new VerticalLayout(createPiggingEventDetails(), createButtonLayout());
            layout.setWidth("50%");
        }

        public VerticalLayout getLayout() {
            return layout;
        }

        private Component createPiggingEventDetails() {
            FormLayout columnLayout = new FormLayout();
            columnLayout.setResponsiveSteps(
                    new ResponsiveStep("25em", 1),
                    new ResponsiveStep("32em", 2),
                    new ResponsiveStep("40em", 3));
            pipelineRun = new TextField("Pipeline Run");
            pipelineRun.setReadOnly(true);
            pipelineRun.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineRun,1);
            fromLocation = new TextField("From Location");
            fromLocation.setReadOnly(true);
            fromLocation.setRequiredIndicatorVisible(true);
            columnLayout.add(fromLocation,1);
            toLocation = new TextField("To Location");
            toLocation.setReadOnly(true);
            toLocation.setRequiredIndicatorVisible(true);
            columnLayout.add(toLocation,1);
            dateSent = new DatePicker("Date Pig Sent");
            dateSent.setRequiredIndicatorVisible(true);
            columnLayout.add(dateSent,1);
            sentBy = new Select<>();
            sentBy.setLabel("Sent By");
            sentBy.setRequiredIndicatorVisible(true);
            List<String> userNames = piggingUserAdmin.getUserNames();
            sentBy.setItems(userNames);
            columnLayout.add(sentBy,2);
            sizingRingUsed = new Select<>(YES, NO);
            sizingRingUsed.setLabel("Was a Sizing Ring Used?");
            sizingRingUsed.setRequiredIndicatorVisible(true);
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
            retrievedBy.setItems(userNames);
            columnLayout.add(retrievedBy,2);
            piggingTime = new IntegerField("Pigging Time (minutes)");
            columnLayout.add(piggingTime,1);
            columnLayout.add(new Html("<div> </div>"),2);
            batchFluidUsed = new Select<>(YES,NO);
            batchFluidUsed.setLabel("Batch Fluid Used?");
            batchFluidUsed.addValueChangeListener(this::handleBatchFluidUsedValueChanged);
            columnLayout.add(batchFluidUsed,1);
            batchVolume = new NumberField("Batch Volume");
            batchVolume.setRequiredIndicatorVisible(true);
            columnLayout.add(batchVolume,1);
            units = new Select<>();
            units.setLabel("Units");
            units.setRequiredIndicatorVisible(true);
            units.setItems(UnitOfMeasure.getVolumeUnitsOfMeasure());
            columnLayout.add(units,1);
            comment = new TextField("Comment");
            columnLayout.add(comment,3);

            binder = new Binder<>();
            binder.forField(pipelineRun).bind(PiggingEvent::getPipelineRunName,null);
            binder.forField(fromLocation).bind(PiggingEvent::getFromLocation,null);
            binder.forField(toLocation).bind(PiggingEvent::getToLocation,null);
            binder.forField(dateSent).withValidator(date -> date != null && !date.isAfter(LocalDate.now()),"Date sent cannot be in the future").bind(PiggingEvent::getDateSent,PiggingEvent::setDateSent);
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

        private void setPiggingEventDetails(Pipeline pipeline) {
            PiggingEvent piggingEvent = piggingEventAdmin.getLastPiggingEventByPipeline(pipeline);
            if (piggingEvent == null) {
                layout.setVisible(false);
                return;
            }
            currentPiggingEvent = piggingEvent;
            layout.setVisible(true);
            pipelineRun.setValue(piggingEvent.getPipelineRunName());
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
            save.setEnabled(false);
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

        private void handleBatchFluidUsedValueChanged(HasValue.ValueChangeEvent<?> e) {
            Acknowledgment action = (Acknowledgment)e.getValue();
            batchVolume.setVisible(action == YES);
            units.setVisible(action == YES);
        }

        private void handleSaveButtonClick() {
            if (binder.writeBeanIfValid(currentPiggingEvent)) {
                currentPiggingEvent.markUpdated();
                piggingEventAdmin.save(currentPiggingEvent, sessionDataProvider.get());
                Notification notification = new Notification("Pigging event has been updated", 5000);
                notification.open();
            }
        }

        private void handleRemoveButtonClick() {
            if (binder.writeBeanIfValid(currentPiggingEvent)) {
                currentPiggingEvent.setStatus(INACTIVE);
                currentPiggingEvent.setDateInactive(LocalDateTime.now());
                User currentUser = sessionDataProvider.get().getCurrentUser();
                currentPiggingEvent.setRemovedBy(currentUser.getName() + " " + currentUser.getLastName());
                piggingEventAdmin.save(currentPiggingEvent, sessionDataProvider.get());
                updateGrid();
            }
        }
    }
}
