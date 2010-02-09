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

package org.artifactory.search.lucene;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Customized term position vector that assists the creation of excerpts from property specific searches on the
 * artifactory:archiveEntry.
 * <p/>
 * Based on: org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0-sources.jar!/org/apache/jackrabbit/core/
 * query/lucene/AbstractExcerpt.java
 * <p/>
 * The implementation mostly remained unchanged apart from the addition of the future entry map.<P> The need for this
 * extra map arrises from the fact that property specific searches on artifactory:archiveEntry return terms that are
 * taken from the customized analyzer, but are compared to the terms of the property value taken from the standard
 * analyzer.
 * <p/>
 * For example, a property contains: "/a/b.class d.class". <p> It will be tokenized by the standard analyzer and saved
 * in the org.artifactory.search.lucene.ArchiveEntryTermPositionVector.terms array as : <ul> <li>/a/b.class</li>
 * <li>d.class</li> </ul>
 * <p/>
 * When the highlighter requests the indexes of terms found in searches, the terms are tokenized by the custom analyzer
 * and are given as: <ul> <li>b.class</li> <li>d.class</li> </ul>
 * <p/>
 * And then the binary search done over the org.artifactory.search.lucene.ArchiveEntryTermPositionVector.terms array is
 * broken.
 * <p/>
 * The added multi map contains a mapping of class name -> full path, so when an index is requested, the full path will
 * be located in the map, and only the the binary search over the array will be done
 *
 * @author Noam Y. Tenne
 */
public class ArchiveEntryTermPositionVector implements TermPositionVector {

    private static final Logger log = LoggerFactory.getLogger(ArchiveEntryTermPositionVector.class);

    private SortedMap termMap;
    private String[] terms;

    private Future<Multimap<String, String>> futureEntryMap;

    public ArchiveEntryTermPositionVector(SortedMap termMap) {
        this.termMap = termMap;
        this.terms = (String[]) termMap.keySet().toArray(new String[termMap.size()]);
        initFutureEntryMap();
    }

    /**
     * Initializes and submits the calculation of the class name -> full path map
     */
    private void initFutureEntryMap() {
        CachedThreadPoolTaskExecutor executor =
                InternalContextHelper.get().beanForType(CachedThreadPoolTaskExecutor.class);
        Callable<Multimap<String, String>> futureEntryMapCallable = new Callable<Multimap<String, String>>() {
            public Multimap<String, String> call() throws Exception {
                Multimap<String, String> map = HashMultimap.create();
                for (String term : terms) {
                    if (StringUtils.isNotBlank(term)) {
                        String key = term;
                        int lastSlashIndex = term.lastIndexOf('/');
                        if (lastSlashIndex != -1) {
                            key = term.substring((lastSlashIndex + 1), term.length());
                        }

                        map.put(key, term);
                    }
                }
                return map;
            }
        };
        futureEntryMap = executor.submit(futureEntryMapCallable);
    }

    public int[] getTermPositions(int index) {
        return null;
    }

    public TermVectorOffsetInfo[] getOffsets(int index) {
        TermVectorOffsetInfo[] info = TermVectorOffsetInfo.EMPTY_OFFSET_INFO;
        if (index >= 0 && index < terms.length) {
            info = (TermVectorOffsetInfo[]) termMap.get(terms[index]);
        }
        return info;
    }

    public String getField() {
        return "";
    }

    public int size() {
        return terms.length;
    }

    public String[] getTerms() {
        return terms;
    }

    public int[] getTermFrequencies() {
        int[] freqs = new int[terms.length];
        for (int i = 0; i < terms.length; i++) {
            freqs[i] = ((TermVectorOffsetInfo[]) termMap.get(terms[i])).length;
        }
        return freqs;
    }

    public int indexOf(String term) {
        int res = Arrays.binarySearch(terms, term);
        return res >= 0 ? res : -1;
    }

    public int[] indexesOf(String[] terms, int start, int len) {
        Multimap<String, String> map;
        try {
            map = futureEntryMap.get();
        } catch (Exception e) {
            log.error("Error occurred acquiring 'class name -> full path' term map.", e);
            map = HashMultimap.create();
        }
        int[] res = new int[len];
        for (int i = 0; i < len; i++) {
            String term = terms[i];
            res[i] = -1;
            if (map.containsKey(term)) {
                Iterator<String> iterator = map.get(term).iterator();
                if (iterator.hasNext()) {
                    res[i] = indexOf(iterator.next());
                    iterator.remove();
                }
            }

        }
        return res;
    }
}