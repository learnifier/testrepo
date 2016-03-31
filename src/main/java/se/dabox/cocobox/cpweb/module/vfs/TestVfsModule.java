/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.vfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import se.dabox.cocobox.vfs.jsonvfs.JsonVfsWebAction;
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
        return new JsonVfsWebAction(LOGGER, () -> getFilespace(cycle, strOrgId)).
                run(cycle, command);
    }

    private Filespace getFilespace(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo ou = secureGetMiniOrg(cycle, strOrgId);
        Filesystem rootFs = new OrgFolderFilesystem(ou.getId());

        CurrentUserFilespaceFactory factory = new CurrentUserFilespaceFactory(rootFs);
        Filespace fs = factory.newInstance();
        return fs;
    }

}
