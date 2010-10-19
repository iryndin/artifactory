/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.message;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.message.ArtifactoryUpdatesService;
import org.artifactory.api.message.Message;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static org.apache.commons.httpclient.params.HttpMethodParams.RETRY_HANDLER;

/**
 * @author Yoav Aharoni
 */
@Service
public class ArtifactoryUpdatesServiceImpl implements ArtifactoryUpdatesService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryUpdatesServiceImpl.class);
    private static final String MESSAGE_CACHE_KEY = "message";
    private static final Message ERROR_MESSAGE = new Message("na", "");

    @Autowired
    private CacheService cacheService;

    @Autowired
    private TaskService taskService;

    public void fetchMessage() {
        final Message message = getRemoteMessage();
        Cache<String, Message> cache = cacheService.getCache(ArtifactoryCache.artifactoryUpdates);
        cache.put(MESSAGE_CACHE_KEY, message);
    }

    public Message getCachedMessage() {
        Cache<String, Message> cache = cacheService.getCache(ArtifactoryCache.artifactoryUpdates);
        return cache.get(MESSAGE_CACHE_KEY);
    }

    public Message getMessage() {
        Message message = getCachedMessage();
        if (message == null) {
            fetchMessageAsync();
        }
        return message;
    }

    private Message getRemoteMessage() {
        final String url = ConstantValues.artifactoryUpdatesUrl.getString();
        try {
            GetMethod getMethod = new GetMethod(url);
            HttpClient client = createHTTPClient();

            client.executeMethod(getMethod);
            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                final String body = messageBody(getMethod);
                final String id = messageId(body);
                return new Message(id, body);
            }
            log.debug(String.format("Tried fetching message from '%s' and got status %s", url,
                    getMethod.getStatusCode()));
        } catch (IOException e) {
            log.debug(String.format("Exception while fetching message from '%s' ", url), e);
        }
        return ERROR_MESSAGE;
    }

    /**
     * Generate unique message id.
     *
     * @param body message body
     * @return unique message id
     */
    private String messageId(String body) {
        return new String(Base64.encodeBase64(DigestUtils.md5(body)))
                .replaceAll("=", "").replaceAll("\\+", "-").replaceAll("/", "_");
    }

    private String messageBody(GetMethod getMethod) throws IOException {
        final String body = getMethod.getResponseBodyAsString();
        return StringUtils.defaultString(body);
    }

    private HttpClient createHTTPClient() {
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setSoTimeout(5000);
        clientParams.setConnectionManagerTimeout(2000);
        clientParams.setParameter(RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));

        // proxy settings
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        HttpClientUtils.configureProxy(client, proxy);
        return client;
    }

    private synchronized void fetchMessageAsync() {
        if (getCachedMessage() == null && !taskService.hasTaskOfType(FetchArtifactoryUpdatesJob.class)) {
            QuartzTask fetchCommand = new QuartzTask(FetchArtifactoryUpdatesJob.class, 0);
            fetchCommand.setSingleton(true);
            taskService.startTask(fetchCommand);
        }
    }

    public static class FetchArtifactoryUpdatesJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
            final ArtifactoryUpdatesService newsService =
                    InternalContextHelper.get().beanForType(ArtifactoryUpdatesService.class);
            newsService.fetchMessage();
        }
    }
}
