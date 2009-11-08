package org.artifactory.maven;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.artifactory.api.maven.MavenSettings;
import org.artifactory.api.maven.MavenSettingsServer;
import org.artifactory.util.StringInputStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * @author Eli Givoni
 */
@Test
public class MavenServiceImplTest {

    public void generateMavenSettings() throws Exception {
        MavenServiceImpl mavenService = new MavenServiceImpl();
        MavenSettings mavenSettings = new MavenSettings("blabla");
        mavenSettings.addServer(new MavenSettingsServer("server1", "elig", "secret"));
        String result = mavenService.generateSettings(mavenSettings);
        assertTrue(result.contains("http://maven.apache.org/xsd/settings-1.0.0.xsd"),
                "Schema declaration not found:\n " + result);
        SettingsXpp3Reader reader = new SettingsXpp3Reader();
        Settings resultedSettings = reader.read(new StringInputStream(result));
        assertEquals(resultedSettings.getServers().size(), 1);
        Server resultedServer = (Server) resultedSettings.getServers().get(0);
        assertEquals(resultedServer.getId(), "server1");
    }

}
