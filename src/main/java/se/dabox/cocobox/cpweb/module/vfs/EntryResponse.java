/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.vfs;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import se.dabox.cocobox.vfs.FileInfo;
import se.dabox.cocobox.vfs.FilePermission;
import se.dabox.cocobox.vfs.FileType;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class EntryResponse {
    private static final int THUMBNAIL_SIZE = 64;

    private final FileInfo info;

    public EntryResponse(FileInfo info) {
        this.info = info;
    }

    public String getFileName() {
        return info.getFileName().getName();
    }

    public String getDisplayName() {
        return info.getDisplayName();
    }

    public FileType getType() {
        return info.getType();
    }

    public String getThumbnailUrl() {
        return info.getThumbnailUrl(THUMBNAIL_SIZE);
    }

    public Collection<String> getSupportedOperations() {
        return info.getSupportedOperations();
    }

    public Map<String, String> getAttributes() {
        return info.getAttributes();
    }

    public Set<FilePermission> getPermissions() {
        return info.getPermissions();
    }

}
