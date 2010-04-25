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

package org.artifactory.ivy;

import org.apache.commons.io.IOUtils;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.repository.BasicResource;
import org.apache.ivy.plugins.repository.Resource;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.StringInputStream;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;

@Service
public class IvyServiceImpl implements IvyService {

    private static final Logger log = LoggerFactory.getLogger(IvyServiceImpl.class);

    @Autowired
    private InternalRepositoryService repositoryService;

    private final ParserSettings settings = new IvySettings();

    public ModuleDescriptor parseIvyFile(File file) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            return parseIvy(input, file.length());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not parse Ivy file.", e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public ModuleDescriptor parseIvyFile(RepoPath repoPath) {
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        String content = localRepo.getTextFileContent(repoPath);
        StringInputStream input = null;
        try {
            input = new StringInputStream(content);
            return parseIvy(input, content.length());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not parse Ivy file.", e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    private ModuleDescriptor parseIvy(InputStream input, long contentLength) {
        IvyParser ivyParser = new IvyParser();
        try {
            ModuleDescriptor md = ivyParser.getModuleDescriptorForStringContent(input, contentLength);
            return md;
        } catch (Exception e) {
            log.warn("Could not parse the item at {} as a valid Ivy file.", e);
            return null;
        }
    }

    class IvyParser extends XmlModuleDescriptorParser {
        public ModuleDescriptor getModuleDescriptorForStringContent(InputStream input, long contentLength)
                throws IOException, ParseException {
            Resource resource = new BasicResource("ivyBasicResource", true, contentLength, new Date().getTime(), true);
            return getModuleDescriptor(settings, input, resource, true);
        }

        public ModuleDescriptor getModuleDescriptor(ParserSettings settings, InputStream input,
                Resource res, boolean validate) throws ParseException, IOException {
            Parser parser = newParser(settings);
            parser.setValidate(validate);
            parser.setResource(res);
            parser.setInput(input);
            parser.parse();
            return parser.getModuleDescriptor();
        }
    }
}

