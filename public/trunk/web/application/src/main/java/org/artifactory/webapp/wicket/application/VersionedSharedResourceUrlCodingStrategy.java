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

package org.artifactory.webapp.wicket.application;

import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.util.time.Duration;

/**
 * @author Yoav Aharoni
 */
public class VersionedSharedResourceUrlCodingStrategy implements IRequestMapper {
    private static final Duration MAX_DURATION = Duration.days(365);
    private String mountPath;
    private Duration cacheDuration;
    private String etag;

    public VersionedSharedResourceUrlCodingStrategy(String mountPath, Duration cacheDuration) {
    }

    @Override
    public IRequestHandler mapRequest(Request request) {
        return null;
    }

    @Override
    public int getCompatibilityScore(Request request) {
        return 0;
    }

    @Override
    public Url mapHandler(IRequestHandler requestHandler) {
        return null;
    }

    //public VersionedSharedResourceUrlCodingStrategy(String mountPath, Duration cacheDuration) {
    //    setMountPath(mountPath);
    //    setCacheDuration(cacheDuration);
    //}
    //
    //public Duration getCacheDuration() {
    //    return cacheDuration;
    //}
    //
    //public void setCacheDuration(Duration cacheDuration) {
    //    if (MAX_DURATION.getMilliseconds() < cacheDuration.getMilliseconds()) {
    //        throw new IllegalArgumentException(
    //                String.format("Cache duration is larger than the maximum of [%s].", MAX_DURATION));
    //    }
    //    this.cacheDuration = cacheDuration;
    //}
    //
    //public String getMountPath() {
    //    return mountPath;
    //}
    //
    //public void setMountPath(String mountPath) {
    //    this.mountPath = mountPath;
    //    etag = String.valueOf(mountPath.hashCode());
    //}
    //
    ///**
    // * @see IRequestTargetUrlCodingStrategy#decode(RequestParameters)
    // */
    //public IRequestTarget decode(RequestParameters requestParameters) {
    //    requestParameters.setResourceKey(requestParameters.getPath().substring(getMountPath().length()));
    //    return new CachedSharedResourceRequestTarget(requestParameters);
    //}
    //
    ///**
    // * @see IRequestTargetUrlCodingStrategy#encode(IRequestTarget)
    // */
    //public CharSequence encode(IRequestTarget requestTarget) {
    //    checkTarget(requestTarget);
    //    ISharedResourceRequestTarget target = (ISharedResourceRequestTarget) requestTarget;
    //    return getMountPath() + target.getRequestParameters().getResourceKey();
    //}
    //
    //public boolean matches(IRequestTarget requestTarget) {
    //    return requestTarget instanceof ISharedResourceRequestTarget;
    //}
    //
    //public boolean matches(String path, boolean caseSensitive) {
    //    return path.startsWith(getMountPath());
    //}
    //
    //private void checkTarget(IRequestTarget requestTarget) {
    //    if (!(requestTarget instanceof ISharedResourceRequestTarget)) {
    //        throw new IllegalArgumentException("This encoder can only be used with " +
    //                "instances of " + ISharedResourceRequestTarget.class.getName());
    //    }
    //}
    //
    //private class CachedSharedResourceRequestTarget extends SharedResourceRequestTarget {
    //    private CachedSharedResourceRequestTarget(RequestParameters requestParameters) {
    //        super(requestParameters);
    //    }
    //
    //    @Override
    //    public void respond(RequestCycle requestCycle) {
    //        super.respond(requestCycle);
    //        Response response = requestCycle.getResponse();
    //        if (response instanceof WebResponse) {
    //            WebResponse webResponse = (WebResponse) response;
    //            Duration duration = getCacheDuration();
    //            long expireDate = System.currentTimeMillis() + duration.getMilliseconds();
    //            webResponse.setDateHeader("Date", System.currentTimeMillis());
    //            webResponse.setDateHeader("Expires", expireDate);
    //            webResponse.setHeader("ETag", etag);
    //            webResponse.setHeader("Cache-Control", "public, max-age=" + (long) duration.seconds());
    //            webResponse.setHeader("Vary", "Accept-Encoding");
    //        }
    //    }
    //}

}