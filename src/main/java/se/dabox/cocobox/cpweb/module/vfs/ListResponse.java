/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.vfs;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import se.dabox.cocobox.vfs.FileInfo;
import se.dabox.cocobox.vfs.FilePermission;
import se.dabox.cocobox.vfs.Path;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
class ListResponse {

    private final List<String> names;
    private final List<EntryResponse> files;
    private final String path;
    private final FileInfo dirInfo;

    ListResponse(Path path, FileInfo dirInfo, List<String> names, List<EntryResponse> responses) {
        this.path = path.toString();
        this.dirInfo = dirInfo;
        this.names = names;
        this.files = responses;
    }

    public String getDisplayName() {
        return dirInfo.getDisplayName();
    }

    public String getThumbnailUrl() {
        return dirInfo.getThumbnailUrl(64);
    }

    public Collection<String> getSupportedOperations() {
        return dirInfo.getSupportedOperations();
    }

    public Map<String, String> getAttributes() {
        return dirInfo.getAttributes();
    }

    public Set<FilePermission> getPermissions() {
        return dirInfo.getPermissions();
    }


    public String getPath() {
        return path;
    }

    public List<String> getNames() {
        return names;
    }

    public List<EntryResponse> getFiles() {
        return files;
    }

}
