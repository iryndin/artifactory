/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.properties;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.properties.LockableAddProperties;
import org.artifactory.api.properties.PropertiesService;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.exception.CancelException;
import org.artifactory.fs.ItemInfo;
import org.artifactory.io.checksum.ChecksumUtil;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.StoringRepo;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.MutableVfsFile;
import org.artifactory.sapi.fs.MutableVfsFolder;
import org.artifactory.sapi.fs.MutableVfsItem;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.fs.lock.LockingHelper;
import org.artifactory.storage.fs.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the properties service.
 *
 * @author Yossi Shaul
 */
@Service
public class PropertiesServiceImpl implements PropertiesService, LockableAddProperties {
    private static final Logger log = LoggerFactory.getLogger(PropertiesServiceImpl.class);

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private StorageInterceptors interceptors;

    @Override
    @Nonnull
    public Properties getProperties(RepoPath repoPath) {
        Properties properties = repoService.getProperties(repoPath);
        if (properties == null) {
            properties = new PropertiesImpl();
        }
        return properties;
    }

    @Override
    public Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths, String... mandatoryKeys) {
        Map<RepoPath, Properties> map = Maps.newHashMap();

        for (RepoPath path : repoPaths) {
            Properties properties = getProperties(path);

            boolean containsAllKeys = true;

            for (String mandatoryKey : mandatoryKeys) {
                if (!properties.containsKey(mandatoryKey)) {
                    containsAllKeys = false;
                    break;
                }
            }

            if (containsAllKeys) {
                map.put(path, properties);
            }
        }

        return map;
    }

    @Override
    public void setProperties(RepoPath repoPath, Properties newProperties) {
        MutableVfsItem mutableItem;
        try {
            mutableItem = repoService.getMutableItem(repoPath);
            mutableItem.setProperties(newProperties);
        } catch (ItemNotFoundRuntimeException e) {
            log.error("Cannot add properties for {}: Item not found.", repoPath);
            return;
        }
    }

    @Override
    public void addProperty(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,String... values) {
        addProperty(repoPath,propertySet,property,false,values);
    }

    @Override
    public void addProperty(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,
            boolean updateAccessLogger, String... values) {
        if (values == null || values.length == 0) {
            return;
        }

        MutableVfsItem mutableItem;
        try {
            mutableItem = repoService.getMutableItem(repoPath);
        } catch (ItemNotFoundRuntimeException e) {
            log.error("Cannot add properties for {}: Item not found.", repoPath);
            AccessLogger.propertyAddedDenied(repoPath, "Property key " + property + " Item not found");
            return;
        }

        BasicStatusHolder statusHolder = new BasicStatusHolder();
        interceptors.beforePropertyCreate(mutableItem, statusHolder, property.getName(), values);
        CancelException cancelException = statusHolder.getCancelException();
        if (cancelException != null) {
            LockingHelper.removeLockEntry(mutableItem.getRepoPath());
            AccessLogger.propertyAddedDenied(repoPath, "Property key "+property+" Canceled");
            throw cancelException;
        }

        Properties properties = mutableItem.getProperties();

        //Build the xml name of the property
        String xmlPropertyName = getXmlPropertyName(propertySet, property);
        boolean exist=properties.containsKey(xmlPropertyName);
        if (!property.isMultipleChoice()) {
            // If the added property is a single selection, remove any existing values belonging to it, before adding
            // the new one that will replace it
            properties.removeAll(xmlPropertyName);
        }

        properties.putAll(xmlPropertyName, Arrays.asList(values));
        mutableItem.setProperties(properties);
        if(updateAccessLogger){
            if(exist) {
                AccessLogger.propertyUpdated(repoPath, "Property key "+xmlPropertyName);
            }else{
                AccessLogger.propertyAdded(repoPath, "Property key "+xmlPropertyName);
            }
        }
        interceptors.afterPropertyCreate(mutableItem, statusHolder, property.getName(), values);

    }

    @Override
    public void editProperty(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,
            boolean updateAccessLogger,String... values) {
        String propertyName = property.getName();
        if (propertySet != null && StringUtils.isNotBlank(propertySet.getName())) {
            propertyName = propertySet.getName() + "." + propertyName;
        }
        deleteProperty(repoPath, propertyName, false);
        addProperty(repoPath, propertySet, property, false, values);
        if(updateAccessLogger) {
            AccessLogger.propertyUpdated(repoPath, "Property key " + propertyName);
        }
    }

    @Override
    public void addPropertyRecursively(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,
            boolean updateAccessLogger,String... values) {
        StoringRepo storingRepo = repoService.storingRepositoryByKey(repoPath.getRepoKey());
        MutableVfsItem fsItem = storingRepo.getMutableFsItem(repoPath);
        if (fsItem == null) {
            log.warn("No item found at {}. Property not added.", repoPath);
            return;
        }
        addPropertyRecursivelyInternal(fsItem, propertySet, property, updateAccessLogger, values);
    }

    @Override
    public void addPropertyRecursively(RepoPath repoPath, @Nullable PropertySet propertySet, Property property,
            String... values) {
        addPropertyRecursively(repoPath,propertySet,property,false,values);
    }

    @Override
    public void addPropertyRecursivelyMultiple(RepoPath repoPath, @Nullable PropertySet propertySet,
            Map<Property, List<String>> propertyMapFromRequests,boolean updateAccessLogger) {
        ItemInfo itemInfo = repoService.getItemInfo(repoPath);
        if (itemInfo == null) {
            log.warn("No item found at {}. Property not added.", itemInfo.getRepoPath());
            return;
        }
        // add property in multi transaction
        addPropertyMultiTransaction(itemInfo, propertySet, propertyMapFromRequests,updateAccessLogger);
    }

    @Override
    public void addPropertySha256RecursivelyMultiple(RepoPath repoPath) {
        ItemInfo itemInfo = repoService.getItemInfo(repoPath);
        if (itemInfo == null) {
            log.warn("No item found at {}. Property not added.", itemInfo.getRepoPath());
            return;
        }
        // add property in multi transaction
        addSha256PropertyMultiTransaction(itemInfo);
    }


    /**
     * add property in multi transaction mode
     *
     * @param itemInfo    - repo path
     * @param propertySet - property set
     * @param propertyMapFromRequests - properties map from request
     */
    private void addPropertyMultiTransaction(ItemInfo itemInfo, @Nullable PropertySet propertySet,
            Map<Property, List<String>> propertyMapFromRequests,boolean updateAccessLogger) {
        // add current property
        log.debug("start tx and add property to artifact:{}", itemInfo.getRepoPath());
        transactionalMe().addPropertyInternalMultiple(itemInfo.getRepoPath(), propertySet, propertyMapFromRequests
                ,updateAccessLogger);
        if (itemInfo.isFolder()) {
            // add to children recursively
            FileService fileService = ContextHelper.get().beanForType(FileService.class);
            List<ItemInfo> children = fileService.loadChildren(itemInfo.getRepoPath());
            for (ItemInfo child : children) {
                addPropertyMultiTransaction(child, propertySet, propertyMapFromRequests,updateAccessLogger);
            }
        }
    }

    /**
     * add property in multi transaction mode
     *
     * @param itemInfo    - repo path
     */
    private void addSha256PropertyMultiTransaction(ItemInfo itemInfo) {
        if (itemInfo.isFolder()) {
            // add to children recursively
            FileService fileService = ContextHelper.get().beanForType(FileService.class);
            List<ItemInfo> children = fileService.loadChildren(itemInfo.getRepoPath());
            for (ItemInfo child : children) {
                addSha256PropertyMultiTransaction(child);
            }
        }else{
            // add current property
            log.debug("start tx and add property to artifact:{}", itemInfo.getRepoPath());
            transactionalMe().addSha256PropertyInternalMultiple(itemInfo.getRepoPath());
        }
    }

    @Override
    public void addSha256PropertyInternalMultiple(RepoPath repoPath){
        MutableVfsFile mutableVfsItem = (MutableVfsFile) repoService.getMutableItem(repoPath);
        // add single property
        String value = ChecksumUtil.getChecksum(ChecksumType.sha256, mutableVfsItem.getStream());
        if (!StringUtils.isEmpty(value)) {
            log.debug("adding sha256 property with value {}",value);
            Property property = new Property("sha256");
            addProperty(mutableVfsItem.getRepoPath(), null, property, false,value);
            BasicStatusHolder statusHolder = new BasicStatusHolder();
            interceptors.afterPropertyDelete(mutableVfsItem, statusHolder, property.getName());
        }
    }

    @Override
    public void addPropertyInternalMultiple(RepoPath repoPath, @Nullable PropertySet propertySet,
            Map<Property, List<String>> propertyMapFromRequest,boolean updateAccessLogger) {
        MutableVfsItem mutableVfsItem = repoService.getMutableItem(repoPath);
        // add single property
        for (Map.Entry<Property, List<String>> propertyStringEntry : propertyMapFromRequest.entrySet()) {
            List<String> value = propertyStringEntry.getValue();
            String[] values = new String[value.size()];
            value.toArray(values);
            Property property = propertyStringEntry.getKey();
            addProperty(mutableVfsItem.getRepoPath(), propertySet, property, updateAccessLogger,values);
            BasicStatusHolder statusHolder = new BasicStatusHolder();
            interceptors.afterPropertyDelete(mutableVfsItem, statusHolder, property.getName());
        }
    }

    private void addPropertyRecursivelyInternal(MutableVfsItem fsItem, PropertySet propertySet, Property property,
            boolean updateAccessLogger,String[] values) {
        // add property to the current path
        // TODO use addProperty internal and pass to it MutableVfsItem not repoPath.
        addProperty(fsItem.getRepoPath(), propertySet, property,updateAccessLogger, values);

        if (fsItem.isFolder()) {
            // add to children recursively
            List<MutableVfsItem> children = ((MutableVfsFolder) fsItem).getMutableChildren();
            for (MutableVfsItem child : children) {
                addPropertyRecursivelyInternal(child, propertySet, property,updateAccessLogger, values);
            }
        }
        BasicStatusHolder statusHolder = new BasicStatusHolder();
        interceptors.afterPropertyDelete(fsItem, statusHolder, property.getName());
    }


    private LockableAddProperties transactionalMe() {
        return InternalContextHelper.get().beanForType(LockableAddProperties.class);
    }

    @Override
    public boolean deleteProperty(RepoPath repoPath, String property) {
        return deleteProperty(repoPath,property,false);
    }

    @Override
    public void addPropertyRecursivelyMultiple(RepoPath repoPath, @Nullable PropertySet propertySet,
            Map<Property, List<String>> propertyMapFromRequest) {
        addPropertyRecursivelyMultiple(repoPath,propertySet,propertyMapFromRequest,false);
    }

    @Override
    public boolean deleteProperty(RepoPath repoPath, String property,boolean updateAccessLogger) {
        MutableVfsItem mutableItem = repoService.getMutableItem(repoPath);
        if (mutableItem == null) {
            log.error("Cannot change properties for {}: Item not found.", repoPath);
            AccessLogger.propertyDeletedDenied(repoPath, "Property key "+property+" Item not found");
            return false;
        }

        BasicStatusHolder statusHolder = new BasicStatusHolder();
        interceptors.beforePropertyDelete(mutableItem, statusHolder, property);
        CancelException cancelException = statusHolder.getCancelException();
        if (cancelException != null) {
            LockingHelper.removeLockEntry(mutableItem.getRepoPath());
            AccessLogger.propertyDeletedDenied(repoPath, "Property key "+property+" Cancelled");
            throw cancelException;
        }

        Properties properties = mutableItem.getProperties();
        boolean exist=properties.containsKey(property);
        properties.removeAll(property);
        mutableItem.setProperties(properties);
        if(updateAccessLogger){
            if(exist) {
                AccessLogger.propertyDeleted(repoPath, "Property key " + property);
            }
        }
        interceptors.afterPropertyDelete(mutableItem, statusHolder, property);
        return exist;
    }

    @Override
    public void deletePropertyRecursively(RepoPath repoPath, String property,boolean updateAccessLogger) {
        StoringRepo storingRepo = repoService.storingRepositoryByKey(repoPath.getRepoKey());
        MutableVfsItem fsItem = storingRepo.getMutableFsItem(repoPath);
        if (fsItem == null) {
            log.warn("No item found at {}. Property not added.", repoPath);
            return;
        }
        deletePropertyRecursivelyInternal(fsItem, property, updateAccessLogger);
    }

    @Override
    public void deletePropertyRecursively(RepoPath repoPath, String property) {
        deletePropertyRecursively(repoPath,property,false);
    }

    private void deletePropertyRecursivelyInternal(MutableVfsItem fsItem, String property,boolean updateAccessLogger) {
        // add property to the current path
        deleteProperty(fsItem.getRepoPath(), property, updateAccessLogger);

        if (fsItem.isFolder()) {
            // delete from children recursively
            // TODO use deleteProperty internal and pass to it MutableVfsItem not repoPath.
            List<MutableVfsItem> children = ((MutableVfsFolder) fsItem).getMutableChildren();
            for (MutableVfsItem child : children) {
                deletePropertyRecursivelyInternal(child, property,updateAccessLogger);
            }
        }
    }

    /**
     * Builds an xml name of the property set and the property in the pattern of PropertSetName.PropertyName
     *
     * @param propertySet Property set to use - can be null
     * @param property    Property to use
     * @return Xml property name
     */
    private String getXmlPropertyName(@Nullable PropertySet propertySet, Property property) {
        String xmlPropertyName = "";
        if (propertySet != null) {
            String setName = propertySet.getName();
            if (StringUtils.isNotBlank(setName)) {
                xmlPropertyName += setName + ".";
            }
        }
        return xmlPropertyName + property.getName();
    }
}
