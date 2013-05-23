/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.build;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.common.StatusEntry;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.DoesNotExistException;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class BuildPromotionHelper extends BaseBuildPromoter {
    private static final Logger log = LoggerFactory.getLogger(BuildPromotionHelper.class);

    public PromotionResult promoteBuild(BuildRun buildRun, Promotion promotion) {
        Build build = getBuild(buildRun);

        if (build == null) {
            throw new DoesNotExistException("Unable to find build '" + buildRun.getName() + "' #" +
                    buildRun.getNumber() + ".");
        }

        PromotionResult promotionResult = new PromotionResult();

        String targetRepo = promotion.getTargetRepo();

        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();

        if (StringUtils.isBlank(targetRepo)) {
            multiStatusHolder.setStatus("Skipping build item relocation: no target repository selected.", log);
        } else {
            assertRepoExists(targetRepo);
            Set<RepoPath> itemsToMove = collectItems(build, promotion.isArtifacts(), promotion.isDependencies(),
                    promotion.getScopes(), promotion.isFailFast(), true, multiStatusHolder);

            if (!itemsToMove.isEmpty()) {
                promoteBuildItems(promotion, multiStatusHolder, itemsToMove);
            }
        }

        if ((!multiStatusHolder.hasWarnings() && !multiStatusHolder.hasErrors()) || !promotion.isFailFast()) {
            Properties properties = (Properties) InfoFactoryHolder.get().createProperties();
            Map<String, Collection<String>> promotionProperties = promotion.getProperties();
            if ((promotionProperties != null) && !promotionProperties.isEmpty()) {
                for (Map.Entry<String, Collection<String>> entry : promotionProperties.entrySet()) {
                    properties.putAll(entry.getKey(), entry.getValue());
                }
            }
            if (!properties.isEmpty()) {
                //Rescan after action might have been taken
                Set<RepoPath> itemsToTag = collectItems(build, promotion.isArtifacts(),
                        promotion.isDependencies(), promotion.getScopes(), promotion.isFailFast(), true,
                        multiStatusHolder);

                if (!itemsToTag.isEmpty()) {
                    tagBuildItemsWithProperties(itemsToTag, properties, promotion.isFailFast(), promotion.isDryRun(),
                            multiStatusHolder);
                }
            }
        }

        performPromotionIfNeeded(multiStatusHolder, build, promotion);

        appendMessages(promotionResult, multiStatusHolder);
        return promotionResult;
    }

    private void performPromotionIfNeeded(MultiStatusHolder statusHolder, Build build, Promotion promotion) {
        String status = promotion.getStatus();

        if (statusHolder.hasErrors() || statusHolder.hasWarnings()) {
            statusHolder.setStatus("Skipping promotion status update: item promotion was completed with errors " +
                    "and warnings.", log);
            return;
        }

        if (StringUtils.isBlank(status)) {
            statusHolder.setStatus("Skipping promotion status update: no status received.", log);
            return;
        }

        PromotionStatusBuilder statusBuilder = new PromotionStatusBuilder(status).
                user(authorizationService.currentUsername()).repository(promotion.getTargetRepo()).
                comment(promotion.getComment()).ciUser(promotion.getCiUser());

        String timestamp = promotion.getTimestamp();

        if (StringUtils.isNotBlank(timestamp)) {
            try {
                ISODateTimeFormat.dateTime().parseMillis(timestamp);
            } catch (Exception e) {
                statusHolder.setError("Skipping promotion status update: invalid\\unparsable timestamp " + timestamp +
                        ".", log);
                return;
            }
            statusBuilder.timestamp(timestamp);
        } else {
            statusBuilder.timestampDate(new Date());
        }

        if (promotion.isDryRun()) {
            statusHolder.setStatus("Skipping promotion status update: running in dry run mode.", log);
            return;
        }

        buildService.addPromotionStatus(build, statusBuilder.build());
    }

    private void promoteBuildItems(Promotion promotion, MultiStatusHolder status, Set<RepoPath> itemsToMove) {
        String targetRepo = promotion.getTargetRepo();
        boolean dryRun = promotion.isDryRun();
        if (promotion.isCopy()) {
            try {
                status.merge(copy(itemsToMove, targetRepo, dryRun, promotion.isFailFast()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                status.setError("Error occurred while copying: " + e.getMessage(), e, log);
            }
        } else {
            try {
                status.merge(move(itemsToMove, targetRepo, dryRun, promotion.isFailFast()));
            } catch (Exception e) {
                status.setError("Error occurred while moving: " + e.getMessage(), e, log);
            }
        }
    }

    private void appendMessages(PromotionResult promotionResult, MultiStatusHolder multiStatusHolder) {
        for (StatusEntry statusEntry : multiStatusHolder.getAllEntries()) {
            promotionResult.messages.add(new PromotionResult.PromotionResultMessages(statusEntry));
        }
    }
}
