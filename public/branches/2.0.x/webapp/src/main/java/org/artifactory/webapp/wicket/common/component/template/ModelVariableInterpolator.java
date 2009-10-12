package org.artifactory.webapp.wicket.common.component.template;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.interpolator.VariableInterpolator;

import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class ModelVariableInterpolator extends VariableInterpolator {
    private Map<String, IModel> variables;

    public ModelVariableInterpolator(String string, Map<String, IModel> variables) {
        super(string);
        this.variables = variables;
    }

    /**
     * Constructor.
     *
     * @param string                  a <code>String</code> to interpolate into
     * @param variables               the variables to substitute
     * @param exceptionOnNullVarValue if <code>true</code> an {@link IllegalStateException} will be thrown if
     *                                {@link #getValue(String)} returns <code>null</code>, otherwise the
     *                                <code>${varname}</code> string will be left in the <code>String</code> so that
     *                                multiple interpolators can be chained
     */
    public ModelVariableInterpolator(String string, Map<String, IModel> variables, boolean exceptionOnNullVarValue) {
        super(string, exceptionOnNullVarValue);
        this.variables = variables;
    }

    public void setVariables(Map<String, IModel> variables) {
        this.variables = variables;
    }

    @Override
    protected final String getValue(String variableName) {
        IModel model = variables.get(variableName);
        if (model == null) {
            return null;
        }
        Object value = model.getObject();
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static String interpolate(String string, Map<String, IModel> variables) {
        return new ModelVariableInterpolator(string, variables).toString();
    }
}
