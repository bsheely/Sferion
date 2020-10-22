package com.sferion.whitewater.ui.views.pigging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.admin.AttachmentAdmin;
import com.sferion.whitewater.backend.admin.PipelineAdmin;
import com.sferion.whitewater.backend.admin.PipelineRunAdmin;
import com.sferion.whitewater.backend.admin.PlantsAdmin;
import com.sferion.whitewater.backend.domain.*;
import com.sferion.whitewater.backend.domain.enums.*;
import com.sferion.whitewater.ui.MainLayout;
import com.sferion.whitewater.ui.SessionData;
import com.sferion.whitewater.ui.components.FlexBoxLayout;
import com.sferion.whitewater.ui.components.SearchBar;
import com.sferion.whitewater.ui.components.navigation.bar.AppBar;
import com.sferion.whitewater.ui.util.CommaDelimStringToListConverter;
import com.sferion.whitewater.ui.util.StringToDoubleConverter;
import com.sferion.whitewater.ui.views.ViewAttachmentsView;
import com.sferion.whitewater.ui.views.ViewFrame;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.NO;
import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.YES;
import static com.sferion.whitewater.backend.domain.enums.AttachmentType.PIPELINE;
import static com.sferion.whitewater.backend.domain.enums.PipelineStatus.INACTIVE;
import static com.sferion.whitewater.ui.views.pigging.SessionVariable.CURRENTLY_SELECTED_PLANT;
import static com.sferion.whitewater.ui.views.pigging.SetupPipelinesView.PAGE_NAME;
import static com.sferion.whitewater.ui.views.pigging.SetupPipelinesView.PAGE_TITLE;
import static com.vaadin.flow.component.icon.VaadinIcon.CLOSE_CIRCLE;
import static com.vaadin.flow.component.icon.VaadinIcon.FILE_TEXT_O;

@PageTitle(PAGE_TITLE)
@Route(value = PAGE_NAME, layout = MainLayout.class)
@CssImport(value="./styles/views/upload-grid.css", themeFor="vaadin-grid")
public class SetupPipelinesView extends ViewFrame implements AfterNavigationObserver {
    public static final String PAGE_NAME = "setupPipelines";
    public static final String PAGE_TITLE = "Setup Pipelines";
    private final PlantsAdmin plantsAdmin;
    private final PipelineRunAdmin pipelineRunAdmin;
    private final PipelineAdmin pipelineAdmin;
    private final AttachmentAdmin attachmentAdmin;
    private final Provider<SessionData> sessionDataProvider;
    private final ViewAttachmentsView viewAttachmentsView;
    private Select<Plants> plants;
    private Grid<Pipeline> grid;
    private ListDataProvider<Pipeline> dataProvider;
    private final List<Pipeline> pipelineList;
    private PipelineDetails pipelineDetails;

    @Inject
    public SetupPipelinesView(PlantsAdmin plantsAdmin, PipelineRunAdmin pipelineRunAdmin, PipelineAdmin pipelineAdmin, AttachmentAdmin attachmentAdmin, Provider<SessionData> sessionDataProvider, ViewAttachmentsView viewAttachmentsView) {
        this.plantsAdmin = plantsAdmin;
        this.pipelineRunAdmin = pipelineRunAdmin;
        this.pipelineAdmin = pipelineAdmin;
        this.attachmentAdmin = attachmentAdmin;
        this.sessionDataProvider = sessionDataProvider;
        this.viewAttachmentsView = viewAttachmentsView;
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        pipelineList = pipelineAdmin.getActivePipelinesByPlant(currentlySelectedPlant.getId());
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
        appBar.setTitle("Setup Pipelines");
        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL);
        appBar.addContextIconClickListener(e -> UI.getCurrent().navigate(PiggingView.class)); //TODO back to previous
    }

    private void init() {
        setViewHeader(createHeader());
        createGrid();
        pipelineDetails = new PipelineDetails();
        VerticalLayout layout = new VerticalLayout(grid, pipelineDetails.getLayout());
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, pipelineDetails.getLayout());
        setViewContent(layout);
    }

    private Component createHeader() {
        plants = new Select<>();
        List<Plants> items = plantsAdmin.listAll().stream().sorted(Comparator.comparing(Plants::getPlantDisplayName)).collect(Collectors.toList());
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
        searchText.addValueChangeListener(event -> dataProvider.addFilter(pipeline ->  StringUtils.containsIgnoreCase(pipeline.getPipelineRunName(), searchText.getValue())));
        searchText.setValueChangeMode(ValueChangeMode.EAGER);
        searchBar.setPlaceHolder("Search");
        searchBar.setActionText("New Pipeline" );
        searchBar.getActionButton().getElement().setAttribute("new-button", true);
        searchBar.addActionClickListener(e -> UI.getCurrent().navigate(NewPipelineView.class));
        FlexBoxLayout searchContainer = new FlexBoxLayout(searchBar);
        searchContainer.setWidthFull();
        return new VerticalLayout(selectLayout, searchContainer);
    }

    private void createGrid() {
        grid = new Grid<>();
        dataProvider = new ListDataProvider<>(pipelineList);
        grid.setDataProvider(dataProvider);
        grid.asSingleSelect().addValueChangeListener(e -> handleGridValueChange());
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.name]]'>[[item.name]]</div>")
                .withProperty("name", Pipeline::getPipelineRunName))
                .setHeader("Pipeline Run")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.license]]'>[[item.license]]</div>")
                .withProperty("license", Pipeline::getLicenseNumbersAsString))
                .setHeader("Pipeline License Number(s)")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.from]]'>[[item.from]]</div>")
                .withProperty("from", Pipeline::getFromLocation))
                .setHeader("From Location")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.to]]'>[[item.to]]</div>")
                .withProperty("to", Pipeline::getToLocation))
                .setHeader("To Location")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.length]]'>[[item.length]]</div>")
                .withProperty("length", Pipeline::getPipelineLength))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Length", "(km)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.diameter]]'>[[item.diameter]]</div>")
                .withProperty("diameter", Pipeline::getPipelineDiameter))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Diameter", "(mm)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.thickness]]'>[[item.thickness]]</div>")
                .withProperty("thickness", Pipeline::getPipelineThickness))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Thickness", "(mm)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.pressure]]'>[[item.pressure]]</div>")
                .withProperty("pressure", Pipeline::getMaxPressure))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s<br>%s</div>", "Max Operating", "Pressure", "(kPa)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.trap]]'>[[item.trap]]</div>")
                .withProperty("trap", Pipeline::getPigTrapSize))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Pig Trap Size", "(m3)"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.substance]]'>[[item.substance]]</div>")
                .withProperty("substance", Pipeline::getSubstance))
                .setHeader("Substance")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.status]]'>[[item.status]]</div>")
                .withProperty("status", Pipeline::getStatus))
                .setHeader("Status")
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.frequency]]'>[[item.frequency]]</div>")
                .withProperty("frequency", Pipeline::getPiggingFrequency))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Pigging", "Frequency"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(TemplateRenderer.<Pipeline> of("<div title='[[item.canPig]]'>[[item.canPig]]</div>")
                .withProperty("canPig", Pipeline::getCanPig))
                .setHeader(new Html(String.format("<div style='text-align:center;'>%s</div>", String.format("<div>%s<br>%s</div>", "Currently", "Can Pig"))))
                .setTextAlign(ColumnTextAlign.CENTER);
        grid.addComponentColumn(this::createAttachmentIcon).setHeader("Attachment")
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    private void handleGridValueChange() {
        Pipeline selectedPipeline = grid.asSingleSelect().getValue();
        if (selectedPipeline == null)
            return; //NOTE: object selected in grid is inexplicably null
        pipelineDetails.refreshUpload();
        pipelineDetails.setPipelineDetails(selectedPipeline);
    }

    private void updateGrid() {
        getGridItems();
        grid.getDataProvider().refreshAll();
        if (!pipelineList.isEmpty()) {
            grid.asSingleSelect().setValue(pipelineList.get(0));
            pipelineDetails.getLayout().setVisible(true);
        } else
            pipelineDetails.getLayout().setVisible(false);
    }

    private void getGridItems() {
        pipelineList.clear();
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        pipelineList.addAll(pipelineAdmin.getActivePipelinesByPlant(currentlySelectedPlant.getId()));
    }

    private Icon createAttachmentIcon(Pipeline pipeline) {
        List<Attachment> attachments = attachmentAdmin.getAttachmentsByTypeAndParentId(PIPELINE, pipeline.getId());
       if (!attachments.isEmpty()) {
           Icon attachmentIcon = new Icon(FILE_TEXT_O);
           attachmentIcon.getStyle().set("cursor", "pointer");
           attachmentIcon.addClickListener(event -> {
               UI.getCurrent().navigate(ViewAttachmentsView.class);
               viewAttachmentsView.init("Pipeline", attachments);
           });
           return attachmentIcon;
       } else {
           Icon blankIcon = new Icon();
           blankIcon.setColor("#ffffff00");
           return blankIcon;
       }
    }

    private String getExportFilename() {
        return "PipelineExport-" + sessionDataProvider.get().getCurrentUser().getCompany().getName() + "-" + LocalDate.now();
    }

    private byte[] getExportFileData() {
        /*
        PlanGlobal Systems Export       www.planglobal.ca
        Company:                        <Clients Company Name>
        Pipeline Run    Pipeline License    From Location   To Location     Length (km)     Diameter (mm)   Thickness (mm)  Max Operating Pressure (kPa)    Pig Trap Size (m3)  Substance   Status  Pigging Frequency   Can Currently Pig
         */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlanGlobal Systems Export" + "\t" + "www.planglobal.ca" + "\n")
                .append("Company:" + "\t").append(sessionDataProvider.get().getCurrentUser().getCompany().getName()).append("\n");
        stringBuilder.append("Pipeline Run" + "\t" + "Pipeline License" + "\t" + "From Location" + "\t" + "To Location" + "\t" + "Length (km)" + "\t" + "Diameter (mm)" + "\t" + "Thickness (mm)" + "\t" + "Max Operating Pressure (kPa)" + "\t" + "Pig Trap Size (m3)" + "\t" + "Substance" + "\t" + "Status" + "\t" + "Pigging Frequency" + "\t" + "Can Currently Pig" + "\n");
        for(Pipeline pipeline : pipelineList)
            stringBuilder.append(pipeline.getPipelineRunName()).append(",").append(pipeline.getLicenseNumbersAsString().replace(","," ")).append(",").append(pipeline.getFromLocation()).append(",").append(pipeline.getToLocation()).append(",").append(pipeline.getPipelineLength()).append(",").append(pipeline.getPipelineDiameter()).append(",").append(pipeline.getPipelineThickness()).append(",").append(pipeline.getMaxPressure()).append(",").append(pipeline.getPigTrapSize()).append(",").append(pipeline.getSubstance()).append(",").append(pipeline.getStatus()).append(",").append(pipeline.getPiggingFrequency()).append(",").append(pipeline.getCanPig()).append("\n");
        return stringBuilder.toString().getBytes();
    }

    @CssImport(value="./styles/views/upload-grid.css", themeFor="vaadin-grid")
    private class PipelineDetails {
        private final VerticalLayout layout;
        private Select<String> pipelineRuns;
        private TextField pipelineLicenseNumbers;
        private TextField fromLocation;
        private TextField toLocation;
        private TextField pipelineLength;
        private TextField pipelineDiameter;
        private TextField pipelineThickness;
        private TextField maxOperatingPressure;
        private TextField pigTrapSize;
        private Select<PipelineSubstance> substance;
        private Select<PipelineStatus> status;
        private Select<PiggingFrequency> piggingFrequency;
        private Select<Acknowledgment> canPig;
        private MultiFileMemoryBuffer buffer;
        private Grid<Attachment> attachmentGrid;
        private Button save;
        private VerticalLayout attachmentLayout;
        private HorizontalLayout buttonLayout;
        private String filename;
        private Dialog confirmDialog;
        private Binder<Pipeline> binder;

        public PipelineDetails() {
            binder = new Binder<>();
            attachmentLayout = createAttachmentLayout();
            buttonLayout = createButtonLayout();
            layout = new VerticalLayout(createPipelineDetails(), attachmentLayout, buttonLayout);
            layout.setWidth("50%");
        }

        public void refreshUpload() {
            layout.remove(attachmentLayout, buttonLayout);
            attachmentLayout = createAttachmentLayout();
            buttonLayout = createButtonLayout();
            layout.add(attachmentLayout, buttonLayout);
        }

        public VerticalLayout getLayout() {
            return layout;
        }

        private FormLayout createPipelineDetails() {
            FormLayout columnLayout = new FormLayout();
            columnLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("25em", 1),
                    new FormLayout.ResponsiveStep("40em", 3));
            pipelineRuns = new Select<>();
            pipelineRuns.setLabel("Pipeline Run");
            pipelineRuns.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineRuns, 1);
            columnLayout.add(new Html("<div> </div>"), 2);
            pipelineLicenseNumbers = new TextField("Pipeline License Number(s)");
            pipelineLicenseNumbers.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineLicenseNumbers, 3);
            fromLocation = new TextField("From Location");
            fromLocation.setRequiredIndicatorVisible(true);
            columnLayout.add(fromLocation, 1);
            toLocation = new TextField("To Location");
            toLocation.setRequiredIndicatorVisible(true);
            columnLayout.add(toLocation, 1);
            columnLayout.add(new Html("<div> </div>"), 1);
            pipelineLength = new TextField("Pipeline Length (km)");
            pipelineLength.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineLength, 1);
            pipelineDiameter = new TextField("Pipeline Diameter (mm");
            pipelineDiameter.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineDiameter, 1);
            pipelineThickness = new TextField("Pipeline Thickness (mm)");
            pipelineThickness.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineThickness, 1);
            maxOperatingPressure = new TextField("Max Operating Pressure (kPa)");
            maxOperatingPressure.setRequiredIndicatorVisible(true);
            columnLayout.add(maxOperatingPressure, 1);
            pigTrapSize = new TextField("Pig Trap Size (m3)");
            pigTrapSize.setRequiredIndicatorVisible(true);
            columnLayout.add(pigTrapSize, 1);
            substance = new Select<>();
            substance.setLabel("Substance");
            substance.setRequiredIndicatorVisible(true);
            substance.setItems(PipelineSubstance.getPipelineSubstances());
            columnLayout.add(substance, 1);
            status = new Select<>();
            status.setLabel("Status");
            status.setRequiredIndicatorVisible(true);
            status.setItems(PipelineStatus.getPipelineStatuses());
            columnLayout.add(status, 1);
            piggingFrequency = new Select<>();
            piggingFrequency.setLabel("Pigging Frequency");
            piggingFrequency.setRequiredIndicatorVisible(true);
            piggingFrequency.setItems(PiggingFrequency.getPiggingFrequencies());
            columnLayout.add(piggingFrequency, 1);
            canPig = new Select<>();
            canPig.setLabel("Currently Can Pig");
            canPig.setRequiredIndicatorVisible(true);
            canPig.setItems(YES,NO);
            columnLayout.add(canPig, 1);

            binder = new Binder<>();
            binder.forField(pipelineRuns).bind(Pipeline::getPipelineRunName,Pipeline::setPipelineRunName);
            binder.forField(pipelineLicenseNumbers).asRequired("").withConverter(new CommaDelimStringToListConverter()).bind(Pipeline::getLicenseNumbers,Pipeline::setLicenseNumbers);
            binder.forField(fromLocation).asRequired("").bind(Pipeline::getFromLocation,Pipeline::setFromLocation);
            binder.forField(toLocation).asRequired("").bind(Pipeline::getToLocation,Pipeline::setToLocation);
            binder.forField(pipelineLength).asRequired("").withConverter(new StringToDoubleConverter()).bind(Pipeline::getPipelineLength,Pipeline::setPipelineLength);
            binder.forField(pipelineDiameter).asRequired("").withConverter(new StringToDoubleConverter()).bind(Pipeline::getPipelineDiameter,Pipeline::setPipelineDiameter);
            binder.forField(pipelineThickness).asRequired("").withConverter(new StringToDoubleConverter()).bind(Pipeline::getPipelineThickness,Pipeline::setPipelineThickness);
            binder.forField(maxOperatingPressure).asRequired("").withConverter(new StringToDoubleConverter()).bind(Pipeline::getMaxPressure,Pipeline::setMaxPressure);
            binder.forField(pigTrapSize).asRequired("").withConverter(new StringToDoubleConverter()).bind(Pipeline::getPigTrapSize,Pipeline::setPigTrapSize);
            binder.forField(substance).bind(Pipeline::getSubstance,Pipeline::setSubstance);
            binder.forField(status).bind(Pipeline::getStatus,Pipeline::setStatus);
            binder.forField(piggingFrequency).bind(Pipeline::getPiggingFrequency,Pipeline::setPiggingFrequency);
            binder.forField(canPig).bind(Pipeline::getCanPig,Pipeline::setCanPig);
            binder.addValueChangeListener(e -> save.setEnabled(binder.validate().isOk()));
            return columnLayout;
        }

        private void setPipelineDetails(Pipeline pipeline) {
            Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
            List<PipelineRun> pipelineRuns = pipelineRunAdmin.getActivePipelineRunsByPlantId(currentlySelectedPlant.getId());
            List<String> pipelineRunNames = new ArrayList<>();
            for(PipelineRun pipelineRun : pipelineRuns)
                pipelineRunNames.add(pipelineRun.getName());
            this.pipelineRuns.setItems(pipelineRunNames);
            this.pipelineRuns.setValue(pipeline.getPipelineRunName());
            pipelineLicenseNumbers.setValue(String.join(",", pipeline.getLicenseNumbers()));
            fromLocation.setValue(pipeline.getFromLocation());
            toLocation.setValue(pipeline.getToLocation());
            pipelineLength.setValue(String.valueOf(pipeline.getPipelineLength()));
            pipelineDiameter.setValue(String.valueOf(pipeline.getPipelineDiameter()));
            pipelineThickness.setValue(String.valueOf(pipeline.getPipelineThickness()));
            maxOperatingPressure.setValue(String.valueOf(pipeline.getMaxPressure()));
            pigTrapSize.setValue(String.valueOf(pipeline.getPigTrapSize()));
            substance.setValue(pipeline.getSubstance());
            status.setValue(pipeline.getStatus());
            piggingFrequency.setValue(pipeline.getPiggingFrequency());
            canPig.setValue(pipeline.getCanPig());

            List<Attachment> attachments = attachmentAdmin.getAttachmentsByTypeAndParentId(PIPELINE, pipeline.getId());
            attachmentGrid.setItems(attachments);
            save.setEnabled(false);
        }

        private VerticalLayout createAttachmentLayout() {
            Span label = new Span("Attachments");
            label.getElement().getStyle().set("font-size", "var(--lumo-font-size-s)");
            label.getElement().getStyle().set("padding-bottom", "0");
            label.getElement().getStyle().set("color", "var(--lumo-secondary-text-color)");
            label.getElement().getStyle().set("font-weight", "500");
            VerticalLayout attachmentsLayout = new VerticalLayout(label);
            attachmentsLayout.getElement().getStyle().set("padding-left", "0");
            attachmentsLayout.getElement().getStyle().set("padding-top", "0");
            attachmentsLayout.getElement().getStyle().set("padding-bottom", "0");
            buffer = new MultiFileMemoryBuffer();
            Upload upload = new Upload(buffer);
            upload.getElement().getStyle().set("padding-top", "0");
            upload.getElement().getStyle().set("margin-top", "0");
            upload.setWidth("400px");
            upload.setDropAllowed(false);
            upload.addSucceededListener(this::succeededListener);
            upload.addFileRejectedListener(this::fileRejectedListener);

            attachmentGrid = new Grid<>();
            attachmentGrid.setHeightByRows(true);
            attachmentGrid.getElement().getStyle().set("margin-top", "0");
            attachmentGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
            attachmentGrid.setSelectionMode(Grid.SelectionMode.NONE);
            attachmentGrid.addComponentColumn(item -> createRemoveButton(attachmentGrid, item)).setFlexGrow(0).setWidth("55px").setClassNameGenerator(e -> "upload-grid");
            attachmentGrid.addComponentColumn(AttachmentComponent::new).setTextAlign(ColumnTextAlign.START).setClassNameGenerator(e -> "upload-grid");
            attachmentGrid.setClassName("upload-grid");
            attachmentsLayout.getElement().getStyle().set("color", "--lumo-primary-color-10pct");

            VerticalLayout uploadLayout = new VerticalLayout(upload, attachmentGrid);
            uploadLayout.getElement().getStyle().set("margin-top", "0");
            uploadLayout.getElement().getStyle().set("border", "1px solid black");
            uploadLayout.getElement().getStyle().set("border-radius", "var(--lumo-border-radius)");
            attachmentsLayout.add(uploadLayout);
            return attachmentsLayout;
        }

        private Button createRemoveButton(Grid<Attachment> grid, Attachment attachment) {
            Icon icon = new Icon(CLOSE_CIRCLE);
            icon.setSize("30px");
            Button button = new Button(icon, clickEvent -> {
                @SuppressWarnings("unchecked")
                ListDataProvider<Attachment> dataProvider = (ListDataProvider<Attachment>) grid.getDataProvider();
                dataProvider.getItems().remove(attachment);
                dataProvider.refreshAll();

                attachment.setStatus(AttachmentStatus.INACTIVE);
                attachment.markUpdated();
                attachmentAdmin.save(attachment, sessionDataProvider.get());
            });
            button.getElement().getStyle().set("width", "42px");
            return button;
        }

        private HorizontalLayout createButtonLayout() {
            save = new Button("Save");
            save.addClickListener(e -> handleSaveButtonClick());
            Button remove = new Button("Remove");
            remove.addClickListener(e -> confirmDialog.open());
            confirmDialog = new Dialog();
            confirmDialog.add(new Text("Are you sure you want to remove pipeline?"));
            confirmDialog.setCloseOnEsc(false);
            confirmDialog.setCloseOnOutsideClick(false);
            confirmDialog.setDraggable(true);
            confirmDialog.setResizable(true);
            Button confirm = new Button("Yes", event -> {
                confirmDialog.close();
                handleRemoveButtonClick();
            });
            confirmDialog.add(new HorizontalLayout(confirm, new Button("No", event -> confirmDialog.close())));
            return new HorizontalLayout(save, remove);
        }

        private void succeededListener(SucceededEvent event) {
            try {
                filename = event.getFileName();
                buffer.getInputStream(filename).readAllBytes();
            } catch (IOException e) {
                Notification notification = new Notification("Unable to read file: " + filename, 3000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            }
        }

        private void fileRejectedListener(FileRejectedEvent e) {
            Notification notification = new Notification(e.getErrorMessage(), 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
        }

        private void handleSaveButtonClick() {
            Pipeline selectedPipeline = grid.asSingleSelect().getValue();
            if (binder.writeBeanIfValid(selectedPipeline)) {
                selectedPipeline.markUpdated();
                pipelineAdmin.save(selectedPipeline, sessionDataProvider.get());
                Notification notification = new Notification("Pipeline has been updated", 5000);
                notification.open();
                updateGrid();
            }

            Set<String> filenames = buffer.getFiles();
            if (!filenames.isEmpty()) {
                List<Attachment> attachments = new ArrayList<>();
                for(String filename : filenames) {
                    try {
                        byte[] data = buffer.getInputStream(filename).readAllBytes();
                        attachments.add(new Attachment(filename, data, AttachmentType.PIPELINE, selectedPipeline.getId(), AttachmentStatus.ACTIVE));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
                attachmentAdmin.saveAttachments(attachments, sessionDataProvider.get());
            }
            save.setEnabled(false);
        }

        private void handleRemoveButtonClick() {
            Pipeline selectedPipeline = grid.asSingleSelect().getValue();
            selectedPipeline.setStatus(INACTIVE);
            selectedPipeline.setDateInactive(LocalDateTime.now());
            User currentUser = sessionDataProvider.get().getCurrentUser();
            selectedPipeline.setRemovedBy(currentUser.getName() + " " + currentUser.getLastName());
            pipelineAdmin.save(selectedPipeline, sessionDataProvider.get());

            List<Attachment> databaseAttachments = attachmentAdmin.getAttachmentsByTypeAndParentId(AttachmentType.PIPELINE, selectedPipeline.getId());
            for (Attachment attachment : databaseAttachments) {
                attachment.setStatus(AttachmentStatus.INACTIVE);
                attachment.markUpdated();
                attachmentAdmin.save(attachment, sessionDataProvider.get());
            }
            updateGrid();
        }

        public class AttachmentComponent extends Div {
            public AttachmentComponent(Attachment attachment) {
                setAttachment(attachment);
            }

            public void setAttachment(Attachment attachment) {
                setText(attachment.getFilename());
            }
        }
    }
}
