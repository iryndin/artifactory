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

package org.artifactory.api.mime;

/**
 * Hardcoded mime types enumeration to simplify selection process:<br/> isXml, isChecksum, isPom....<br/> The mime.types
 * will look like:<br/><ul> <li>application/xml         xml pom xsl xsi</li> <li>application/xml-schema  xsd</li>
 * <li>application/xml-external-parsed-entity ent</li> <li>application/java-archive          zip jar war ear sar har
 * rar</li> <li>application/x-java-pack200        jar.pack.gz</li> <li>application/x-java-archive-diff jardiff</li>
 * <li>application/x-java-jnlp-file      jnlp</li> <li>text/x-checksum sha1 asc md5</li> <li>text/plain txt</li></ul>
 *
 * @author freds
 */
public enum ContentType {
    def(new MimeEntry("application/octet-stream")),
    textPlain(new MimeEntry("text/plain", "txt", "properties", "mf"), true),
    textXml(new MimeEntry("text/xml"), true),
    textHtml(new MimeEntry("text/html", "htm", "html"), true),
    applicationXml(new MimeEntry("application/xml", "xml", "xsl", "xsi")/*, textXml*/, true),
    mavenPom(new MimeEntry("application/x-maven-pom+xml", "pom"), true),
    applicationXmlDtd(new MimeEntry("application/xml-dtd", "dtd"), applicationXml, true),
    applicationXmlSchema(new MimeEntry("application/xml-schema", "xsd"), applicationXml, true),
    applicationXmlExternal(new MimeEntry("application/xml-external-parsed-entity", "ent"), applicationXml, true),
    zip(new MimeEntry("application/zip", "zip")),
    javaArchive(new MimeEntry("application/java-archive", "jar", "war", "ear", "sar", "har", "rar")),
    javaPack200(new MimeEntry("application/x-java-pack200", "jar.pack.gz")),
    javaArchiveDiff(new MimeEntry("application/x-java-archive-diff", "jardiff")),
    javaJnlp(new MimeEntry("application/x-java-jnlp-file", "jnlp"), true),
    cheksum(new MimeEntry("application/x-checksum", "sha1", "md5"), textPlain, true),
    ivyXml(new MimeEntry("application/x-ivy+xml", "ivy"), true),
    javaSource(new MimeEntry("text/x-java-source", "java"), true),
    groovySource(new MimeEntry("text/x-groovy-source", "groovy", "gradle"), true),
    css(new MimeEntry("text/css", "css"), true);

    private final MimeEntry mimeEntry;
    private final ContentType alternate;
    private final boolean viewable;

    ContentType(MimeEntry mimeEntry) {
        this(mimeEntry, null, false);
    }

    ContentType(MimeEntry mimeEntry, ContentType alternate) {
        this(mimeEntry, alternate, false);
    }

    ContentType(MimeEntry mimeEntry, boolean viewable) {
        this(mimeEntry, null, viewable);
    }

    ContentType(MimeEntry mimeEntry, ContentType alternate, boolean viewable) {
        this.mimeEntry = mimeEntry;
        this.alternate = alternate;
        this.viewable = viewable;
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

    /**
     * @return True files of this type can be viewed as text file
     */
    public boolean isViewable() {
        return viewable;
    }

    public boolean isXml() {
        if (alternate != null) {
            return alternate.isXml();
        }
        return this == textXml || this == applicationXml || this == mavenPom || this == ivyXml;
    }

    public boolean isChecksum() {
        return this == cheksum;
    }

    public boolean isPom() {
        return this == mavenPom;
    }

    public boolean isJnlp() {
        return this == javaJnlp;
    }

    public boolean isJarVariant() {
        return this == javaArchive;
    }

    public String getDefaultExtension() {
        return mimeEntry.getDefaultExtension();
    }
}
