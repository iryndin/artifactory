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

package org.artifactory.descriptor.security.sshserver;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The SSH Server related settings
 *
 * @author Noam Y. Tenne
 */
@XmlType(name = "SshServerSettingsType",
        propOrder = {"enableSshServer", "sshServerPort"}, namespace = Descriptor.NS)
public class SshServerSettings implements Descriptor {

    @XmlElement(defaultValue = "false")
    private boolean enableSshServer = false;

    @XmlElement(defaultValue = "1339")
    private int sshServerPort = 1339;

    public boolean isEnableSshServer() {
        return enableSshServer;
    }

    public void setEnableSshServer(boolean enableSshServer) {
        this.enableSshServer = enableSshServer;
    }

    public int getSshServerPort() {
        return sshServerPort;
    }

    public void setSshServerPort(int sshServerPort) {
        this.sshServerPort = sshServerPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SshServerSettings that = (SshServerSettings) o;

        if (enableSshServer != that.enableSshServer) {
            return false;
        }
        return sshServerPort == that.sshServerPort;

    }

    @Override
    public int hashCode() {
        int result = (enableSshServer ? 1 : 0);
        result = 31 * result + sshServerPort;
        return result;
    }
}