package com.sferion.whitewater.ui.views.pigging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.admin.PiggingEventAdmin;
import com.sferion.whitewater.backend.admin.PiggingUserAdmin;
import com.sferion.whitewater.backend.admin.PipelineAdmin;
import com.sferion.whitewater.backend.domain.*;
import com.sferion.whitewater.backend.domain.enums.Acknowledgment;
import com.sferion.whitewater.backend.domain.enums.PiggingEventStatus;
import com.sferion.whitewater.backend.domain.enums.UnitOfMeasure;
import com.sferion.whitewater.ui.MainLayout;
import com.sferion.whitewater.ui.SessionData;
import com.sferion.whitewater.ui.components.navigation.bar.AppBar;
import com.sferion.whitewater.ui.views.ViewFrame;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.time.LocalDate;
import java.util.*;

import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.NO;
import static com.sferion.whitewater.backend.domain.enums.Acknowledgment.YES;
import static com.sferion.whitewater.ui.views.pigging.NewPiggingEventView.PAGE_TITLE;
import static com.sferion.whitewater.ui.views.pigging.SessionVariable.CURRENTLY_SELECTED_PLANT;

@PageTitle(PAGE_TITLE)
@Route(value = NewPiggingEventView.PAGE_NAME, layout = MainLayout.class)
public class NewPiggingEventView extends ViewFrame {
    public static final String PAGE_TITLE = "New Pigging Event";
    public static final String PAGE_NAME = "newPiggingEvent";
    private final PiggingEventAdmin piggingEventAdmin;
    private final PiggingUserAdmin piggingUserAdmin;
    private final PipelineAdmin pipelineAdmin;
    private final Provider<SessionData> sessionDataProvider;

    @Inject
    public NewPiggingEventView(PiggingEventAdmin piggingEventAdmin, PiggingUserAdmin piggingUserAdmin, PipelineAdmin pipelineAdmin, Provider<SessionData> sessionDataProvider) {
        this.piggingEventAdmin = piggingEventAdmin;
        this.piggingUserAdmin = piggingUserAdmin;
        this.pipelineAdmin = pipelineAdmin;
        this.sessionDataProvider = sessionDataProvider;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        PiggingEventDetails piggingEventDetails = new PiggingEventDetails();
        VerticalLayout layout = new VerticalLayout(piggingEventDetails.getLayout());
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, piggingEventDetails.getLayout());
        setViewContent(layout);
        AppBar appBar = Objects.requireNonNull(MainLayout.get()).getAppBar();
        appBar.setNaviMode(AppBar.NaviMode.CONTEXTUAL);
        appBar.addContextIconClickListener(e -> UI.getCurrent().navigate(PiggingView.class));
        appBar.setTitle("Pigging - New Pigging Event");
    }

    private class PiggingEventDetails {
        private final VerticalLayout layout;
        private DatePicker dateSent;
        private Select<Acknowledgment> batchFluidUsed;
        private NumberField batchVolume;
        private Select<UnitOfMeasure> units;
        private Button save;
        private Binder<PiggingEvent> binder;

        public PiggingEventDetails() {
            layout = new VerticalLayout();
            layout.add(createPipelineDetails(),createButtonLayout());
            layout.setWidth("50%");
            save.setEnabled(false);
        }

        protected VerticalLayout getLayout() {
            return layout;
        }

        private FormLayout createPipelineDetails() {
            FormLayout columnLayout = new FormLayout();
            columnLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("25em", 1),
                    new FormLayout.ResponsiveStep("32em", 2),
                    new FormLayout.ResponsiveStep("40em", 3));
            Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
            Select<Pipeline> pipelines = new Select<>();
            pipelines.setLabel("Pipeline Run");
            pipelines.setRequiredIndicatorVisible(true);
            pipelines.setEmptySelectionAllowed(false);
            pipelines.setItemLabelGenerator(Pipeline::getPipelineRunName);
            pipelines.setItems(pipelineAdmin.getActivePipelinesByPlant(currentlySelectedPlant.getId()));
            columnLayout.add(pipelines,1);
            Set<String> locations = getPipelineLocations(currentlySelectedPlant);
            Select<String> fromLocation = new Select<>();
            fromLocation.setLabel("From Location");
            fromLocation.setRequiredIndicatorVisible(true);
            fromLocation.setEmptySelectionAllowed(false);
            fromLocation.setItems(locations);
            columnLayout.add(fromLocation,1);
            Select<String> toLocation = new Select<>();
            toLocation.setLabel("To Location");
            toLocation.setRequiredIndicatorVisible(true);
            toLocation.setEmptySelectionAllowed(false);
            toLocation.setItems(locations);
            columnLayout.add(toLocation,1);
            dateSent = new DatePicker("Date Pig Sent");
            dateSent.setRequiredIndicatorVisible(true);
            dateSent.setValue(LocalDate.now());
            columnLayout.add(dateSent,1);
            Select<String> sentBy = new Select<>();
            sentBy.setLabel("Sent By");
            sentBy.setRequiredIndicatorVisible(true);
            sentBy.setEmptySelectionAllowed(false);
            List<String> userNames = piggingUserAdmin.getUserNames();
            sentBy.setItems(userNames);
            columnLayout.add(sentBy,2);
            Select<Acknowledgment> sizingRingUsed = new Select<>(YES, NO);
            sizingRingUsed.setLabel("Was a Sizing Ring Used?");
            sizingRingUsed.setRequiredIndicatorVisible(true);
            sizingRingUsed.setEmptySelectionAllowed(false);
            sizingRingUsed.setValue(YES);
            columnLayout.add(sizingRingUsed,1);
            columnLayout.add(new Html("<div> </div>"),2);
            TextField pigType = new TextField("Pig Type");
            pigType.setRequiredIndicatorVisible(true);
            columnLayout.add(pigType,1);
            NumberField pigSize = new NumberField("Pig Size (inches)");
            pigSize.setRequiredIndicatorVisible(true);
            columnLayout.add(pigSize,1);
            NumberField durometer = new NumberField("Durometer");
            columnLayout.add(durometer,1);
            DatePicker dateRetrieved = new DatePicker("Date Pig Retrieved");
            columnLayout.add(dateRetrieved,1);
            Select<String> retrievedBy = new Select<>();
            retrievedBy.setLabel("Retrieved By");
            retrievedBy.setEmptySelectionAllowed(true);
            retrievedBy.setItems(userNames);
            columnLayout.add(retrievedBy,2);
            IntegerField piggingTime = new IntegerField("Pigging Time (minutes)");
            columnLayout.add(piggingTime,1);
            columnLayout.add(new Html("<div> </div>"),2);
            batchFluidUsed = new Select<>(YES,NO);
            batchFluidUsed.setLabel("Batch Fluid Used?");
            batchFluidUsed.addValueChangeListener(this::handleBatchFluidUsedValueChanged);
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
            TextField comment = new TextField("Comment");
            columnLayout.add(comment,3);

            binder = new Binder<>();
            binder.forField(pipelines).asRequired().bind(PiggingEvent::getPipeline,PiggingEvent::setPipeline);
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

        private Set<String> getPipelineLocations(Plants plant) {
            Set<String> locations = new HashSet<>();
            List<Pipeline> pipelines = pipelineAdmin.getActivePipelinesByPlant(plant.getId());
            for (Pipeline pipeline : pipelines) {
                locations.add(pipeline.getFromLocation());
                locations.add(pipeline.getToLocation());
            }
            return locations;
        }

        private HorizontalLayout createButtonLayout() {
            save = new Button("Save");
            save.addClickListener(e -> handleSaveButtonClick());
            Button cancel = new Button("Cancel");
            cancel.addClickListener(e -> handleCancelButtonClick());
            return new HorizontalLayout(save, cancel);
        }

        private void handleBatchFluidUsedValueChanged(HasValue.ValueChangeEvent<?> e) {
            Acknowledgment action = (Acknowledgment)e.getValue();
            batchVolume.setVisible(action == YES);
            units.setVisible(action == YES);
        }

        private void handleSaveButtonClick() {
        PiggingEvent piggingEvent = new PiggingEvent();
        if (binder.writeBeanIfValid(piggingEvent)) {
            piggingEvent.setStatus(PiggingEventStatus.ACTIVE);
            Plants currentlySelectedPlant = (Plants) VaadinSession.getCurrent().getAttribute(CURRENTLY_SELECTED_PLANT);
            piggingEvent.setPlantId(currentlySelectedPlant.getId());
            piggingEventAdmin.save(piggingEvent, sessionDataProvider.get());
            Notification notification = new Notification("Pigging Event has been saved", 3000);
            notification.open();
        }
            clear();
        }

        private void handleCancelButtonClick() {
            UI.getCurrent().navigate(PiggingView.class);
        }

        private void clear() {
            layout.removeAll();
            layout.add(createPipelineDetails(),createButtonLayout());
        }
    }
}
