package org.artifactory.addon;

/**
 * The states that an installed addon can be in
 *
 * @author Noam Y. Tenne
 */
public enum AddonState {
    ACTIVATED("activated", "Activated"),
    DISABLED("disabled", "Disabled"),
    INACTIVE("unactivated", "Unactivated");

    private String state;
    private String stateName;

    /**
     * Main constructor
     *
     * @param state     State
     * @param stateName State name
     */
    AddonState(String state, String stateName) {
        this.state = state;
        this.stateName = stateName;
    }

    /**
     * Returns the state
     *
     * @return State
     */
    public String getState() {
        return state;
    }

    /**
     * Returns the name of the state
     *
     * @return State name
     */
    public String getStateName() {
        return stateName;
    }
}
