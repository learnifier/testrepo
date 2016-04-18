/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.material;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.vfs.Filespace;
import se.dabox.cocobox.vfs.Path;
import se.dabox.cocobox.vfs.factory.CurrentUserFilespaceFactory;
import se.dabox.cocobox.vfs.jsonvfs.JsonVfsWebAction;
import se.dabox.cocobox.vfs.orgfolder.GrantedOrgFolderFilesystem;
import se.dabox.cocobox.vfs.orgfolder.OrgFolderFilesystem;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.freemarker.util.CdnUtils;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.folder.FolderId;
import se.dabox.service.common.ccbc.folder.OrgMaterialFolder;
import se.dabox.service.common.ccbc.folder.OrgMaterialFolderClient;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
@WebModuleMountpoint("/libvfs-util.js")
public class LibraryVfsModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryVfsModule.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @WebActionMountpoint("/libvfs.js")
    @WebAction
    public RequestTarget onVfs(RequestCycle cycle, String strOrgId, String command) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_VIEW_PRODUCTS);

        final JsonVfsWebAction vfsAction
                = new JsonVfsWebAction(LOGGER,
                        () -> createFilespace(cycle, strOrgId));

        vfsAction.setLocale(CocositeUserHelper.getRequestUserLocale(cycle));
        vfsAction.setZoneId(CocositeUserHelper.getZoneId(cycle));

        return vfsAction.run(cycle, command);
    }

    /**
     * Resolves a productId to a VFS path for this org unit.
     *
     * <p>
     * This method action expects a {@code productId} query parameter with a product id.
     * </p>
     *
     * <p>
     * The response is a json object in the following style:
     * </p>
     *
     * <pre>{"status": "ok", "path": "/some/path"}</pre>
     *
     * <p>
     * Path is only returned if the status is "ok".
     * </p>
     *
     * <p>
     *  HTTP Error code 404 if the path could not be resolved. HTTP Error code 400 is returned
     * if no {@code productId} query parameter is specified or is blank.
     * </p>
     *
     * @param cycle
     * @param strOrgId An organization id.
     * @return
     */
    @WebAction
    public RequestTarget onResolveProduct(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_PRODUCTS);

        String productId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(),
                "productId");

        List<OrgProduct> orgProds = getCocoboxCordinatorClient(cycle).listOrgProducts(org.getId());

        Optional<OrgProduct> orgProd
                = orgProds.stream().filter(op -> productId.equals(op.getProdId())).findAny();

        if (!orgProd.isPresent()) {
            LOGGER.warn("Unable to find product {} for org unit {}", productId, strOrgId);
            return new ErrorCodeRequestTarget(404);
        }

        String path = resolveOrgProduct(cycle, org.getId(), orgProd.get());

        return toJsonResponse(new OrgProductResolveResponse("ok", path));
    }

    private Filespace createFilespace(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo orgUnit = secureGetMiniOrg(cycle, strOrgId);

        OrgFolderFilesystem rootFs = new OrgFolderFilesystem(orgUnit.getId());

        CurrentUserFilespaceFactory filespaceFactory = new CurrentUserFilespaceFactory(rootFs);
        filespaceFactory.setDefaultUnknownThumbnailUrl(CdnUtils.getResourceUrl(
                "/cocobox/img/producttypes/folder.svg"));

        GrantedOrgFolderFilesystem grantedFs = new GrantedOrgFolderFilesystem(orgUnit.getId());
        grantedFs.setRootThumbnailUrl(CdnUtils.getResourceUrl(
                "/cocobox/img/producttypes/dollarfolder.svg"));
        grantedFs.setRootDisplayName("Purchased");

        filespaceFactory.mount(Path.valueOf("/purchased"), grantedFs);

        return filespaceFactory.newInstance();
    }

    private RequestTarget toJsonResponse(Object object) {
        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to encode object", ex);
        }

        return jsonTarget(bytes);
    }

    private String resolveOrgProduct(RequestCycle cycle, long orgId, OrgProduct orgProd) {

        if (orgProd.getFolderId().equals(FolderId.forRoot())) {
            return '/' + OrgFolderFilesystem.PRODUCT_PREFIX + orgProd.getOrgProductId();
        }

        OrgMaterialFolderClient omfClient = CacheClients.getClient(cycle,
                OrgMaterialFolderClient.class);

        List<OrgMaterialFolder> folders = omfClient.listFolders(orgId);
        Map<FolderId, OrgMaterialFolder> folderMap
                = CollectionsUtil.createMap(folders, f -> f.getId());

        List<String> segments = new ArrayList<>();
        segments.add(OrgFolderFilesystem.PRODUCT_PREFIX + orgProd.getOrgProductId());

        for (FolderId id = orgProd.getFolderId(); !id.equals(FolderId.forRoot());) {
            OrgMaterialFolder parent = folderMap.get(id);
            segments.add(parent.getName());

            id = parent.getParentId();
        }

        Collections.reverse(segments);

        return '/' + StringUtils.join(segments, '/');
    }

}
