package org.artifactory.common.wicket.component.confirm;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;

/**
 * @author Yoav Aharoni
 */
public interface ConfirmDialog extends Serializable {
    String getMessage();

    void onConfirm(boolean approved, AjaxRequestTarget target);
}
