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

package org.artifactory.message;


import com.google.common.collect.MapMaker;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.message.ArtifactoryUpdatesService;
import org.artifactory.api.message.Message;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Yoav Aharoni
 */
@Service
public class ArtifactoryUpdatesServiceImpl implements ArtifactoryUpdatesService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryUpdatesServiceImpl.class);
    private static final String MESSAGE_CACHE_KEY = "message";

    @Autowired
    private CentralConfigService centralConfigService;

    private Map<String, Message> cache =
            new MapMaker().initialCapacity(1).
                    expireAfterWrite(ConstantValues.artifactoryUpdatesRefreshIntervalSecs.getLong(),
                            TimeUnit.SECONDS).makeMap();

    @Override
    public void fetchMessage() {
        if (!ConstantValues.versionQueryEnabled.getBoolean() && centralConfigService.getDescriptor().isOfflineMode()) {
            return;
        }
        final Message message = getRemoteMessage();
        cache.put(MESSAGE_CACHE_KEY, message);
    }

    @Override
    public Message getCachedMessage() {
        return cache.get(MESSAGE_CACHE_KEY);
    }

    @Override
    public Message getMessage() {
        Message message = getCachedMessage();
        if (ConstantValues.versionQueryEnabled.getBoolean() && !cache.containsKey(MESSAGE_CACHE_KEY)) {
            cache.put(MESSAGE_CACHE_KEY, PROCESSING_MESSAGE);
            ContextHelper.get().beanForType(ArtifactoryUpdatesService.class).fetchMessage();
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
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();

        return new HttpClientConfigurator()
                .soTimeout(5000)
                .connectionTimeout(2000)
                .retry(0, false)
                .proxy(proxy)
                .getClient();
    }
}
