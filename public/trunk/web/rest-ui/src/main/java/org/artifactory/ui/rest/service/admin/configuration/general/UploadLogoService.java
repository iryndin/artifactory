package org.artifactory.ui.rest.service.admin.configuration.general;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.utils.FileUpload;
import org.artifactory.ui.utils.MultiPartUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;

/**
 * @author Chen keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UploadLogoService implements RestService {

    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        try {
            File tempWorkingDir = ContextHelper.get().getArtifactoryHome().getTempWorkDir();
            String tempFileName = UUID.randomUUID().toString();
            // save file to logo folder
            saveFileToTempLocation(request, tempWorkingDir,tempFileName);
            File tempLogoFile = new File(tempWorkingDir, tempFileName);
            boolean fakeImage = isImageFake(tempLogoFile);
            if(fakeImage){
                tempLogoFile.delete();
                response.error("Invalid file type");
                return;
            }else {
                String logoDir = ContextHelper.get().getArtifactoryHome().getLogoDir().getAbsolutePath();
                File finalLogoFile = new File(logoDir, "logo");
                java.nio.file.Files.move(tempLogoFile.toPath(), finalLogoFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                response.info("Logo Uploaded Successfully");
            }
        }catch (Exception e){
            response.error("error uploading file to server");
        }
    }

    /**
     * save logo to logo folder
     * @param artifactoryRequest - encapsulate data related to request
     */
    private void saveFileToTempLocation(ArtifactoryRestRequest artifactoryRequest, File tempWorkingDir,String tempFileName) {
        FileUpload fileUpload = (FileUpload) artifactoryRequest.getImodel();
        MultiPartUtils.saveSpecificFile(centralConfigService, fileUpload.getFormDataMultiPart(), tempWorkingDir.getAbsolutePath(),
                tempFileName);


    }

    /**
     * check if the image has fake format , its not a real image
     * this check done to eliminate security issue
     */
    private boolean isImageFake(File file) throws Exception {
        boolean isFakeImage = false;
        ImageInputStream imageInputStream = null;
        try {
            Path path = Paths.get(file.getCanonicalPath());
            byte[] data = Files.readAllBytes(path);
            imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
            Iterator<ImageReader> iter = ImageIO.getImageReaders(imageInputStream);
            if (!iter.hasNext()) {
                isFakeImage = true;
            }
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            if (imageInputStream != null) {
                try {
                    imageInputStream.close();
                } catch (IOException e) {
                    throw new IOException(e);
                }
            }
        }
        return isFakeImage;
    }
}
