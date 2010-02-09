/*
 * This file is part of Artifactory.
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
    textPlain(new MimeEntry("text/plain", "txt")),
    textXml(new MimeEntry("text/xml")),
    //textHtml(new MimeEntry("text/html", "htm", "html")),
    applicationXml(new MimeEntry("application/xml", "xml", "xsl", "xsi")/*, textXml*/),
    mavenPom(new MimeEntry("application/x-maven-pom+xml", "pom")),
    applicationXmlDtd(new MimeEntry("application/xml-dtd", "dtd"), applicationXml),
    applicationXmlSchema(new MimeEntry("application/xml-schema", "xsd"), applicationXml),
    applicationXmlExternal(new MimeEntry("application/xml-external-parsed-entity", "ent"), applicationXml),
    zip(new MimeEntry("application/zip", "zip")),
    javaArchive(new MimeEntry("application/java-archive", "jar", "war", "ear", "sar", "har", "rar")),
    javaPack200(new MimeEntry("application/x-java-pack200", "jar.pack.gz")),
    javaArchiveDiff(new MimeEntry("application/x-java-archive-diff", "jardiff")),
    javaJnlp(new MimeEntry("application/x-java-jnlp-file", "jnlp")),
    cheksum(new MimeEntry("application/x-checksum", "sha1", "asc", "md5"), textPlain),
    ivyXml(new MimeEntry("application/x-ivy+xml", "ivy"));

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
