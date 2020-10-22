package com.sferion.whitewater.backend.domain;

import com.sferion.whitewater.backend.domain.enums.PipelineRunStatus;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.sferion.whitewater.backend.util.EntityUtil.SEQUENCE_ALLOCATION_SIZE;
import static com.sferion.whitewater.backend.util.EntityUtil.SEQUENCE_NAME;

@Entity
@Table(name = "pipeline_run")
@SequenceGenerator(name = SEQUENCE_NAME, sequenceName = SEQUENCE_NAME, allocationSize = SEQUENCE_ALLOCATION_SIZE)
public class PipelineRun implements RecordCreateUpdateWithAuth, CommonFields {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SEQUENCE_NAME)
    @Column(name = "id")
    private long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PipelineRunStatus status;

    @Column(name = "date_inactive")
    private LocalDateTime dateInactive;

    @Column(name = "removed_by")
    private String removedBy;

    @JoinColumn(name = "id", foreignKey = @ForeignKey(name = "plants_fk"), nullable = false)
    private long plantId;

    @ManyToOne
    @JoinColumn(name = "auth_log_id", foreignKey = @ForeignKey(name = "auth_log_fk"), nullable = false)
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PipelineRunStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineRunStatus status) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PipelineRun)) return false;
        PipelineRun that = (PipelineRun) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PipelineRun{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", dateInactive=" + dateInactive +
                ", removedBy='" + removedBy + '\'' +
                '}';
    }
}
