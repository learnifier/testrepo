/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.branding;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class PreviewState implements Serializable, HttpSessionBindingListener {
    private static final long serialVersionUID = 3L;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PreviewState.class);

    private boolean deleteFiles = true;

    private final UUID uuid;
    private final File rawFile;
    private final File previewFile;
    private final int orgwidth;
    private final int orgheight;

    public PreviewState(UUID uuid, File rawFile, File previewFile, int orgwidth, int orgheight) {
        this.uuid = uuid;
        this.rawFile = rawFile;
        this.previewFile = previewFile;
        this.orgwidth = orgwidth;
        this.orgheight = orgheight;
    }

    public int getOrgheight() {
        return orgheight;
    }

    public int getOrgwidth() {
        return orgwidth;
    }

    public File getPreviewFile() {
        return previewFile;
    }

    public File getRawFile() {
        return rawFile;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isDeleteFiles() {
        return deleteFiles;
    }

    public void setDeleteFiles(boolean deleteFiles) {
        this.deleteFiles = deleteFiles;
    }
    
    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        LOGGER.debug("I'm bound to a session!");
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        LOGGER.debug("I'm about to be unbound");
        if (deleteFiles) {
            LOGGER.debug("Deleting file {}", rawFile);
            FileUtils.deleteQuietly(rawFile);
            LOGGER.debug("Deleting file {}", previewFile);
            FileUtils.deleteQuietly(previewFile);
        }
    }

    public String getSessionName() {
        return computeSessionName(uuid.toString());
    }

    public static String computeSessionName(String uuid) {
        return "preview."+uuid;
    }

}
