/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.ui.rest.service.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.license.LicenseModuleModel;
import org.artifactory.util.CollectionUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * A helper class to filter unwanted modules and dependencies by the scopes.
 *
 * @author Tomer Cohen
 */
public abstract class ModuleFilterHelper {

    private ModuleFilterHelper() {
        // utility class
    }

    public static final Predicate<LicenseModuleModel> APPROVED_MODULE_LICENSE = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(LicenseModuleModel input) {
            return input.getLicense().isApproved() && input.getLicense().isValidLicense();
        }
    };
    public static final Predicate<LicenseModuleModel> NOT_APPROVED_MODULE_LICENSE = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(LicenseModuleModel input) {
            return !input.getLicense().isApproved() && input.getLicense().isValidLicense();
        }
    };

    public static final Predicate<LicenseModuleModel> UNKNOWN_MODULE_LICENSE = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(LicenseModuleModel input) {
            return input.getLicense().isUnknown();
        }
    };
    public static final Predicate<LicenseModuleModel> NOT_FOUND_MODULE_LICENSE = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(LicenseModuleModel input) {
            return input.getLicense().isNotFound();
        }
    };
    public static final Predicate<LicenseModuleModel> NEUTRAL_MODULE_MODEL = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(LicenseModuleModel input) {
            return input.isNeutral();
        }
    };

    /**
     * Filter a list of {@link Dependency} by a set of scopes.
     * NOTE: The filtering is done on the passed list so pass a copy if you need the original!
     */
    public static class RemoveDependenciesWithoutIndicatedScopes implements Predicate<Dependency> {
        private Set<String> scopes;

        public RemoveDependenciesWithoutIndicatedScopes(Set<String> scopes) {
            this.scopes = scopes;
        }

        @Override
        public boolean apply(@Nonnull Dependency input) {
            if (CollectionUtils.isNullOrEmpty(scopes)) {
                return false;
            } else if (CollectionUtils.isNullOrEmpty(input.getScopes())) {
                return true;
            }
            for (String scope : input.getScopes()) {
                if (scopes.contains(scope)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Filter out the dependencies {@link Dependency} which are actually artifacts that were published. i.e.
     * if artifact3 is depending upon artifact1, and artifact1 is a part of(created by) the build, then it is a
     * dependency on a published module and should be filtered out.
     * NOTE: The filtering is done on the passed list so pass a copy if you need the original!
     */
    public static class RemovePublishedDependencies implements Predicate<Dependency> {
        private Set<Artifact> moduleArtifacts = Sets.newHashSet();

        public RemovePublishedDependencies(Collection<Module> modules) {
            for (Module module : modules) {
                if (CollectionUtils.notNullOrEmpty(module.getArtifacts())) {
                    moduleArtifacts.addAll(module.getArtifacts());
                }
            }
        }

        @Override
        public boolean apply(@Nonnull Dependency input) {
            // filter published artifacts based on the checksum
            for (Artifact artifact : moduleArtifacts) {
                if (StringUtils.isNotBlank(artifact.getSha1()) && artifact.getSha1().equals(input.getSha1())) {
                    return true;
                } else if (StringUtils.isNotBlank(artifact.getMd5()) && artifact.getMd5().equals(input.getMd5())) {
                    return true;
                }
            }
            return false;
        }
    }


    /**
     * the property-based {@link org.artifactory.api.license.LicenseInfo} and then according to the extracted license
     */
    public static class LicenseModelComparator implements Comparator<LicenseModuleModel>, Serializable {
        @Override
        public int compare(LicenseModuleModel o1, LicenseModuleModel o2) {
            int result = o1.getLicense().getName().compareTo(o2.getLicense().getName());
            if (result == 0) {
                result = o1.getExtractedLicense().getName().compareTo(o2.getExtractedLicense().getName());
            }
            return result;
        }
    }

    /**
     * A predicate to filter out all selected rows in the licenses override table
     */
    public static final Predicate<LicenseModuleModel> SELECTED_LICENSES = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(@Nonnull LicenseModuleModel input) {
            return input.isSelected();
        }
    };

    /**
     * A predicate to filter out all overridable (selectable) licenses.
     */
    public static final Predicate<LicenseModuleModel> SELECTABLE_PREDICATE = new Predicate<LicenseModuleModel>() {
        @Override
        public boolean apply(@Nonnull LicenseModuleModel input) {
            return input.isOverridable();
        }
    };

}
