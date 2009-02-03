package org.artifactory.webapp.wicket.model;

import org.apache.log4j.Logger;
import wicket.Component;
import wicket.model.CompoundPropertyModel;
import wicket.util.string.Strings;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

/**
 * A compound model that uses MVEL for nested expression evaluation Created by IntelliJ IDEA. User:
 * yoavl
 */
public class GPathPropertyModel extends CompoundPropertyModel {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(GPathPropertyModel.class);

    private final static ScriptEngine engine;

    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("groovy");
        if (engine == null) {
            throw new RuntimeException("Failed to create the scripting engine.");
        }
    }

    /**
     * Constructor
     *
     * @param model The model object, which may or may not implement IModel
     */
    public GPathPropertyModel(final Object model) {
        super(model);
    }

    /**
     * @see wicket.model.AbstractDetachableModel#onGetObject(wicket.Component)
     */
    protected Object onGetObject(final Component component) {
        final Object modelObject = modelObject(component);
        final String expression = propertyExpression(component);
        if (Strings.isEmpty(expression)) {
            //Return a meaningful value for an empty property expression
            return modelObject;
        }
        if (modelObject != null) {
            Object result;
            ScriptContext scriptContext = getScriptContext(modelObject);
            try {
                result = engine.eval("model." + expression, scriptContext);
            } catch (ScriptException e) {
                throw new RuntimeException("Failed to get object.", e);
            }
            return result;
        }
        return null;
    }

    /**
     * Applies the property expression on the model object using the given object argument.
     *
     * @param object The object that will be used when setting a value on the model object
     */
    protected void onSetObject(final Component component, Object object) {
        final String expression = propertyExpression(component);
        if (Strings.isEmpty(expression)) {
            super.onSetObject(component, object);
        } else {
            // Get the real object
            Object modelObject = modelObject(component);
            ScriptContext scriptContext = getScriptContext(modelObject);
            scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("obj", object);
            try {
                engine.eval("model." + expression + " = obj", scriptContext);
            } catch (ScriptException e) {
                throw new RuntimeException("Failed to get object.", e);
            }
        }
    }

    private ScriptContext getScriptContext(Object modelObject) {
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put("model", modelObject);
        return scriptContext;
    }
}
