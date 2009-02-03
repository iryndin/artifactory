package org.artifactory.test.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores mock test statistics
 *
 * @author Noam Tenne
 */
public class TestStats {

    /**
     * The name of the MockTest that the stats belong to
     */
    private String name;

    /**
     * A map of MockPathTests and an array list of their stats
     */
    private Map<String, ArrayList<PathStats>> pathStatsMap =
            new HashMap<String, ArrayList<PathStats>>();

    /**
     * Main constructor
     *
     * @param name
     */
    public TestStats(String name) {
        this.name = name;
    }

    /**
     * Sets the name of the mock tests that the stats belong to
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add a PathStats object to the PathTest stats array list
     *
     * @param pathName
     * @param stats
     */
    public synchronized void addPathStats(String pathName, PathStats stats) {
        ArrayList<PathStats> statsArray;

        if (pathStatsMap.containsKey(pathName)) {
            statsArray = pathStatsMap.get(pathName);
        } else {
            statsArray = new ArrayList<PathStats>();
        }

        statsArray.add(stats);
        pathStatsMap.put(pathName, statsArray);
    }

    /**
     * Returns the comple stats map
     *
     * @return Map
     */
    public Map<String, ArrayList<PathStats>> getStatsMap() {
        return pathStatsMap;
    }

    /**
     * Returns a count of requests the were made to the given path name with the given method name
     *
     * @param pathName
     * @param methodName
     * @return
     */
    public int requestsPerMethod(String pathName, String methodName) {
        int count = 0;

        if (pathStatsMap.containsKey(pathName)) {
            ArrayList<PathStats> pathStatsArray = pathStatsMap.get(pathName);
            for (PathStats stats : pathStatsArray) {
                if (stats.getMethodName().equals(methodName)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a count of requests the were made with the given method name
     *
     * @param methodName
     * @return
     */
    public int requestsPerMethod(String methodName) {
        int count = 0;

        for (ArrayList<PathStats> statses : pathStatsMap.values()) {
            for (PathStats stats : statses) {
                if (stats.getMethodName().equals(methodName)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a count of requests the were made to the given path name with the given return code
     *
     * @param pathName
     * @param returnCode
     * @return
     */
    public int requestsPerReturnCode(String pathName, int returnCode) {
        int count = 0;

        if (pathStatsMap.containsKey(pathName)) {
            ArrayList<PathStats> pathStatsArray = pathStatsMap.get(pathName);
            for (PathStats stats : pathStatsArray) {
                if (stats.getReturnCode() == returnCode) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a count of requests the were made with the given return code
     *
     * @param returnCode
     * @return
     */
    public int requestsPerReturnCode(int returnCode) {
        int count = 0;

        for (ArrayList<PathStats> statses : pathStatsMap.values()) {
            for (PathStats stats : statses) {
                if (stats.getReturnCode() == returnCode) {
                    count++;
                }
            }
        }

        return count;
    }
}
