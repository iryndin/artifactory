package org.artifactory.jcr.search;

import org.artifactory.sapi.data.VfsNodeType;

/**
 * Date: 8/6/11
 * Time: 7:41 PM
 *
 * @author Fred Simon
 */
abstract class JcrQueryHelper {
    static final String FORWARD_SLASH = "/";
    static final char SLASH_CHAR = '/';

    private JcrQueryHelper() {
    }

    protected static void fillWithNodeTypeFilter(StringBuilder query, VfsNodeType matchNodeType) {
        query.append("element(*, ").append(matchNodeType.storageTypeName).append(")");
    }

    public static void addSlashIfNeeded(StringBuilder query) {
        if (query.charAt(query.length() - 1) != SLASH_CHAR) {
            query.append(SLASH_CHAR);
        }
    }

    public static void addDoubleSlashesIfNeeded(StringBuilder query) {
        int currentLength = query.length();
        char one = query.charAt(currentLength - 1);
        char two = query.charAt(currentLength - 2);
        if (one == '*' && two == SLASH_CHAR) {
            query.setCharAt(currentLength - 1, SLASH_CHAR);
        } else if (two == '*' && one == SLASH_CHAR) {
            query.setCharAt(currentLength - 2, SLASH_CHAR);
            query.deleteCharAt(currentLength - 1);
        } else {
            if (one != SLASH_CHAR || two != SLASH_CHAR) {
                query.append(SLASH_CHAR);
                if (one != SLASH_CHAR) {
                    query.append(SLASH_CHAR);
                }
            }
        }
    }
}
