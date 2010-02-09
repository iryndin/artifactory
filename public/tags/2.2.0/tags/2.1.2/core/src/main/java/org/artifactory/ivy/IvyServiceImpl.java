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
        FileInputStream input;
        try {
            input = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Could not parse Ivy file.", e);
        }
        return parseIvy(input);
    }

    public ModuleDescriptor parseIvyFile(RepoPath repoPath) {
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        String content = localRepo.getTextFileContent(repoPath);
        StringInputStream input;
        try {
            input = new StringInputStream(content);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not parse Ivy file.", e);
        }
        return parseIvy(input);
    }

    private ModuleDescriptor parseIvy(InputStream input) {
        IvyParser ivyParser = new IvyParser();
        try {
            ModuleDescriptor md = ivyParser.getModuleDescriptorForStringContent(input);
            return md;
        } catch (Exception e) {
            log.warn("Could not parse the item at {} as a valid Ivy file.", e);
            return null;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    class IvyParser extends XmlModuleDescriptorParser {
        public ModuleDescriptor getModuleDescriptorForStringContent(InputStream input)
                throws IOException, ParseException {
            //TODO: [by YS] pass resource to this method (fileresource if file, basic with data from the jcrfile)
            Resource resource = new BasicResource("ivyBasicResource", true, 1000l, new Date().getTime(), true);
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

