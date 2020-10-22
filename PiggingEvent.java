package com.sferion.whitewater.backend.domain;

import com.sferion.whitewater.backend.domain.enums.Acknowledgment;
import com.sferion.whitewater.backend.domain.enums.PiggingEventStatus;
import com.sferion.whitewater.backend.domain.enums.UnitOfMeasure;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.sferion.whitewater.backend.util.EntityUtil.SEQUENCE_ALLOCATION_SIZE;
import static com.sferion.whitewater.backend.util.EntityUtil.SEQUENCE_NAME;

@Entity
@Table(name = "pigging_event")
@SequenceGenerator(name = SEQUENCE_NAME, sequenceName = SEQUENCE_NAME, allocationSize = SEQUENCE_ALLOCATION_SIZE)
public class PiggingEvent implements RecordCreateUpdateWithAuth, CommonFields, Comparable<PiggingEvent> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_NAME)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pipeline_id", foreignKey = @ForeignKey(name = "pipeline_fk"), nullable = false)
    private Pipeline pipeline;

    @Column(name = "from_location", nullable = false)
    private String fromLocation;

    @Column(name = "to_location", nullable = false)
    private String toLocation;

    @Column(name = "date_sent", nullable = false)
    private Date dateSent; //NOTE: Hibernate does not support LocalDate in queries

    @Column(name = "sent_by", nullable = false)
    private String sentBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "sizing_ring_used", nullable = false)
    private Acknowledgment sizingRingUsed;

    @Column(name = "pig_type", nullable = false)
    private String pigType;

    @Column(name = "pig_size", nullable = false)
    private double pigSize;

    @Column(name = "durometer")
    private Double durometer;

    @Column(name = "date_retrieved")
    private LocalDate dateRetrieved;

    @Column(name = "retrieved_by")
    private String retrievedBy;

    @Column(name = "pigging_time")
    private Integer piggingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_fluid_used")
    private Acknowledgment batchFluidUsed;

    @Column(name = "batch_volume")
    private Double batchVolume;

    @Enumerated(EnumType.STRING)
    @Column(name = "units")
    private UnitOfMeasure units;

    @Column(name = "comment")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PiggingEventStatus status;

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

    public LocalDate getDateSent() {
        return dateSent != null ? new java.sql.Date(dateSent.getTime()).toLocalDate() : null;
    }

    public void setDateSent(LocalDate dateSent) {
        this.dateSent = java.sql.Date.valueOf(dateSent);
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public Acknowledgment getSizingRingUsed() {
        return sizingRingUsed;
    }

    public void setSizingRingUsed(Acknowledgment sizingRingUsed) {
        this.sizingRingUsed = sizingRingUsed;
    }

    public String getPigType() {
        return pigType;
    }

    public void setPigType(String pigType) {
        this.pigType = pigType;
    }

    public double getPigSize() {
        return pigSize;
    }

    public void setPigSize(double pigSize) {
        this.pigSize = pigSize;
    }

    public Double getDurometer() {
        return durometer;
    }

    public void setDurometer(Double durometer) {
        this.durometer = durometer;
    }

    public LocalDate getDateRetrieved() {
        return dateRetrieved;
    }

    public void setDateRetrieved(LocalDate dateRetrieved) {
        this.dateRetrieved = dateRetrieved;
    }

    public String getRetrievedBy() {
        return retrievedBy;
    }

    public void setRetrievedBy(String retrievedBy) {
        this.retrievedBy = retrievedBy;
    }

    public Integer getPiggingTime() {
        return piggingTime;
    }

    public void setPiggingTime(Integer piggingTime) {
        this.piggingTime = piggingTime;
    }

    public Acknowledgment getBatchFluidUsed() {
        return batchFluidUsed;
    }

    public void setBatchFluidUsed(Acknowledgment batchFluidUsed) {
        this.batchFluidUsed = batchFluidUsed;
    }

    public Double getBatchVolume() {
        return batchVolume;
    }

    public void setBatchVolume(Double batchVolume) {
        this.batchVolume = batchVolume;
    }

    public UnitOfMeasure getUnits() {
        return units;
    }

    public void setUnits(UnitOfMeasure units) {
        this.units = units;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PiggingEventStatus getStatus() {
        return status;
    }

    public void setStatus(PiggingEventStatus status) {
        this.status = status;
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

    public Pipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public String getPipelineRunName() {
        return pipeline.getPipelineRunName();
    }

    public String getDateSentString() {
        return new SimpleDateFormat("dd/MM/yyyy").format(dateSent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PiggingEvent)) return false;
        PiggingEvent that = (PiggingEvent) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PiggingEvent{" +
                "fromLocation='" + fromLocation + '\'' +
                ", toLocation='" + toLocation + '\'' +
                ", dateSent=" + dateSent +
                ", sentBy='" + sentBy + '\'' +
                ", sizingRingUsed=" + sizingRingUsed +
                ", pigType='" + pigType + '\'' +
                ", pigSize=" + pigSize +
                ", durometer=" + durometer +
                ", dateRetrieved=" + dateRetrieved +
                ", retrievedBy='" + retrievedBy + '\'' +
                ", piggingTime=" + piggingTime +
                ", batchFluidUsed=" + batchFluidUsed +
                ", batchVolume=" + batchVolume +
                ", units='" + units + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }

    @Override
    public int compareTo(@NotNull PiggingEvent o) {
        return dateSent.compareTo(o.dateSent);
    }
}
