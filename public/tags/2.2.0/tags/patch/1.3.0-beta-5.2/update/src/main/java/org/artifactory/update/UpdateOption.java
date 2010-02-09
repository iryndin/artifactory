package org.artifactory.update;

import org.artifactory.cli.Option;
import org.artifactory.cli.OptionInfo;

public enum UpdateOption implements Option {
    help("displays this usage message"),
    home("mandatory - the home folder of the old Artifactory", true, "old Artifactory home")/*,
    backup("a full backup folder done with an old version of Artifactory," +
            " set the norepo, convert, config, security flag if none set", true,
            "old backup folder")*/,
    dest("the destination folder for the new export files. Default value: tmpExport", true,
            "destination folder"),
    version("the actual version of the old Artifactory if the Update Manager cannot find it",
            true, "version name"),
    repo("export only a specified list of repositories. Default value: " +
            ArtifactoryUpdate.EXPORT_ALL_NON_CACHED_REPOS, true,
            "repo names separated by ':'"),
    norepo("does not export the repositories, just convert config and security"),
    convert("activate the Local and Virtual repository names conversion"),
    noconvert("does not activate the Local and Virtual repository names conversion during a " +
            "full export"),
    //config("convert the Artifactory config XML file, and set the norepo flag"),
    security("only export the security file from DB, and set the norepo flag"),
    caches("include cached repositories in the export (by default caches are not exported). " +
            "If repo option is passed this option will be ignored");

    private final Option option;

    UpdateOption(String description) {
        this.option = new OptionInfo(name(), description);
    }

    UpdateOption(String description, boolean needExtraParam, String paramDescription) {
        this.option = new OptionInfo(name(), description, needExtraParam, paramDescription);
    }

    public String getDescription() {
        return option.getDescription();
    }

    public boolean isNeedExtraParam() {
        return option.isNeedExtraParam();
    }

    public String getParamDescription() {
        return option.getParamDescription();
    }

    public void setValue(String value) {
        option.setValue(value);
    }

    public String getValue() {
        return option.getValue();
    }

    public boolean isSet() {
        return option.isSet();
    }

    public String argValue() {
        return option.argValue();
    }

    public String getName() {
        return option.getName();
    }

    public void set() {
        option.set();
    }
}
