/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.mime;

/**
 * Hardcoded mime types enumeration to simplify selection process:<br/> isXml, isChecksum,
 * isPom....<br/> The mime.types will look like:<br/><ul> <li>application/xml         xml pom xsl
 * xsi</li> <li>application/xml-schema  xsd</li> <li>application/xml-external-parsed-entity ent</li>
 * <li>application/java-archive          zip jar war ear sar har rar</li>
 * <li>application/x-java-pack200        jar.pack.gz</li> <li>application/x-java-archive-diff
 * jardiff</li> <li>application/x-java-jnlp-file      jnlp</li> <li>text/x-checksum sha1 asc
 * md5</li> <li>text/plain                        txt</li></ul>
 *
 * @author freds
 */
public enum ContentType {
    def(new MimeEntry("application/octet-stream")),
    textPlain(new MimeEntry("text/plain", "txt")),
    textXml(new MimeEntry("text/xml")),
    applicationXml(new MimeEntry("application/xml", "xml", "xsl", "xsi")/*, textXml*/),
    mavenPom(new MimeEntry("application/x-maven-pom+xml", "pom")),
    applicationXmlDtd(new MimeEntry("application/xml-dtd", "dtd"), applicationXml),
    applicationXmlSchema(new MimeEntry("application/xml-schema", "xsd"), applicationXml),
    applicationXmlExternal(new MimeEntry("application/xml-external-parsed-entity", "ent"), applicationXml),
    zip(new MimeEntry("application/zip", "zip")),
    javaArchive(new MimeEntry("application/java-archive", "jar", "war", "ear", "sar", "har", "rar"), zip),
    javaPack200(new MimeEntry("application/x-java-pack200", "jar.pack.gz")),
    javaArchiveDiff(new MimeEntry("application/x-java-archive-diff", "jardiff")),
    javaJnlp(new MimeEntry("application/x-java-jnlp-file", "jnlp")),
    cheksum(new MimeEntry("application/x-checksum", "sha1", "asc", "md5"), textPlain);

    private final MimeEntry mimeEntry;
    private final ContentType alternate;

    ContentType(MimeEntry mimeEntry) {
        this.mimeEntry = mimeEntry;
        this.alternate = null;
    }

    ContentType(MimeEntry mimeEntry, ContentType alternate) {
        this.mimeEntry = mimeEntry;
        this.alternate = alternate;
    }

    public String getMimeType() {
        if (alternate != null) {
            return alternate.getMimeType();
        }
        return mimeEntry.getMimeType();
    }

    public MimeEntry getMimeEntry() {
        return mimeEntry;
    }

    public ContentType getAlternate() {
        return alternate;
    }

    public boolean isXml() {
        if (alternate != null) {
            return alternate.isXml();
        }
        return this == textXml || this == applicationXml || this == mavenPom;
    }

    public boolean isChecksum() {
        return this == cheksum;
    }

    public boolean isPom() {
        return this == mavenPom;
    }

    public boolean isJarVariant() {
        return this == javaArchive;
    }

    public String getDefaultExtension() {
        return mimeEntry.getDefaultExtension();
    }
}
