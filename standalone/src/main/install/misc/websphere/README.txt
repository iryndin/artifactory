Following the issues - RTFACT-345, RTFACT-638
=============================================
Due to the inconsistency of api implemenatations between different application servers, we must sometimes use different configurations.
In order for Artifactory to run properly under the IBM WebSphere application server, we must:
    - Extract the artifactory.war file.
    - Replace the standard web.xml file under "EXTRACTED_WAR_FOLDER/WEB-INF" with the WebSphere specific web.xml from "ARTIFACTORY_HOME/misc/websphere".
    - Repackage the war file and run normally.

The changes in the configuration are:
    - Assingning a WicketServlet as well as the the WicketFilter.
    - A boolean value which controls the usage of the methods getServletPath() or getServletInfo().