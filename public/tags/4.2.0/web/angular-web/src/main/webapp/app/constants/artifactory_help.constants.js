export default {
    "/**": [
        {
            title: "User Guide",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+User+Guide",
            priority: 0
        },
        {
            title: "REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API",
            priority: 0
        }
    ],

    "/home": [
        {
            title: "General Information",
            link: "https://www.jfrog.com/confluence/display/RTF/General+Information",
            priority: 1
        }
    ],
    "/profile": [
        {
            title: "Updating Your Profile",
            link: "https://www.jfrog.com/confluence/display/RTF/Updating+Your+Profile",
            priority: 1
        }
    ],
    "/forgot-password": [
        {
            title: "Forgot Password",
            link: "https://www.jfrog.com/confluence/display/RTF/Updating+Your+Profile#UpdatingYourProfile-ResettingYourPassword",
            priority: 1
        }
    ],

    "/artifacts/browse/tree/**": [
        {
            title: "Browsing Artifacts",
            link: "https://www.jfrog.com/confluence/display/RTF/Browsing+Artifactory#BrowsingArtifactory-TreeBrowsing",
            priority: 1
        },
    ],
    "/artifacts/browse/simple/**": [
        {
            title: "Browsing Artifacts",
            link: "https://www.jfrog.com/confluence/display/RTF/Browsing+Artifactory#BrowsingArtifactory-SimpleBrowsing",
            priority: 1
        },
    ],
    "/artifacts/browse/**": [
        {
            title: "Deploying Artifacts",
            link: "https://www.jfrog.com/confluence/display/RTF/Deploying+Artifacts",
            priority: 2
        },
        {
            title: "Set Me Up",
            link: "https://www.jfrog.com/confluence/display/RTF/Using+Artifactory#UsingArtifactory-SetMeUp",
            priority: 5
        },
        {
            title: "Deploy Artifact with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-DeployArtifact",
            priority: 200
        }
    ],


    "/artifacts/browse/tree/search/**": [
        {
            title: "Searching Artifacts",
            link: "https://www.jfrog.com/confluence/display/RTF/Searching+Artifacts",
            priority: 3
        },
        {
            title: "Stash Search Results",
            link: "https://www.jfrog.com/confluence/display/RTF/Searching+for+Artifacts#SearchingforArtifacts-SavingSearchResultsintheStash",
            priority: 4
        },
        {
            title: "Artifactory Query Language",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ArtifactoryQueryLanguage%28AQL%29",
            priority: 200
        }
    ],

    // Every search type needs a duplicated entry, one under the tree search and one for simple

    "/artifacts/browse/tree/search/quick/**": [
        {
            title: "Execute Quick Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ArtifactSearch%28QuickSearch%29",
            priority: 200
        }
    ],
    "/artifacts/browse/simple/search/quick/**": [
        {
            title: "Execute Quick Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ArtifactSearch%28QuickSearch%29",
            priority: 200
        }
    ],
    "/artifacts/browse/tree/search/class/**": [
        {
            title: "Execute Archive Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ArchiveEntrySearch%28ClassSearch%29",
            priority: 200
        }
    ],
    "/artifacts/browse/simple/search/class/**": [
        {
            title: "Execute Archive Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ArchiveEntrySearch%28ClassSearch%29",
            priority: 200
        }
    ],
    "/artifacts/browse/tree/search/gavc/**": [
        {
            title: "Execute GAVC Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-GAVCSearch",
            priority: 200
        }
    ],
    "/artifacts/browse/simple/search/gavc/**": [
        {
            title: "Execute GAVC Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-GAVCSearch",
            priority: 200
        }
    ],
    "/artifacts/browse/tree/search/property/**": [
        {
            title: "Execute Property Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-PropertySearch",
            priority: 200
        }
    ],
    "/artifacts/browse/simple/search/property/**": [
        {
            title: "Execute Property Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-PropertySearch",
            priority: 200
        }
    ],
    "/artifacts/browse/tree/search/checksum/**": [
        {
            title: "Execute Checksum Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ChecksumSearch",
            priority: 200
        }
    ],
    "/artifacts/browse/simple/search/checksum/**": [
        {
            title: "Execute Checksum Search with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ChecksumSearch",
            priority: 200
        }
    ],
    //"/artifacts/browse/tree/search/remote/**": [
    //    {
    //        title: "",
    //        link: "",
    //        priority: 200
    //    }
    //],
    //"/artifacts/browse/simple/search/remote/**": [
    //    {
    //        title: "",
    //        link: "",
    //        priority: 200
    //    }
    //],

    "/builds/**": [
        {
            title: "Build Integration",
            link: "https://www.jfrog.com/confluence/display/RTF/Build+Integration",
            priority: 1
        },
        {
            title: "Upload Build with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-BuildUpload",
            priority: 200
        }
    ],


    "/admin/repositories/**": [
        {
            title: "Configuring Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Configuring+Repositories",
            priority: 100
        },
        {
            title: "Single Package Type Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Upgrading+Artifactory#UpgradingArtifactory-SinglePackageTypeRepositories",
            priority: 101
        },
        {
            title: "Create Repository with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateorReplaceRepositoryConfiguration",
            priority: 200
        }
    ],

    "/admin/repository/**": [
        {
            title: "Single Package Type Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Upgrading+Artifactory#UpgradingArtifactory-SinglePackageTypeRepositories",
            priority: 101
        },
        {
            title: "Create Repository with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateorReplaceRepositoryConfiguration",
            priority: 200
        }
    ],

    "/admin/repositories/local": [
        {
            title: "Local Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Local+Repositories",
            priority: 1
        }
    ],
    "/admin/repository/local/**": [
        {
            title: "Local Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Local+Repositories",
            priority: 1
        },
        {
            title: "Repository Replication",
            link: "https://www.jfrog.com/confluence/display/RTF/Repository+Replication",
            priority: 2
        }
    ],

    "/admin/repositories/remote": [
        {
            title: "Remote Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Remote+Repositories",
            priority: 1
        }
    ],
    "/admin/repository/remote/**": [
        {
            title: "Remote Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Remote+Repositories",
            priority: 1
        },
        {
            title: "Pull Replication",
            link: "https://www.jfrog.com/confluence/display/RTF/Repository+Replication#RepositoryReplication-PullReplication",
            priority: 2
        }
    ],

    "/admin/repositories/virtual": [
        {
            title: "Virtual Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Virtual+Repositories",
            priority: 1
        }
    ],
    "/admin/repository/virtual/**": [
        {
            title: "Virtual Repositories",
            link: "https://www.jfrog.com/confluence/display/RTF/Virtual+Repositories",
            priority: 1
        }
    ],


    "/admin/repo_layouts**": [
        {
            title: "Repository Layouts",
            link: "https://www.jfrog.com/confluence/display/RTF/Repository+Layouts",
            priority: 1
        }
    ],

    "/admin/configuration/licenses": [
        {
            title: "License Control",
            link: "https://www.jfrog.com/confluence/display/RTF/License+Control",
            priority: 1
        }
    ],
    "/admin/configuration/black_duck": [
        {
            title: "Black Duck Integration",
            link: "https://www.jfrog.com/confluence/display/RTF/Black+Duck+Code+Center+Integration",
            priority: 1
        }
    ],
    "/admin/configuration/property_sets**": [
        {
            title: "Property Sets",
            link: "https://www.jfrog.com/confluence/display/RTF/Properties",
            priority: 1
        }
    ],
    "/admin/configuration/proxies**": [
        {
            title: "Managing Proxies",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Proxies",
            priority: 1
        }
    ],
    "/admin/configuration/mail": [
        {
            title: "Mail Configuration",
            link: "https://www.jfrog.com/confluence/display/RTF/Mail+Server+Configuration",
            priority: 1
        }
    ],
    "/admin/configuration/ha": [
        {
            title: "High Availability",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+High+Availability",
            priority: 1
        }
    ],
    "/admin/configuration/register_pro": [
        {
            title: "Activating License",
            link: "https://www.jfrog.com/confluence/display/RTF/Activating+Artifactory+Pro",
            priority: 1
        }
    ],

    "/admin/security/users**": [
        {
            title: "User Management",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Users",
            priority: 1
        },
        {
            title: "Create User with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateorReplaceUser",
            priority: 200
        }
    ],
    "/admin/security/groups**": [
        {
            title: "Group Management",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Users#ManagingUsers-CreatingandEditingGroups",
            priority: 1
        },
        {
            title: "Create Group with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateorReplaceGroup",
            priority: 200
        }
    ],
    "/admin/security/permissions**": [
        {
            title: "Permission Management",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Permissions",
            priority: 1
        },
        {
            title: "Create Permission with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateorReplacePermissionTarget",
            priority: 200
        }
    ],


    // Remove after fixing the url of the new permission form

    "/admin/security/permission/**": [
        {
            title: "Permission Management",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Permissions",
            priority: 1
        },
        {
            title: "Create Permission with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateorReplacePermissionTarget",
            priority: 200
        }
    ],



    "/admin/security/ldap_settings": [
        {
            title: "LDAP Settings",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Security+with+LDAP",
            priority: 1
        },
        {
            title: "LDAP Groups",
            link: "https://www.jfrog.com/confluence/display/RTF/LDAP+Groups",
            priority: 2
        }
    ],
    "/admin/security/ldap_settings/**": [
        {
            title: "LDAP Settings",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Security+with+LDAP",
            priority: 1
        }
    ],
    "/admin/security/ldap_groups/**": [
        {
            title: "LDAP Groups",
            link: "https://www.jfrog.com/confluence/display/RTF/LDAP+Groups",
            priority: 1
        }
    ],
    "/admin/security/crowd_integration": [
        {
            title: "Crowd Integration",
            link: "https://www.jfrog.com/confluence/display/RTF/Atlassian+Crowd+Integration",
            priority: 1
        }
    ],
    "/admin/security/saml_integration": [
        {
            title: "SAML Integration",
            link: "https://www.jfrog.com/confluence/display/RTF/SAML+SSO+Integration",
            priority: 1
        }
    ],
    "/admin/security/http_sso": [
        {
            title: "HTTP SSO",
            link: "https://www.jfrog.com/confluence/display/RTF/Single+Sign-on",
            priority: 1
        }
    ],
    "/admin/security/signing_keys": [
        {
            title: "Signing Keys",
            link: "https://www.jfrog.com/confluence/display/RTF/Master+Key+Encryption",
            priority: 1
        },
        {
            title: "Signing Debian Packages",
            link: "https://www.jfrog.com/confluence/display/RTF/Debian+Repositories#DebianRepositories-SigningDebianPackages",
            priority: 2
        }
    ],

    "/admin/services/backups**": [
        {
            title: "Backup Management",
            link: "https://www.jfrog.com/confluence/display/RTF/Managing+Backups",
            priority: 1
        }
    ],
    "/admin/services/indexer": [
        {
            title: "Maven Indexer",
            link: "https://www.jfrog.com/confluence/display/RTF/Exposing+Maven+Indexes",
            priority: 1
        }
    ],

    "/admin/import_export**": [
        {
            title: "Importing & Exporting",
            link: "https://www.jfrog.com/confluence/display/RTF/Importing+and+Exporting",
            priority: 1
        }
    ],
    "/admin/import_export/repositories": [
        {
            title: "Repository Import with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ImportRepositoryContent",
            priority: 200
        }
    ],
    "/admin/import_export/system": [
        {
            title: "System Import with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-FullSystemImport",
            priority: 200
        },
        {
            title: "System Export with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-ExportSystem",
            priority: 200
        }
    ],

    "/admin/advanced/system_info": [
        {
            title: "System Info",
            link: "https://www.jfrog.com/confluence/display/RTF/System+Information",
            priority: 1
        },
        {
            title: "Get Sysmtem Info with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-SystemInfo",
            priority: 200
        }
    ],
    "/admin/advanced/system_logs": [
        {
            title: "Artifactory Logs",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+Log+Files",
            priority: 1
        }
    ],
    "/admin/advanced/maintenance": [
        {
            title: "Maintenance",
            link: "https://www.jfrog.com/confluence/display/RTF/Regular+Maintenance+Operations",
            priority: 1
        }
    ],
    "/admin/advanced/storage_summary": [
        {
            title: "Monitoring Storage",
            link: "https://www.jfrog.com/confluence/display/RTF/Monitoring+Storage",
            priority: 1
        }
    ],
    "/admin/advanced/config_descriptor": [
        {
            title: "Configuration Files",
            link: "https://www.jfrog.com/confluence/display/RTF/Configuration+Files",
            priority: 1
        },
        {
            title: "Get Config Descriptor with REST API",
            link: "https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-GeneralConfiguration",
            priority: 200
        }
    ],
    "/admin/advanced/security_descriptor": [
        {
            title: "Security Configuration",
            link: "https://www.jfrog.com/confluence/display/RTF/Configuring+Security",
            priority: 1
        }
    ]
}