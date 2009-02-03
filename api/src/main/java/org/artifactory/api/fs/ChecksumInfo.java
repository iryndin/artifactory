package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.mime.ChecksumType;

import java.io.Serializable;

/**
 * Holds original and calculated values of a checksum.
 *
 * @author Yossi Shaul
 */
@XStreamAlias("checksum")
public class ChecksumInfo implements Serializable {
    // marks a checksum type with no original checksum to be safe.
    // this marker is used when a file is deployed and we don't have the remote
    // checksum but we have the actual file
    public static final String TRUSTED_FILE_MARKER = "NO_ORIG";

    private ChecksumType type;
    private String original;
    private String actual;

    public ChecksumInfo(ChecksumType type) {
        this.type = type;
    }

    public ChecksumInfo(ChecksumType type, String original, String actual) {
        this.type = type;
        this.original = original;
        this.actual = actual;
    }

    public ChecksumType getType() {
        return type;
    }

    public String getOriginal() {
        if (isMarkedAsTrusted()) {
            return getActual();
        } else {
            return original;
        }
    }

    public String getActual() {
        return actual;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public boolean checksumsMatches() {
        return original != null && actual != null && (isMarkedAsTrusted() || actual.equals(original));
    }

    public boolean isMarkedAsTrusted() {
        return TRUSTED_FILE_MARKER.equals(original);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChecksumInfo info = (ChecksumInfo) o;

        if (actual != null ? !actual.equals(info.actual) : info.actual != null) {
            return false;
        }
        if (original != null ? !original.equals(info.original) : info.original != null) {
            return false;
        }
        if (type != info.type) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (original != null ? original.hashCode() : 0);
        result = 31 * result + (actual != null ? actual.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChecksumInfo{" +
                "type=" + type +
                ", original='" + original + '\'' +
                ", actual='" + actual + '\'' +
                '}';
    }

}