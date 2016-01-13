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

package org.artifactory.api.mail;

import org.artifactory.util.EmailException;

import javax.mail.Message;

/**
 * The mail service's main interface
 *
 * @author Noam Tenne
 */
public interface MailService {
    void sendMail(String[] recipients, String subject, String body) throws EmailException;

    void sendMail(String[] recipients, String subject, String body, MailServerConfiguration config)
            throws EmailException;

    /**
     * Send an e-mail message
     *
     * @param recipients Recipients of the message that will be sent
     * @param subject    The subject of the message
     * @param body       The body of the message
     * @param config     A mail server configuration to use
     * @param recipientType {@link Message.RecipientType}
     *
     * @throws EmailException
     */
    void sendMail(String[] recipients, String subject, String body, MailServerConfiguration config,
            Message.RecipientType recipientType) throws EmailException;
}