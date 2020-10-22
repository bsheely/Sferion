package com.sferion.whitewater.backend.admin;

import com.google.inject.Inject;
import com.sferion.whitewater.backend.domain.PiggingEvent;
import com.sferion.whitewater.backend.domain.Pipeline;
import com.sferion.whitewater.backend.service.CrudService;
import com.sferion.whitewater.ui.SessionData;
import org.apache.onami.persist.EntityManagerProvider;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class PiggingEventAdmin extends CrudService<PiggingEvent> {
    private final EntityManagerProvider entityManagerProvider;

    @Inject
    public PiggingEventAdmin(EntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManagerProvider.get();
    }

    @Override
    public Class<PiggingEvent> getEntityClass() {
        return PiggingEvent.class;
    }

    @Override
    public PiggingEvent createNew(SessionData currentSession) {
        PiggingEvent piggingEvent = new PiggingEvent();
        piggingEvent.setAuthLog(currentSession.getAuthLog());
        piggingEvent.markCreatedUpdated();
        return piggingEvent;
    }

    @Override
    public PiggingEvent load(long id) {
        return findPiggingEvent(id);
    }

    public PiggingEvent findPiggingEvent(long id) {
        return entityManagerProvider.get().find(PiggingEvent.class, id);
    }

    public List<PiggingEvent> getActivePiggingEventsByPlantIdBetweenFromDateAndToDate(long id, LocalDate from, LocalDate to) {
        Date fromDate = java.sql.Date.valueOf(from);
        Date toDate = java.sql.Date.valueOf(to);
        return entityManagerProvider.get()
                .createQuery("from PiggingEvent where status != 'INACTIVE' and plantId = :id and dateSent between :fromDate and :toDate order by pipeline asc", PiggingEvent.class)
                .setParameter("id", id)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
    }

    public List<PiggingEvent> getPiggingEventsByPipeline(Pipeline pipeline) {
        return entityManagerProvider.get()
                .createQuery("from PiggingEvent where status != 'INACTIVE' and pipeline=:pipeline order by dateSent asc", PiggingEvent.class)
                .setParameter("pipeline", pipeline)
                .getResultList();
    }

    public PiggingEvent getLastPiggingEventByPipeline(Pipeline pipeline) {
       List<PiggingEvent> piggingEvents = getPiggingEventsByPipeline(pipeline);
       return !piggingEvents.isEmpty() ? piggingEvents.get(0) : null;
    }
}
