package org.artifactory.storage.db.aql.service;

import com.google.common.collect.Lists;
import org.artifactory.storage.db.aql.service.optimizer.FileTypeOptimization;
import org.artifactory.storage.db.aql.service.optimizer.OptimizationStrategy;
import org.artifactory.storage.db.aql.service.optimizer.PropertyCriteriaRelatedWithOr;
import org.artifactory.storage.db.aql.sql.builder.query.aql.AqlQuery;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class AqlQueryOptimizer {
    List<OptimizationStrategy> strategies;

    public AqlQueryOptimizer() {
        strategies = Lists.newArrayList();
        strategies.add(new FileTypeOptimization());
        strategies.add(new PropertyCriteriaRelatedWithOr());
    }

    public void optimize(AqlQuery aqlQuery) {
        for (OptimizationStrategy strategy : strategies) {
            strategy.doJob(aqlQuery);
        }
    }
}
