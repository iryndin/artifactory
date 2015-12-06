package org.artifactory.exception;

/**
 * Exception thrown on invalid input from a UI control.
 *
 * @author Yossi Shaul
 */
public class ValidationException extends Exception {
    private Integer index;
    /**
     * Builds a new validation exception with message to display to the user.
     *
     * @param uiMessage Message to display in the UI
     */
    public ValidationException(String uiMessage) {
        super(uiMessage);
    }
    /**
     * Builds a new validation exception with message to display to the user.
     *
     * @param uiMessage Message to display in the UI
     * @param index the index of invalid char
     */
    public ValidationException(String uiMessage, int index) {
        super(uiMessage);
        this.index = Integer.valueOf(index);
    }

    /**
     * @return the index of invalid char
     */
    public int getIndex() {
        return index == null ? -1 : index.intValue();
    }
}
