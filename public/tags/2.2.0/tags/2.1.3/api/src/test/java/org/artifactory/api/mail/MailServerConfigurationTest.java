package org.artifactory.api.mail;

import org.artifactory.descriptor.mail.MailServerDescriptor;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * Tests the mail server configuration object
 *
 * @author Noam Y. Tenne
 */
@Test
public class MailServerConfigurationTest {

    /**
     * Tests the state of the configuration after constructing with a null MailServerDescriptor object
     */
    public void testNullMailServerDescriptorConstructor() {
        MailServerConfiguration mailServerConfiguration = new MailServerConfiguration(null);
        assertNull(mailServerConfiguration.getHost(), "Default state of host should be null.");
        assertEquals(mailServerConfiguration.getPort(), 0, "Default state of port should be 0.");
        assertNull(mailServerConfiguration.getUsername(), "Default state of username should be null.");
        assertNull(mailServerConfiguration.getPassword(), "Default state of password should be null.");
        assertNull(mailServerConfiguration.getFrom(), "Default state of from should be null.");
        assertNull(mailServerConfiguration.getSubjectPrefix(),
                "Default state of subject prefix should be null.");
        assertFalse(mailServerConfiguration.isUseSsl(), "Default state of SSL should be false.");
        assertFalse(mailServerConfiguration.isUseTls(), "Default state of TLS should be false.");
    }

    /**
     * Tests the state of the configuration after constructing with a default MailServerDescriptor object
     */
    public void testDefaultMailServerDescriptorConstructor() {
        MailServerConfiguration mailServerConfiguration = new MailServerConfiguration(new MailServerDescriptor());
        assertNull(mailServerConfiguration.getHost(), "Default state of descriptor host should be null.");
        assertEquals(mailServerConfiguration.getPort(), 25, "Default state of descriptor port should be 25.");
        assertNull(mailServerConfiguration.getUsername(), "Default state of descriptor username should be null.");
        assertNull(mailServerConfiguration.getPassword(), "Default state of descriptor password should be null.");
        assertNull(mailServerConfiguration.getFrom(), "Default state of descriptor from should be null.");
        assertEquals(mailServerConfiguration.getSubjectPrefix(), "[Artifactory]",
                "Default state of descriptor subject prefix should be [Artifactory].");
        assertFalse(mailServerConfiguration.isUseSsl(), "Default state of descriptor SSL should be false.");
        assertFalse(mailServerConfiguration.isUseTls(), "Default state of descriptor TLS should be false.");
    }

    /**
     * Tests the state of the configuration after constructing with a valid MailServerDescriptor object
     */
    public void testMailServerDescriptorConstructor() {
        String host = "momo";
        int port = 2131;
        String username = "popo";
        String password = "jojo";
        String from = "mitzi";
        String subjectPrefix = "gogo";
        boolean useSsl = true;
        boolean useTls = true;

        MailServerDescriptor descriptor = new MailServerDescriptor();
        descriptor.setHost(host);
        descriptor.setPort(port);
        descriptor.setUsername(username);
        descriptor.setPassword(password);
        descriptor.setFrom(from);
        descriptor.setSubjectPrefix(subjectPrefix);
        descriptor.setSsl(useSsl);
        descriptor.setTls(useTls);

        MailServerConfiguration config = new MailServerConfiguration(descriptor);

        assertEquals(config.getHost(), host, "Unexpected mail server host.");
        assertEquals(config.getPort(), port, "Unexpected mail server port.");
        assertEquals(config.getUsername(), username, "Unexpected mail server username.");
        assertEquals(config.getPassword(), password, "Unexpected mail server password.");
        assertEquals(config.getFrom(), from, "Unexpected mail server from.");
        assertEquals(config.getSubjectPrefix(), subjectPrefix, "Unexpected mail server subject prefix.");
        assertEquals(config.isUseSsl(), useSsl, "Unexpected mail server SSL state.");
        assertEquals(config.isUseTls(), useTls, "Unexpected mail server TLS state.");
    }

    /**
     * Tests the state of the configuration after constructing the the main constructor
     */
    public void testMainConstructor() {
        String host = "momo";
        int port = 2131;
        String username = "popo";
        String password = "jojo";
        String from = "mitzi";
        String subjectPrefix = "gogo";
        boolean useSsl = true;
        boolean useTls = true;

        MailServerConfiguration config = new MailServerConfiguration(host, port, username, password, from,
                subjectPrefix, useSsl, useTls);

        assertEquals(config.getHost(), host, "Unexpected mail server host.");
        assertEquals(config.getPort(), port, "Unexpected mail server port.");
        assertEquals(config.getUsername(), username, "Unexpected mail server username.");
        assertEquals(config.getPassword(), password, "Unexpected mail server password.");
        assertEquals(config.getFrom(), from, "Unexpected mail server from.");
        assertEquals(config.getSubjectPrefix(), subjectPrefix, "Unexpected mail server subject prefix.");
        assertEquals(config.isUseSsl(), useSsl, "Unexpected mail server SSL state.");
        assertEquals(config.isUseTls(), useTls, "Unexpected mail server TLS state.");
    }
}