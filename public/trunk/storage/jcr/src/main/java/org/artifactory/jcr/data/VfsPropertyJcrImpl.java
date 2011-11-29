package org.artifactory.jcr.data;

import com.google.common.collect.Lists;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.MutableVfsProperty;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * Date: 8/5/11
 * Time: 12:52 AM
 *
 * @author Fred Simon
 */
public class VfsPropertyJcrImpl implements MutableVfsProperty {
    private final Property property;
    private VfsValueType valueType;
    private VfsPropertyType propertyType;

    public VfsPropertyJcrImpl(Property property) {
        this.property = property;
        try {
            if (property.isMultiple()) {
                propertyType = VfsPropertyType.MULTI_VALUE;
                int type = property.getValues()[0].getType();
                this.valueType = JcrVfsHelper.getValueTypeFromJcrType(type);
            } else {
                propertyType = VfsPropertyType.AUTO;
                int type = property.getValue().getType();
                this.valueType = JcrVfsHelper.getValueTypeFromJcrType(type);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public VfsPropertyJcrImpl(Property property, VfsValueType valueType, VfsPropertyType propertyType) {
        this.property = property;
        this.valueType = valueType;
        this.propertyType = propertyType;
    }

    public VfsValueType getValueType() {
        return valueType;
    }

    public VfsPropertyType getPropertyType() {
        try {
            if (propertyType == VfsPropertyType.AUTO) {
                if (property.isMultiple()) {
                    propertyType = VfsPropertyType.MULTI_VALUE;
                } else {
                    return VfsPropertyType.SINGLE;
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return propertyType;
    }

    public String getString() {
        try {
            return property.getString();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Collection<String> getStrings() {
        List<String> result = Lists.newArrayList();
        try {
            if (property.isMultiple()) {
                Value[] values = property.getValues();
                for (Value value : values) {
                    result.add(value.getString());
                }
            } else {
                result.add(property.getString());
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return result;
    }

    public Long getLong() {
        try {
            return property.getLong();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Calendar getDate() {
        try {
            return property.getDate();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    // Mutable part

    public void setString(String value) {
        try {
            property.setValue(value);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void addString(String value) {
        Collection<String> strings = getStrings();
        strings.add(value);
        setStrings(strings);
    }

    public void setStrings(Collection<String> value) {
        try {
            property.setValue(value.toArray(new String[value.size()]));
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void setLong(Long value) {
        try {
            property.setValue(value);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public void setDate(Calendar value) {
        try {
            property.setValue(value);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
