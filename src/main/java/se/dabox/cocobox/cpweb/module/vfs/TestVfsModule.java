/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.vfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.vfs.AccessDeniedException;
import se.dabox.cocobox.vfs.FileInfo;
import se.dabox.cocobox.vfs.Filespace;
import se.dabox.cocobox.vfs.NotFoundException;
import se.dabox.cocobox.vfs.Path;
import se.dabox.cocobox.vfs.factory.CurrentUserFilespaceFactory;
import se.dabox.cocobox.vfs.orgfolder.OrgFolderFilesystem;
import se.dabox.cocobox.vfs.spi.Filesystem;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class TestVfsModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestVfsModule.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public TestVfsModule() {
    }

    @WebActionMountpoint("/vfs")
    @WebAction
    public RequestTarget onVfs(RequestCycle cycle, String strOrgId, String command) throws JsonProcessingException {
        switch (command) {
            case "list":
                return onList(cycle, strOrgId);
            case "op":
                return onOperation(cycle, strOrgId);
            default:
                throw new IllegalStateException("Invalid command: " + command);
        }
    }

    public RequestTarget onList(RequestCycle cycle, String strOrgId) throws JsonProcessingException {
        Filespace fs = getFilespace(cycle, strOrgId);

        Path path = getPath(cycle);
        try {
            FileInfo dirInfo = fs.stat(path);
            List<FileInfo> entries = fs.statdir(path);

            List<EntryResponse> responses = CollectionsUtil.transformList(entries,
                    EntryResponse::new);

            List<String> names = getDisplayNamesForPath(fs, path);

            ListResponse resp = new ListResponse(path, dirInfo, names, responses);

            return jsonTarget(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(resp));
        } catch (NotFoundException nfe) {
            return new ErrorCodeRequestTarget(404);
        }
    }

    private RequestTarget onOperation(RequestCycle cycle, String strOrgId) {
        String op = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "op");
        String requestJson = cycle.getRequest().getParameter("request");

        Filespace fs = getFilespace(cycle, strOrgId);

        Path path = getPath(cycle);

        Class<?> paramType = fs.execParamType(path, op);
        Object param = null;

        if (paramType != null) {
            String paramJson = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(),
                    "request");

            try {
                param = mapper.readValue(paramJson, paramType);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to process response object", ex);
            }
        }

        Object resp = fs.exec(path, op, param);

        try {
            byte[] data = mapper.writeValueAsBytes(new OpResponse("ok", resp));

            return new JsonRequestTarget(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to encode response", ex);
        }
    }

    private List<String> getDisplayNamesForPath(Filespace fs, Path path) {
        List<String> names = new ArrayList<>(path.getNameCount());
        names.add("/");
        for (int i = 1; i <= path.getNameCount(); i++) {
            Path subpath = path.subpath(0, i);
            try {
                FileInfo spInfo = fs.stat(subpath);
                names.add(spInfo.getDisplayName());
            } catch (NotFoundException | AccessDeniedException notFoundException) {
                names.add(path.getName(i).getName());
            }
        }
        return names;
    }

    private Path getPath(RequestCycle cycle) throws RetargetException {

        String strPath = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "path");

        final Path path = Path.valueOf(strPath);
        return path;
    }

    private Filespace getFilespace(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo ou = secureGetMiniOrg(cycle, strOrgId);
        Filesystem rootFs = new OrgFolderFilesystem(ou.getId());
        CurrentUserFilespaceFactory factory = new CurrentUserFilespaceFactory(rootFs);
        Filespace fs = factory.newInstance();
        return fs;
    }

}
