package com.sferion.whitewater.backend.admin;

import com.google.inject.Inject;
import com.sferion.whitewater.backend.domain.PipelineRun;
import com.sferion.whitewater.backend.service.CrudService;
import com.sferion.whitewater.ui.SessionData;
import org.apache.onami.persist.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.List;

public class PipelineRunAdmin extends CrudService<PipelineRun> {
    private final EntityManagerProvider entityManagerProvider;

    @Inject
    public PipelineRunAdmin(EntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManagerProvider.get();
    }

    @Override
    public Class<PipelineRun> getEntityClass() {
        return PipelineRun.class;
    }

    @Override
    public PipelineRun createNew(SessionData currentSession) {
        PipelineRun pipelineRun = new PipelineRun();
        pipelineRun.setAuthLog(currentSession.getAuthLog());
        pipelineRun.markCreatedUpdated();
        return pipelineRun;
    }

    @Override
    public PipelineRun load(long id) {
        return findById(id);
    }

    public PipelineRun findById(long id) {
        return entityManagerProvider.get().find(PipelineRun.class, id);
    }

    public List<PipelineRun> getActivePipelineRunsByPlantId(long id) {
        return entityManagerProvider.get()
                .createQuery("from PipelineRun where status != 'INACTIVE' and plantId = :id order by name asc", PipelineRun.class)
                .setParameter("id", id)
                .getResultList();
    }
}
