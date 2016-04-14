/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.material;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.vfs.Filespace;
import se.dabox.cocobox.vfs.Path;
import se.dabox.cocobox.vfs.factory.CurrentUserFilespaceFactory;
import se.dabox.cocobox.vfs.jsonvfs.JsonVfsWebAction;
import se.dabox.cocobox.vfs.orgfolder.GrantedOrgFolderFilesystem;
import se.dabox.cocobox.vfs.orgfolder.OrgFolderFilesystem;
import se.dabox.cocosite.freemarker.util.CdnUtils;
import se.dabox.cocosite.org.MiniOrgInfo;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class LibraryVfsModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryVfsModule.class);

    @WebActionMountpoint("/libvfs.js")
    @WebAction
    public RequestTarget onVfs(RequestCycle cycle, String strOrgId, String command) {
        return new JsonVfsWebAction(LOGGER,
                () -> createFilespace(cycle, strOrgId)).
                run(cycle, command);
    }

    private Filespace createFilespace(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo orgUnit = secureGetMiniOrg(cycle, strOrgId);

        OrgFolderFilesystem rootFs = new OrgFolderFilesystem(orgUnit.getId());

        CurrentUserFilespaceFactory filespaceFactory = new CurrentUserFilespaceFactory(rootFs);
        filespaceFactory.setDefaultUnknownThumbnailUrl(CdnUtils.getResourceUrl(
                "/cocobox/img/producttypes/folder.svg"));

//        GrantedOrgFolderFilesystem grantedFs = new GrantedOrgFolderFilesystem(orgUnit.getId());
//        grantedFs.setRootThumbnailUrl(CdnUtils.getResourceUrl(
//                "/cocobox/img/producttypes/dollarfolder.svg"));
//        grantedFs.setRootDisplayName("Purchased");
//
//        filespaceFactory.mount(Path.valueOf("/purchased"), grantedFs);

        return filespaceFactory.newInstance();
    }

}
