package com.sferion.whitewater.backend.admin;

import com.google.inject.Inject;
import com.sferion.whitewater.backend.domain.HierarchicalPipeline;
import com.sferion.whitewater.backend.domain.PiggingEvent;
import com.sferion.whitewater.backend.domain.Pipeline;
import com.sferion.whitewater.backend.domain.PipelineRun;
import com.sferion.whitewater.backend.service.CrudService;
import com.sferion.whitewater.ui.SessionData;
import org.apache.onami.persist.EntityManagerProvider;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.sferion.whitewater.ui.views.admin.UserEditAdminView.distinctByKey;

public class PipelineAdmin extends CrudService<Pipeline> {
    private final PipelineRunAdmin pipelineRunAdmin;
    private final PiggingEventAdmin piggingEventAdmin;
    private final EntityManagerProvider entityManagerProvider;

    @Inject
    public PipelineAdmin(PipelineRunAdmin pipelineRunAdmin, PiggingEventAdmin piggingEventAdmin, EntityManagerProvider entityManagerProvider) {
        this.pipelineRunAdmin = pipelineRunAdmin;
        this.piggingEventAdmin = piggingEventAdmin;
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManagerProvider.get();
    }

    @Override
    public Class<Pipeline> getEntityClass() {
        return Pipeline.class;
    }

    @Override
    public Pipeline createNew(SessionData currentSession) {
        Pipeline pipeline = new Pipeline();
        pipeline.setAuthLog(currentSession.getAuthLog());
        pipeline.markCreatedUpdated();
        return pipeline;
    }

    @Override
    public Pipeline load(long id) {
        return findById(id);
    }

    public Pipeline findById(long id) {
        return entityManagerProvider.get().find(Pipeline.class, id);
    }

    public List<Pipeline> getActivePipelinesByPlant(long plantId) {
        return entityManagerProvider.get()
                .createQuery("from Pipeline where status != 'INACTIVE' and plantId = :id order by pipelineRunName asc", Pipeline.class)
                .setParameter("id", plantId)
                .getResultList();
    }

    public List<Pipeline> getActivePipelinesByPlantAndRunName(long plantId, String runName) {
        return entityManagerProvider.get()
                .createQuery("from Pipeline where status != 'INACTIVE' and plantId = :id and pipelineRunName = :name order by pipelineRunName asc", Pipeline.class)
                .setParameter("id", plantId)
                .setParameter("name", runName)
                .getResultList();
    }

    public List<Pipeline> getActivePipelinesByPlantAndFilter(long plantId, String runName, String filter) {
        return entityManagerProvider.get()
                .createQuery("from Pipeline where status != 'INACTIVE' and plantId = :id and pipelineRunName = :name and (" +
                        "lower(fromLocation) like '%" + filter.toLowerCase() + "%'" +
                        " or lower(toLocation) like '%" + filter.toLowerCase() + "%'" +
                        " or lower(substance) like '%" + filter.toLowerCase() + "%'" +
                        " or lower(status) like '%" + filter.toLowerCase() + "%'" +
                        " or lower(piggingFrequency) like '%" + filter.toLowerCase() + "%'" +
                        ") order by pipelineRunName asc", Pipeline.class)
                .setParameter("id", plantId)
                .setParameter("name", runName)
                .getResultList();
    }

    public List<HierarchicalPipeline> getHierarchicalPipelinesByPlantAndFilter(long plantId, String filter) {
        List<HierarchicalPipeline> hierarchicalPipelines = new ArrayList<>();
        List<PipelineRun> pipelineRuns = pipelineRunAdmin.getActivePipelineRunsByPlantId(plantId).stream().filter(distinctByKey(PipelineRun::getName)).sorted(Comparator.comparing(PipelineRun::getName)).collect(Collectors.toList());

        for (PipelineRun pipelineRun : pipelineRuns) {
            HierarchicalPipeline parent = new HierarchicalPipeline();
            parent.setPipelineRunName(pipelineRun.getName());
            hierarchicalPipelines.add(parent);
            List<Pipeline> pipelines = getActivePipelinesByPlantAndFilter(plantId, pipelineRun.getName(), filter);
            for (Pipeline pipeline : pipelines) {
                HierarchicalPipeline hierarchicalPipeline = getHierarchicalPipeline(pipeline, parent);
                hierarchicalPipelines.add(hierarchicalPipeline);
            }
        }
        return hierarchicalPipelines;
    }

    public List<HierarchicalPipeline> getHierarchicalPipelinesByPlant(long plantId) {
        List<HierarchicalPipeline> hierarchicalPipelines = new ArrayList<>();
        List<PipelineRun> pipelineRuns = pipelineRunAdmin.getActivePipelineRunsByPlantId(plantId).stream().filter(distinctByKey(PipelineRun::getName)).sorted(Comparator.comparing(PipelineRun::getName)).collect(Collectors.toList());

        for (PipelineRun pipelineRun : pipelineRuns) {
            HierarchicalPipeline parent = new HierarchicalPipeline();
            parent.setPipelineRunName(pipelineRun.getName());
            hierarchicalPipelines.add(parent);
            List<Pipeline> pipelines = getActivePipelinesByPlantAndRunName(plantId, pipelineRun.getName());
            for (Pipeline pipeline : pipelines) {
                HierarchicalPipeline hierarchicalPipeline = getHierarchicalPipeline(pipeline, parent);
                hierarchicalPipelines.add(hierarchicalPipeline);
            }
        }
        return hierarchicalPipelines;
    }

    private HierarchicalPipeline getHierarchicalPipeline(Pipeline pipeline, HierarchicalPipeline parent) {
        HierarchicalPipeline hierarchicalPipeline = new HierarchicalPipeline();
        hierarchicalPipeline.setFromLocation(pipeline.getFromLocation());
        hierarchicalPipeline.setToLocation(pipeline.getToLocation());
        hierarchicalPipeline.setLength(pipeline.getPipelineLength());
        hierarchicalPipeline.setDiameter(pipeline.getPipelineDiameter());
        hierarchicalPipeline.setSubstance(pipeline.getSubstance());
        hierarchicalPipeline.setStatus(pipeline.getStatus());
        hierarchicalPipeline.setFrequency(pipeline.getPiggingFrequency());
        PiggingEvent piggingEvent = piggingEventAdmin.getLastPiggingEventByPipeline(pipeline);
        if (piggingEvent != null)
            hierarchicalPipeline.setLastPigDate(piggingEvent.getDateSent());
        hierarchicalPipeline.setParent(parent);
        hierarchicalPipeline.setPipeline(pipeline);
        return hierarchicalPipeline;
    }
}
