/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.security.jobs;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailServerConfiguration;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.security.UserInfo;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.util.HttpUtils;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import java.util.Map;

/**
 * A job that notifies users about expiring password
 *
 * @author Michael Pasternak
 */
@JobCommand(description = "Password expire notification job",
        singleton = true, runOnlyOnPrimary = false, schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.SYSTEM)
public class PasswordExpireNotificationJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(PasswordExpireNotificationJob.class);
    private final ArtifactoryContext artifactoryContext = ContextHelper.get();

    @Override
    public void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        log.debug("Password expire notification job is started");

        CentralConfigService configService = artifactoryContext.beanForType(CentralConfigService.class);
        if(!configService.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy().isEnabled()) {
            log.debug("ExpirationPolicy is disabled");
            return;
        }
        if(!configService.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy().isNotifyByEmail()) {
            log.debug("Email notification is disabled, not notifying users about expiring passwords ...");
            return;
        }

        MailService mailService = artifactoryContext.beanForType(MailService.class);
        SecurityService securityService = artifactoryContext.beanForType(SecurityService.class);

        MailServerConfiguration emailConfig = getEmailConfig();
        if(emailConfig == null || !emailConfig.isEnabled()) {
            log.debug("Mail server is not configured/disabled, not notifying users about expiring passwords ...");
            return;
        }

        Map<UserInfo, Long> usersWitchPasswordIsAboutToExpire = securityService.getUsersWitchPasswordIsAboutToExpire();
        usersWitchPasswordIsAboutToExpire.entrySet().parallelStream().forEach(entry -> {
            mailService.sendMail(
                    getEmailAddresses(entry.getKey().getEmail()),
                    getMailSabject(),
                    getMailBody(entry.getKey(), entry.getValue(), false),
                    emailConfig,
                    Message.RecipientType.TO
            );
        });

        log.debug("Password expire notification job is finished");
    }

    private String[] getEmailAddresses(String addresses) {
        return new String[]{addresses};
    }

    /**
     * @return mail server configuration {@link MailServerConfiguration}
     */
    private MailServerConfiguration getEmailConfig() {
        CentralConfigService centralConfigService = artifactoryContext.beanForType(CentralConfigService.class);
        MailServerDescriptor m = centralConfigService.getMutableDescriptor().getMailServer();

        if (m == null) {
            return null;
        }

        return new MailServerConfiguration(
                m.isEnabled(), m.getHost(), m.getPort(), m.getUsername(),
                CryptoHelper.decryptIfNeeded(m.getPassword()), m.getFrom(), m.getSubjectPrefix(),
                m.isTls(), m.isSsl(), m.getArtifactoryUrl()
        );
    }

    /**
     * @return email subject line
     */
    public String getMailSabject() {
        return "Your Artifactory password is about to expire";
    }

    /**
     * Produces unique (per user) email body
     *
     * @param user an addressee
     * @param passwordCreated the date when password was created
     * @param plainText whether email should be send in plain text or html
     *
     * @return email body
     */
    public String getMailBody(UserInfo user, Long passwordCreated, boolean plainText) {
        DateTime created = new DateTime(passwordCreated);
        CentralConfigService centralConfigService = ContextHelper.get().beanForType(CentralConfigService.class);
        int expiresIn = centralConfigService.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy().getExpiresIn();
        DateTime now = DateTime.now();
        int daysLeft = created.plusDays(expiresIn).minusDays(now.getDayOfYear()).getDayOfYear();
        if ((daysLeft == 365 || daysLeft == 366) && created.plusDays(expiresIn).dayOfYear().get() != daysLeft) daysLeft = 0;
        DateTime dueDate = now.plusDays(daysLeft);

        if(daysLeft == 0) {
            if(!plainText)
                return String.format(
                        "<!DOCTYPE html>" +
                                "<html>" +
                                "<body>" +
                                "<p>" +
                                "Dear %s,<br><br>" +
                                "Your Artifactory password is about to expire today (%s).<br>" +
                                "Please change your password before it expires, " +
                                "Once expired you will not be able to  access the<br>" +
                                "Artifactory UI or REST API using that password until it will be changed.<br><br>" +
                                "Password can be changed in <a href=\""+getProfilePageUrl()+"\">your profile page</a> " +
                                "or by your system administrator.<br><br>" +
                                "</p>" +
                                "</body>" +
                                "</html>",
                        user.getUsername().toUpperCase(),
                        dueDate.toString("MMMMM dd, YYYY")
                );
            return String.format(
                    "Dear %s,\n" +
                            "\n" +
                            "Your Artifactory password is about to expire today (%s).\n" +
                            "Please change your password before it expires. Once expired you will not be able to  access the\n" +
                            "Artifactory UI or REST API using that password until it will be changed.\n" +
                            "\n" +
                            "Password can be changed in your page [1] or by your system administrator.\n\n" +
                            "[1] " + getProfilePageUrl(),
                    user.getUsername().toUpperCase(),
                    dueDate.toString("MMMMM dd, YYYY")
            );
        } else {
            if(!plainText)
                return String.format(
                        "<!DOCTYPE html>" +
                                "<html>" +
                                "<body>" +
                                "<p>" +
                                "Dear %s,<br><br>" +
                                "Your Artifactory password is about to expire in %d " + (daysLeft == 1 ? "day" : "days") + " (%s).<br>" +
                                "Please change your password before it expires, " +
                                "Once expired you will not be able to  access the<br>" +
                                "Artifactory UI or REST API using that password until it will be changed.<br><br>" +
                                "Password can be changed in <a href=\""+getProfilePageUrl()+"\">your profile page</a> " +
                                "or by your system administrator.<br><br>" +
                                "</p>" +
                                "</body>" +
                                "</html>",
                        user.getUsername().toUpperCase(),
                        daysLeft,
                        dueDate.toString("MMMMM dd, YYYY")
                );
            return String.format(
                    "Dear %s,\n" +
                            "\n" +
                            "Your Artifactory password is about to expire in %d " + (daysLeft == 1 ? "day" : "days") + " (%s).\n" +
                            "Please change your password before it expires. Once expired you will not be able to  access the\n" +
                            "Artifactory UI or REST API using that password until it will be changed.\n" +
                            "\n" +
                            "Password can be changed in your page [1] or by your system administrator.\n\n" +
                            "[1] " + getProfilePageUrl(),
                    user.getUsername().toUpperCase(),
                    daysLeft,
                    dueDate.toString("MMMMM dd, YYYY")
            );
        }
    }

    /**
     * @return a URL referring artifactory instance profile page
     */
    private String getProfilePageUrl() {
        CoreAddons addon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(CoreAddons.class);
        String resetPageUrl = addon.getArtifactoryUrl();
        if (resetPageUrl != null &&  StringUtils.isNotBlank(resetPageUrl)) {
            if (!resetPageUrl.endsWith("/")) {
                resetPageUrl += "/";
            }
            if (addon.isAol()) {
                resetPageUrl += "#/profile";
            } else {
                resetPageUrl += HttpUtils.WEBAPP_URL_PATH_PREFIX + "/#/profile ";
            }
        }
        return resetPageUrl;
    }
}
