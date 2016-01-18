package org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria;

import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.result.AqlUISearchDockerV1ResultManipulator;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.result.AqlUISearchDummyResultManipulator;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.result.AqlUISearchNpmResultManipulator;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.result.AqlUISearchResultManipulator;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains all available criteria specific to each package type the search supports
 *
 * @author Dan Feldman
 */
public enum PackageSearchCriteria {

    npmName(PackageSearchType.npm, "npm.name",
            new AqlUISearchModel("npmName", "Name", "Npm Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUINpmNameSearchStrategy("npm.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    npmVersion(PackageSearchType.npm, "npm.version",
            new AqlUISearchModel("npmVersion", "Version", "Npm Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("npm.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    npmScope(PackageSearchType.npm, "npm.name",
            new AqlUISearchModel("npmScope", "Scope", "Npm Scope",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUINpmScopeSearchStrategy("npm.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchNpmResultManipulator()),

    debianName(PackageSearchType.debian, "deb.name",
            new AqlUISearchModel("debianName", "Name", "Debian Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianVersion(PackageSearchType.debian, "deb.version",
            new AqlUISearchModel("debianVersion", "Version", "Debian Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianPriority(PackageSearchType.debian, "deb.priority",
            new AqlUISearchModel("debianPriority", "Priority", "Debian Priority",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.priority",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianMaintainer(PackageSearchType.debian, "deb.maintainer",
            new AqlUISearchModel("debianMaintainer", "Maintainer", "Debian Maintainer",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.maintainer",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianDistribution(PackageSearchType.debian, "deb.distribution",
            new AqlUISearchModel("debianDistribution", "Distribution", "Debian Distribution",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.distribution",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianComponent(PackageSearchType.debian, "deb.component",
            new AqlUISearchModel("debianComponent", "Component", "Debian Component",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.component",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    debianArchitecture(PackageSearchType.debian, "deb.architecture",
            new AqlUISearchModel("debianArchitecture", "Architecture", "Debian Architecture",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("deb.architecture",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgName(PackageSearchType.opkg, "opkg.name",
            new AqlUISearchModel("opkgName", "Name", "Opkg Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgVersion(PackageSearchType.opkg, "opkg.version",
            new AqlUISearchModel("opkgVersion", "Version", "Opkg Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgArchitecture(PackageSearchType.opkg, "opkg.architecture",
            new AqlUISearchModel("opkgArchitecture", "Architecture", "Opkg Architecture",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.architecture",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgPriority(PackageSearchType.opkg, "opkg.priority",
            new AqlUISearchModel("opkgPriority", "Priority", "Opkg Priority",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.priority",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    opkgMaintainer(PackageSearchType.opkg, "opkg.maintainer",
            new AqlUISearchModel("opkgMaintainer", "Maintainer", "Opkg Maintainer",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("opkg.maintainer",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV1Image(PackageSearchType.dockerV1, "path",
            new AqlUISearchModel("dockerV1Image", "Image", "Docker V1 Image",
                    new AqlComparatorEnum[]{AqlComparatorEnum.matches}),
            new AqlUIDockerV1ImageSearchStrategy(AqlFieldEnum.itemPath,
                    new AqlDomainEnum[]{AqlDomainEnum.items}),
            new AqlUISearchDockerV1ResultManipulator()),

    dockerV1Tag(PackageSearchType.dockerV1, "docker.tag.name",
            new AqlUISearchModel("dockerV1Tag", "Tag", "Docker V1 Tag",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("docker.tag.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV2Image(PackageSearchType.dockerV2, "docker.repoName",
            new AqlUISearchModel("dockerV2Image", "Image", "Docker V2 Image",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIDockerV2ImageSearchStrategy("docker.repoName",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV2Tag(PackageSearchType.dockerV2, "docker.manifest",
            new AqlUISearchModel("dockerV2Tag", "Tag", "Docker V2 Tag",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("docker.manifest",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    dockerV2ImageDigest(PackageSearchType.dockerV2, "sha256",
            new AqlUISearchModel("dockerV2ImageDigest", "Image Digest", "Docker V2 Image Digest",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIDockerV2ImageDigestSearchStrategy("sha256",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    nugetPackageId(PackageSearchType.nuget, "nuget.id",
            new AqlUISearchModel("nugetPackageId", "ID", "NuGet Package ID",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.id",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    nugetVersion(PackageSearchType.nuget, "nuget.version",
            new AqlUISearchModel("nugetVersion", "Version", "NuGet Package Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

/*    nugetTags(PackageSearchType.nuget, "nuget.tags",
            new AqlUISearchModel("nugetTags", "Tags", "NuGet Tags",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.tags",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    nugetDigest(PackageSearchType.nuget, "nuget.digest",
            new AqlUISearchModel("nugetDigest", "Digest", "NuGet Digest",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("nuget.digest",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),*/

    bowerName(PackageSearchType.bower, "bower.name",
            new AqlUISearchModel("bowerName", "Name", "Bower Package Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("bower.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    bowerVersion(PackageSearchType.bower, "bower.version",
            new AqlUISearchModel("bowerVersion", "Version", "Bower Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("bower.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    pypiName(PackageSearchType.pypi, "pypi.name",
            new AqlUISearchModel("pypiName", "Name", "PyPi Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("pypi.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    pypiVersion(PackageSearchType.pypi, "pypi.version",
            new AqlUISearchModel("pypiVersion", "Version", "Pypi Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("pypi.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    rpmName(PackageSearchType.rpm, "rpm.metadata.name",
            new AqlUISearchModel("rpmName", "Name", "RPM Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    rpmVersion(PackageSearchType.rpm, "rpm.metadata.version",
            new AqlUISearchModel("rpmVersion", "Version", "RPM Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    rpmArchitecture(PackageSearchType.rpm, "rpm.metadata.arch",
            new AqlUISearchModel("rpmArchitecture", "Architecture", "RPM Architecture",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.arch",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

   /* rpmRelease(PackageSearchType.rpm, "rpm.metadata.release",
            new AqlUISearchModel("rpmRelease", "Release", "RPM Release",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("rpm.metadata.release",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),*/

    gemName(PackageSearchType.gems, "gem.name",
            new AqlUISearchModel("gemName", "Name", "Gem Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("gem.name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    gemVersion(PackageSearchType.gems, "gem.version",
            new AqlUISearchModel("gemVersion", "Version", "Gem Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("gem.version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    vagrantName(PackageSearchType.vagrant, "box_name",
            new AqlUISearchModel("vagrantName", "Box Name", "Vagrant Box Name",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("box_name",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    vagrantVersion(PackageSearchType.vagrant, "box_version",
            new AqlUISearchModel("vagrantVersion", "Box Version", "Vagrant Box Version",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("box_version",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator()),

    vagrantProvider(PackageSearchType.vagrant, "box_provider",
            new AqlUISearchModel("vagrantProvider", "Box Provider", "Vagrant Box Provider",
                    new AqlComparatorEnum[]{AqlComparatorEnum.equals, AqlComparatorEnum.matches}),
            new AqlUIPropertySearchStrategy("box_provider",
                    new AqlDomainEnum[]{AqlDomainEnum.items, AqlDomainEnum.properties}),
            new AqlUISearchDummyResultManipulator());

    PackageSearchType type;
    String aqlName;
    AqlUISearchModel model;
    AqlUISearchStrategy strategy;
    AqlUISearchResultManipulator resultManipulator;

    PackageSearchCriteria(PackageSearchType type, String aqlName, AqlUISearchModel model,
            AqlUISearchStrategy strategy, AqlUISearchResultManipulator resultManipulator) {
        this.type = type;
        this.aqlName = aqlName;
        this.model = model;
        this.strategy = strategy;
        this.resultManipulator = resultManipulator;
    }

    public PackageSearchType getType() {
        return type;
    }

    public AqlUISearchModel getModel() {
        return model;
    }

    public AqlUISearchStrategy getStrategy() {
        return strategy;
    }

    public static AqlUISearchStrategy getStrategyByFieldId(String id) {
        return valueOf(id).strategy;
    }

    public AqlUISearchResultManipulator getResultManipulator() {
        return resultManipulator;
    }

    /**
     * Returns the criteria that matches the AQL field name or the property key that {@param aqlName} references
     */
    public static PackageSearchCriteria getCriteriaByAqlFieldOrPropName(String aqlName) {
        return Stream.of(values())
                .filter(value -> value.aqlName.equalsIgnoreCase(aqlName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported field or property '" + aqlName + "'."));
    }

    public static List<PackageSearchCriteria> getCriteriaByPackage(String packageType) {
        return Stream.of(values())
                .filter(searchCriterion -> searchCriterion.type.equals(PackageSearchType.getById(packageType)))
                .collect(Collectors.toList());
    }

    public static List<PackageSearchCriteria> getCriteriaByPackage(PackageSearchType packageType) {
        return Stream.of(values())
                .filter(searchCriterion -> searchCriterion.type.equals(packageType))
                .collect(Collectors.toList());
    }

    /**
     * Returns the {@link AqlUISearchResultManipulator} the AQL field name or the property key that {@param aqlName}
     * references
     */
    public static AqlUISearchResultManipulator getResultManipulatorByAqlFieldOrPropName(String aqlName) {
        return Stream.of(values())
                .filter(value -> value.aqlName.equalsIgnoreCase(aqlName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported field or property '" + aqlName + "'."))
                .getResultManipulator();
    }

    public static List<AqlUISearchResultManipulator> getResultManipulatorsByPackage(PackageSearchType packageType) {
        return getCriteriaByPackage(packageType)
                .stream()
                .map(PackageSearchCriteria::getResultManipulator)
                .collect(Collectors.toList());
    }

    public static PackageSearchType getPackageTypeByFieldId(String fieldId) {
        try {
            return valueOf(fieldId).getType();
        } catch (IllegalArgumentException iae) {
            //no such fieldId
        }
        return null;
    }

    public static List<AqlUISearchStrategy> getStartegiesByPackageSearchType(PackageSearchType type) {
        return getCriteriaByPackage(type).stream()
                .map(PackageSearchCriteria::getStrategy)
                .collect(Collectors.toList());
    }

    public enum PackageSearchType {
        dockerV1(RepoType.Docker, true, "docker"), dockerV2(RepoType.Docker, true, "docker"),
        nuget(RepoType.NuGet, true, "nuget"), npm(RepoType.Npm, true, "npm"), bower(RepoType.Bower, true, "bower"),
        gems(RepoType.Gems, false, "ruby-gems"), rpm(RepoType.YUM, true, "yum"), debian(RepoType.Debian, false, "deb"),
        opkg(RepoType.Opkg, false, "opkg"), pypi(RepoType.Pypi, false, "pypi"), gavc(RepoType.Maven, true, "pom"),
        vagrant(RepoType.Vagrant, false, "vagrant");
        /*, all(""),*/ /*gitlfs(RepoType.GitLfs, false, "git-lfs"),*/

        boolean remoteCachesProps;
        RepoType repoType;
        String icon;

        PackageSearchType(RepoType repoType, boolean remoteCachesProps, String icon) {
            this.repoType = repoType;
            this.remoteCachesProps = remoteCachesProps;
            this.icon = icon;
        }

        public static PackageSearchType getById(String id) {
            for (PackageSearchType type : values()) {
                if (type.name().equalsIgnoreCase(id)) {
                    return type;
                }
            }
            throw new UnsupportedOperationException("Unsupported package " + id);
        }

        public String getDisplayName() {
            if (this.equals(dockerV1)) {
                return "Docker V1";
            } else if (this.equals(dockerV2)) {
                return "Docker V2";
            } else if (this.equals(rpm)) {
                return "RPM";
            } else if (this.equals(gavc)) {
                return "GAVC";
            } else if (this.equals(pypi)) {
                return "PyPI";
            }
            return repoType.name();
        }

        public boolean isRemoteCachesProps() {
            return remoteCachesProps;
        }

        public String getId() {
            return this.name();
        }

        public RepoType getRepoType() {
            return repoType;
        }

        public String getIcon() {
            return icon;
        }
    }
}
