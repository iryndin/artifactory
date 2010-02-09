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

package org.artifactory.jcr.ocm;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.exception.RepositoryException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ParentBeanConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Extracts a parent bean when the child is part of a named collection (has to climb 3 levels up to get to the parent),
 * e.g.: AnonAcl/aces/ace1/parentAcl
 *
 * @author Noam Tenne
 */
public class ParentCollectionBeanConverter extends ParentBeanConverterImpl {

    public ParentCollectionBeanConverter(Mapper mapper, ObjectConverter objectConverter,
            AtomicTypeConverterProvider atomicTypeConverterProvider) {
        super(mapper, objectConverter, atomicTypeConverterProvider);
    }

    @Override
    public Object getObject(Session session, Node parentNode, BeanDescriptor beanDescriptor,
            ClassDescriptor beanClassDescriptor, Class beanClass, Object parent)
            throws ObjectContentManagerException, RepositoryException, JcrMappingException {
        try {
            Node grandParentNode = parentNode.getParent();
            if ("/".equals(grandParentNode.getPath())) {
                return null;
            }
            Node greatGrandParentNode = grandParentNode.getParent();
            if ("/".equals(greatGrandParentNode.getPath())) {
                return null;
            }
            return objectConverter.getObject(session, greatGrandParentNode.getPath());

        } catch (javax.jcr.RepositoryException e) {
            throw new RepositoryException(e);
        }
    }
}
