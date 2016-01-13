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

package org.artifactory.repo.http;

import com.google.common.collect.Lists;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.observers.CloseableObserver;

import java.io.IOException;
import java.util.List;

/**
 * Provides decoration capabilities for HttpClient,
 * where {@link CloseableObserver} can register to
 * onClose() event
 *
 * @author Michael Pasternak
 */
public class CloseableHttpClientDecorator extends CloseableHttpClient {

    private final CloseableHttpClient closeableHttpClient;
    private final List<CloseableObserver> closeableObservers;
    private final IdleConnectionMonitorService idleConnectionMonitorService;

    /**
     * @param closeableHttpClient {@link CloseableHttpClient}
     * @param clientConnectionManager {@link PoolingHttpClientConnectionManager}
     */
    public CloseableHttpClientDecorator(CloseableHttpClient closeableHttpClient,
            PoolingHttpClientConnectionManager clientConnectionManager) {
        assert closeableHttpClient != null : "closeableHttpClient cannot be empty";
        assert clientConnectionManager != null : "clientConnectionManager cannot be empty";
        this.closeableObservers = Lists.newArrayList();
        this.closeableHttpClient = closeableHttpClient;
        idleConnectionMonitorService = ContextHelper.get()
                .beanForType(IdleConnectionMonitorService.class);

        idleConnectionMonitorService.add(this, clientConnectionManager);
        registerCloseableObserver((CloseableObserver) idleConnectionMonitorService);
    }

    /**
     * Release resources and unregister itself from {@link IdleConnectionMonitorService}
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        // notify listeners
        onClose();
        // release resources
        closeableHttpClient.close();
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context)
            throws IOException {
        return closeableHttpClient.execute(target, request, context);
    }

    @Deprecated
    @Override
    public HttpParams getParams() {
        return closeableHttpClient.getParams();
    }

    @Deprecated
    @Override
    public ClientConnectionManager getConnectionManager() {
        return closeableHttpClient.getConnectionManager();
    }

    /**
     * @return {@link CloseableHttpClient}
     */
    public final CloseableHttpClient getDecorated() {
        return closeableHttpClient;
    }

    /**
     * Registers {@link CloseableObserver}
     *
     * @param closeableObserver
     */
    public final void registerCloseableObserver(CloseableObserver closeableObserver) {
        closeableObservers.add(closeableObserver);
    }

    /**
     * Fired on close() event
     */
    private void onClose() {
        closeableObservers.stream().forEach(o -> o.onObservedClose(this));
    }
}
