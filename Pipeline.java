package com.sferion.whitewater.backend.domain;

import com.sferion.whitewater.backend.domain.enums.*;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.sferion.whitewater.backend.util.EntityUtil.SEQUENCE_ALLOCATION_SIZE;
import static com.sferion.whitewater.backend.util.EntityUtil.SEQUENCE_NAME;

@Entity
@Table(name = "pipeline")
@SequenceGenerator(name = SEQUENCE_NAME, sequenceName = SEQUENCE_NAME, allocationSize = SEQUENCE_ALLOCATION_SIZE)
public class Pipeline implements RecordCreateUpdateWithAuth, CommonFields, Comparable<Pipeline> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_NAME)
    @Column(name = "id")
    private Long id;

    @Column(name = "pipeline_run_name", nullable = false)
    private String pipelineRunName;

    @ElementCollection()
    @CollectionTable(name = "pipeline_license_number", joinColumns = @JoinColumn(name = "pipeline_id"))
    private List<String> licenseNumbers;

    @Column(name = "from_location", nullable = false)
    private String fromLocation;

    @Column(name = "to_location", nullable = false)
    private String toLocation;

    @Column(name = "pipeline_length", nullable = false)
    private double pipelineLength;

    @Column(name = "pipeline_diameter", nullable = false)
    private double pipelineDiameter;

    @Column(name = "pipeline_thickness", nullable = false)
    private double pipelineThickness;

    @Column(name = "max_pressure", nullable = false)
    private double maxPressure;

    @Column(name = "pig_trap_size", nullable = false)
    private double pigTrapSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "substance", nullable = false)
    private PipelineSubstance substance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PipelineStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "pigging_frequency", nullable = false)
    private PiggingFrequency piggingFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "can_pig", nullable = false)
    private Acknowledgment canPig;

    @Column(name = "date_inactive")
    private LocalDateTime dateInactive;

    @Column(name = "removed_by")
    private String removedBy;

    @JoinColumn(name = "id", foreignKey = @ForeignKey(name = "plants_fk"), nullable = false)
    private long plantId;

    @ManyToOne
    @JoinColumn(name = "auth_log_id", foreignKey = @ForeignKey(name = "fk_auth_log"), nullable = false)
    private AuthLog authLog;

    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "updated", nullable = false)
    private Instant updated;

    @Override
    public Long getValueFieldId() {
        return id;
    }

    @Override
    public AuthLog getAuthLog() {
        return authLog;
    }

    @Override
    public void setAuthLog(AuthLog authLog) {
        this.authLog = authLog;
    }

    @Override
    public Instant getCreated() {
        return created;
    }

    @Override
    public void setCreated(Instant created) {
        this.created = created;
    }

    @Override
    public Instant getUpdated() {
        return updated;
    }

    @Override
    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public Long getId() {
        return id;
    }

    public String getPipelineRunName() {
        return pipelineRunName;
    }

    public void setPipelineRunName(String pipelineRunName) {
        this.pipelineRunName = pipelineRunName;
    }

    public List<String> getLicenseNumbers() {
        return licenseNumbers;
    }

    public String getLicenseNumbersAsString() {
        return String.join(",",licenseNumbers);
    }

    public void setLicenseNumbers(List<String> licenseNumbers) {
        this.licenseNumbers = licenseNumbers;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public double getPipelineLength() {
        return pipelineLength;
    }

    public void setPipelineLength(double pipelineLength) {
        this.pipelineLength = pipelineLength;
    }

    public double getPipelineDiameter() {
        return pipelineDiameter;
    }

    public void setPipelineDiameter(float pipelineDiameter) {
        this.pipelineDiameter = pipelineDiameter;
    }

    public double getPipelineThickness() {
        return pipelineThickness;
    }

    public void setPipelineThickness(double pipelineThickness) {
        this.pipelineThickness = pipelineThickness;
    }

    public double getMaxPressure() {
        return maxPressure;
    }

    public void setMaxPressure(float maxPressure) {
        this.maxPressure = maxPressure;
    }

    public double getPigTrapSize() {
        return pigTrapSize;
    }

    public void setPigTrapSize(float pigTrapSize) {
        this.pigTrapSize = pigTrapSize;
    }

    public PipelineSubstance getSubstance() {
        return substance;
    }

    public void setSubstance(PipelineSubstance substance) {
        this.substance = substance;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineStatus status) {
        this.status = status;
    }

    public PiggingFrequency getPiggingFrequency() {
        return piggingFrequency;
    }

    public void setPiggingFrequency(PiggingFrequency piggingFrequency) {
        this.piggingFrequency = piggingFrequency;
    }

    public Acknowledgment getCanPig() {
        return canPig;
    }

    public void setCanPig(Acknowledgment canPig) {
        this.canPig = canPig;
    }

    public void setPipelineDiameter(double pipelineDiameter) {
        this.pipelineDiameter = pipelineDiameter;
    }

    public void setMaxPressure(double maxPressure) {
        this.maxPressure = maxPressure;
    }

    public void setPigTrapSize(double pigTrapSize) {
        this.pigTrapSize = pigTrapSize;
    }

    public LocalDateTime getDateInactive() {
        return dateInactive;
    }

    public void setDateInactive(LocalDateTime dateInactive) {
        this.dateInactive = dateInactive;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public long getPlantId() {
        return plantId;
    }

    public void setPlantId(long plantId) {
        this.plantId = plantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pipeline)) return false;
        Pipeline pipeline = (Pipeline) o;
        return getId().equals(pipeline.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return pipelineRunName;
    }

    @Override
    public int compareTo(@NotNull Pipeline o) {
        return getPipelineRunName().compareTo(o.getPipelineRunName());
    }
}
