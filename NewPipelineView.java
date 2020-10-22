package com.sferion.whitewater.ui.views.pigging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.admin.AttachmentAdmin;
import com.sferion.whitewater.backend.admin.PipelineAdmin;
import com.sferion.whitewater.backend.admin.PipelineRunAdmin;
import com.sferion.whitewater.backend.domain.Attachment;
import com.sferion.whitewater.backend.domain.Pipeline;
import com.sferion.whitewater.backend.domain.PipelineRun;
import com.sferion.whitewater.backend.domain.Plants;
import com.sferion.whitewater.backend.domain.enums.*;
import com.sferion.whitewater.ui.MainLayout;
import com.sferion.whitewater.ui.SessionData;
import com.sferion.whitewater.ui.components.navigation.bar.AppBar;
import com.sferion.whitewater.ui.util.CommaDelimStringToListConverter;
import com.sferion.whitewater.ui.util.StringToDoubleConverter;
import com.sferion.whitewater.ui.views.ViewFrame;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;

import java.io.IOException;
import java.util.*;

import static com.sferion.whitewater.ui.views.pigging.NewPipelineView.PAGE_NAME;
import static com.sferion.whitewater.ui.views.pigging.NewPipelineView.PAGE_TITLE;
import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.*;
import static com.sferion.whitewater.ui.views.pigging.SessionVariable.CURRENTLY_SELECTED_PLANT;

@PageTitle(PAGE_TITLE)
@Route(value = PAGE_NAME, layout = MainLayout.class)
public class NewPipelineView extends ViewFrame {
    public static final String PAGE_TITLE = "New Pipeline";
    public static final String PAGE_NAME = "newPipeline";
    private final PipelineRunAdmin pipelineRunAdmin;
    private final PipelineAdmin pipelineAdmin;
    private final AttachmentAdmin attachmentAdmin;
    private final Provider<SessionData> sessionDataProvider;

    @Inject
    public NewPipelineView(PipelineRunAdmin pipelineRunAdmin, PipelineAdmin pipelineAdmin, AttachmentAdmin attachmentAdmin, Provider<SessionData> sessionDataProvider) {
        this.pipelineRunAdmin = pipelineRunAdmin;
        this.pipelineAdmin = pipelineAdmin;
        this.attachmentAdmin = attachmentAdmin;
        this.sessionDataProvider = sessionDataProvider;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();
        AppBar appBar = Objects.requireNonNull(MainLayout.get()).getAppBar();
        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL);
        appBar.addContextIconClickListener(e -> UI.getCurrent().navigate(SetupPipelinesView.class));
        appBar.setTitle("Pigging - New Pipeline");
    }

    private void init() {
        Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
        List<PipelineRun> pipelineRuns = pipelineRunAdmin.getActivePipelineRunsByPlantId(currentlySelectedPlant.getId());
        if (pipelineRuns.isEmpty()) {
            Notification notification = new Notification("There is no Pipeline Run for " + currentlySelectedPlant.getPlantDisplayName(), 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
            UI.getCurrent().navigate(SetupPipelineRunsView.class);
            return;
        }
        List<String> pipelineRunNames = new ArrayList<>();
        for(PipelineRun pipelineRun : pipelineRuns) {
            pipelineRunNames.add(pipelineRun.getName());
        }
        PipelineDetails pipelineDetails = new PipelineDetails(pipelineAdmin, attachmentAdmin, sessionDataProvider, pipelineRunNames);
        VerticalLayout layout = new VerticalLayout(pipelineDetails.getLayout());
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, pipelineDetails.getLayout());
        setViewContent(layout);
    }

    public static class PipelineDetails {
        private final PipelineAdmin pipelineAdmin;
        private final AttachmentAdmin attachmentAdmin;
        private final Provider<SessionData> sessionDataProvider;
        private final List<String> pipelineRunNames;
        private final VerticalLayout layout;
        private MultiFileMemoryBuffer buffer;
        private Button save;
        private String filename;
        private Binder<Pipeline> binder;

        public PipelineDetails(PipelineAdmin pipelineAdmin, AttachmentAdmin attachmentAdmin, Provider<SessionData> sessionDataProvider, List<String> pipelineRunNames) {
            this.pipelineAdmin = pipelineAdmin;
            this.attachmentAdmin = attachmentAdmin;
            this.sessionDataProvider = sessionDataProvider;
            this.pipelineRunNames = pipelineRunNames;
            layout = new VerticalLayout();
            layout.add(createPipelineDetails(), createAttachmentLayout(), createButtonLayout());
            layout.setWidth("50%");
        }

        protected VerticalLayout getLayout() {
            return layout;
        }

        private FormLayout createPipelineDetails() {
            FormLayout columnLayout = new FormLayout();
            columnLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("25em", 1),
                    new FormLayout.ResponsiveStep("40em", 3));
            Select<String> pipelineRuns = new Select<>();
            pipelineRuns.setEmptySelectionAllowed(false);
            pipelineRuns.setLabel("Pipeline Run");
            pipelineRuns.setRequiredIndicatorVisible(true);
            pipelineRuns.setItems(pipelineRunNames);
            pipelineRuns.setValue(pipelineRunNames.get(0));
            columnLayout.add(pipelineRuns, 1);
            columnLayout.add(new Html("<div> </div>"), 2);
            TextField pipelineLicenseNumbers = new TextField("Pipeline License Number(s)");
            pipelineLicenseNumbers.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineLicenseNumbers, 3);
            TextField fromLocation = new TextField("From Location");
            fromLocation.setRequiredIndicatorVisible(true);
            columnLayout.add(fromLocation, 1);
            TextField toLocation = new TextField("To Location");
            toLocation.setRequiredIndicatorVisible(true);
            columnLayout.add(toLocation, 1);
            columnLayout.add(new Html("<div> </div>"), 1);
            TextField pipelineLength = new TextField("Pipeline Length (km)");
            pipelineLength.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineLength, 1);
            TextField pipelineDiameter = new TextField("Pipeline Diameter (mm");
            pipelineDiameter.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineDiameter, 1);
            TextField pipelineThickness = new TextField("Pipeline Thickness (mm)");
            pipelineThickness.setRequiredIndicatorVisible(true);
            columnLayout.add(pipelineThickness, 1);
            TextField maxOperatingPressure = new TextField("Max Operating Pressure (kPa)");
            maxOperatingPressure.setRequiredIndicatorVisible(true);
            columnLayout.add(maxOperatingPressure, 1);
            TextField pigTrapSize = new TextField("Pig Trap Size (m3)");
            pigTrapSize.setRequiredIndicatorVisible(true);
            columnLayout.add(pigTrapSize, 1);
            Select<PipelineSubstance> substance = new Select<>();
            substance.setLabel("Substance");
            substance.setRequiredIndicatorVisible(true);
            substance.setItems(PipelineSubstance.getPipelineSubstances());
            substance.setValue(PipelineSubstance.getPipelineSubstances().get(0));
            columnLayout.add(substance, 1);
            Select<PipelineStatus> status = new Select<>();
            status.setLabel("Status");
            status.setRequiredIndicatorVisible(true);
            status.setItems(PipelineStatus.getPipelineStatuses());
            status.setValue(PipelineStatus.getPipelineStatuses().get(0));
            columnLayout.add(status, 1);
            Select<PiggingFrequency> piggingFrequency = new Select<>();
            piggingFrequency.setLabel("Pigging Frequency");
            piggingFrequency.setRequiredIndicatorVisible(true);
            piggingFrequency.setItems(PiggingFrequency.getPiggingFrequencies());
            piggingFrequency.setValue(PiggingFrequency.getPiggingFrequencies().get(0));
            columnLayout.add(piggingFrequency, 1);
            Select<Acknowledgment> canPig = new Select<>();
            canPig.setLabel("Currently Can Pig");
            canPig.setRequiredIndicatorVisible(true);
            canPig.setItems(YES,NO);
            canPig.setValue(YES);
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
            upload.getElement().getStyle().set("border", "1px solid black");
            upload.getElement().getStyle().set("border-radius", "var(--lumo-border-radius)");
            upload.setWidth("400px");
            upload.setDropAllowed(false);
            upload.addSucceededListener(this::succeededListener);
            upload.addFileRejectedListener(this::fileRejectedListener);
            attachmentsLayout.add(upload);
            return attachmentsLayout;
        }

        private HorizontalLayout createButtonLayout() {
            save = new Button("Save");
            save.addClickListener(e -> handleSaveButtonClick());
            save.setEnabled(false);
            Button cancel = new Button("Cancel");
            cancel.addClickListener(e -> handleCancelButtonClick());
            return new HorizontalLayout(save, cancel);
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
            Pipeline pipeline = new Pipeline();
            if (binder.writeBeanIfValid(pipeline)) {
                Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
                pipeline.setPlantId(currentlySelectedPlant.getId());
                pipelineAdmin.save(pipeline, sessionDataProvider.get());

                Set<String> filenames = buffer.getFiles();
                if (!filenames.isEmpty()) {
                    List<Attachment> attachments = new ArrayList<>();
                    for(String filename : filenames) {
                        try {
                            byte[] data = buffer.getInputStream(filename).readAllBytes();
                            attachments.add(new Attachment(filename, data, AttachmentType.PIPELINE, pipeline.getId(), AttachmentStatus.ACTIVE));
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    attachmentAdmin.saveAttachments(attachments, sessionDataProvider.get());
                }
                Notification notification = new Notification("Pipeline has been saved", 3000);
                notification.open();
                //save.setEnabled(false);
                clear();
            }
        }

        private void handleCancelButtonClick() {
            //clear();
            UI.getCurrent().navigate(SetupPipelinesView.class);
        }

        private void clear() {
            layout.removeAll();
            layout.add(createPipelineDetails(), createAttachmentLayout(), createButtonLayout());
        }
    }
}
