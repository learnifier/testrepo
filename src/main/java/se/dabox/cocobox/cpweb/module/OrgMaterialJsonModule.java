/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import com.fasterxml.jackson.core.JsonGenerator;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import net.unixdeveloper.druwa.request.StringRequestTarget;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.ProjectModule;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.converter.DateConverter;
import se.dabox.cocosite.converter.cds.CdsUtil;
import se.dabox.cocosite.date.DatePickerDateConverter;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.pdweb.PdwebProductEditorUrlFactory;
import se.dabox.cocosite.product.GetProjectCompatibleProducts;
import se.dabox.dws.client.langservice.LangBundle;
import se.dabox.dws.client.langservice.LangService;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.InvalidTargetException;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.OrgProductClient;
import se.dabox.service.common.ccbc.folder.FolderId;
import se.dabox.service.common.ccbc.folder.OrgMaterialFolder;
import se.dabox.service.common.ccbc.folder.OrgMaterialFolderClient;
import se.dabox.service.common.ccbc.material.MaterialLink;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.ccbc.material.OrgMaterialConverter;
import se.dabox.service.common.ccbc.material.OrgMaterialLink;
import se.dabox.service.common.ccbc.material.UpdateOrgMaterialLink;
import se.dabox.service.common.ccbc.org.AddOrgProductLinkRequest;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductLink;
import se.dabox.service.common.ccbc.org.OrgProductTransformers;
import se.dabox.service.common.ccbc.org.UpdateOrgProductLinkRequest;
import se.dabox.service.common.ccbc.product.ProductInUseException;
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectSubtypeConstants;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.filter.FilterProjectRequest;
import se.dabox.service.common.ccbc.project.filter.FilterProjectRequestBuilder;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.common.folder.FolderName;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.material.MaterialUtils;
import se.dabox.service.common.proddir.CocoboxProductTypeConstants;
import se.dabox.service.common.proddir.CocoboxProductUtil;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductTypeUtil;
import se.dabox.service.common.proddir.filter.DeeplinkProductFilter;
import se.dabox.service.common.proddir.material.ProductMaterial;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;
import se.dabox.service.common.proddir.material.StandardThumbnailGeneratorFactory;
import se.dabox.service.common.proddir.material.ThumbnailGeneratorFactory;
import se.dabox.service.proddir.data.FieldValue;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.proddir.data.ProductPredicates;
import se.dabox.service.proddir.data.ProductTypeId;
import se.dabox.service.tokenmanager.client.AccountBalance;
import se.dabox.service.tokenmanager.client.AccountBalanceTransformers;
import se.dabox.service.tokenmanager.client.TokenManagerClient;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;
import se.dabox.util.collections.NotPredicate;
import se.dabox.util.collections.OrListPredicate;
import se.dabox.util.collections.Predicate;
import se.dabox.util.collections.ValueUtils;
import se.dabox.util.converter.ConversionContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/orgmats.json")
public class OrgMaterialJsonModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(OrgMaterialJsonModule.class);

    private static final AccountBalance FAKE_BALANCE = new AccountBalance(-1, 0, 0, 0);

    /**
     * Produces a JSON response for the table component containing orgMaterial information.
     *
     *
     * @param cycle param strOrgId
     * @param strOrgId
     *
     * @return
     *
     * @throws Exception
     */
    @WebAction
    public RequestTarget onListOrgMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<OrgMaterial> materials =
                getCocoboxCordinatorClient(cycle).listOrgMaterial(orgId);

        sortOrgMats(cycle, materials);

        ByteArrayOutputStream baos = toJsonObject(cycle, materials);

        return jsonTarget(baos);
    }

    /**
     * Returns all orgmats that can be deeplinked. This includes anonymous products which
     * are added to client level (new-style orgmats).
     *
     *
     * @param cycle param strOrgId
     * @param strOrgId
     *
     * @return
     *
     * @throws Exception
     */
    @WebAction
    public RequestTarget onListDeeplinkOrgMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<OrgMaterial> materials =
                getCocoboxCordinatorClient(cycle).listOrgMaterial(orgId);

        List<OrgProduct> orgProds =
                getCocoboxCordinatorClient(cycle).listOrgProducts(orgId);

        List<Product> products = getOrgMatProducts(cycle, orgProds);


        Locale sortLocale = CocositeUserHelper.getUserLocale(cycle);
        MaterialListFactory mlf = new MaterialListFactory(cycle, sortLocale);
        mlf.addOrgMaterials(materials);
        mlf.addProducts(products);

        ByteArrayOutputStream baos = new DeeplinkMaterialJsonEncoder(cycle, orgId).encode(mlf.getList());

        return jsonTarget(baos);
    }

    @WebAction
    public RequestTarget onSearchOrgMats(RequestCycle cycle, String strOrgId, String term)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        final long userId = LoginUserAccountHelper.getUserId(cycle);

        List<OrgMaterial> materials = Collections.emptyList();

        if (hasOrgPermission(cycle, userId, CocoboxPermissions.CP_LIST_ORGMATS)) {
            materials = getCocoboxCordinatorClient(cycle).searchOrgMaterial(userId, term,
                    Collections.
                    singletonList(orgId));
        }

        sortOrgMats(cycle, materials);

        ByteArrayOutputStream baos = toJsonObject(cycle, materials);

        return jsonTarget(baos);
    }

    @WebAction
    public RequestTarget onListMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<Material> materials = getOrgMaterials(cycle, orgId, null, null);

        final List<OrgMaterialFolder> folders = getOrgMaterialFolderClient(cycle).listFolders(orgId);

        final Map<String, OrgProduct> orgProdMap = CollectionsUtil.createMap(getCocoboxCordinatorClient(cycle).listOrgProducts(orgId), OrgProduct::getProdId);

        boolean allowDelete = hasOrgPermission(cycle, orgId, CocoboxPermissions.CP_DELETE_ORGMAT);

        return jsonTarget(toJsonMaterials(cycle, null, materials, orgProdMap, folders, allowDelete));
    }

    @WebAction
    public RequestTarget onListMatListMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<Material> materials = getOrgMaterials(cycle, orgId, null, ProjectType.MATERIAL_LIST_PROJECT);

        final Map<String, OrgProduct> orgProdMap = CollectionsUtil.createMap(getCocoboxCordinatorClient(cycle).listOrgProducts(orgId), OrgProduct::getProdId);

        final List<OrgMaterialFolder> folders = getOrgMaterialFolderClient(cycle).listFolders(orgId);

        return jsonTarget(toJsonMaterials(cycle, null, materials, orgProdMap, folders, false));
    }

    @WebAction
    public RequestTarget onListPurchasedMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<OrgProduct> orgProds =
                getCocoboxCordinatorClient(cycle).listOrgProducts(orgId);

        List<Product> products = getGrantedProducts(cycle, orgProds);
        List<Product> filteredProducts = filterProducts(cycle, products);

        return processProducts(cycle, filteredProducts, orgId, orgProds, strOrgId);
    }

    @WebAction
    public RequestTarget onListProductProjects(RequestCycle cycle) {
        String productId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(),
                "productId");

        Product product = getProductDirectoryClient(cycle).getProduct(productId);

        Map<String,Object> map = new Flat3Map<>();

        if (product == null) {
            map.put("status", "notfound");
        } else if (!product.isAnonymous()) {
            map.put("status", "denied");
        } else {
            map.put("status", "ok");

            List<OrgProject> projects
                    = getCocoboxCordinatorClient(cycle).listOrgProjectsUsingProduct(new ProductId(
                    productId));

            map.put("projects", CollectionsUtil.transformList(projects, this::toProjectIdAndName));
        }

        return jsonTarget(map);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDeleteOrgProduct(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_DELETE_ORGMAT);

        String productId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(),
                "productId");

        Product product = getProductDirectoryClient(cycle).getProduct(productId);

        Map<String,Object> map = new Flat3Map<>();

        if (product == null) {
            map.put("status", "notfound");
        } else if (!product.isAnonymous()) {
            map.put("status", "denied");
        } else {
            long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
            try {
                getCocoboxCordinatorClient(cycle).
                        deleteOrgProduct(caller, Long.valueOf(strOrgId), productId);
                map.put("status", "ok");
            } catch (ProductInUseException ex) {
                map.put("status", "inuse");
            }
        }

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onSearchPurchasedMats(RequestCycle cycle, String strOrgId, String term)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<OrgProduct> orgProds = Collections.emptyList();

        if (hasOrgPermission(cycle, orgId, CocoboxPermissions.CP_LIST_ORGMATS)) {
                orgProds = getCocoboxCordinatorClient(cycle).listOrgProducts(orgId);
        }

        final long userId = LoginUserAccountHelper.getUserId(cycle);
        final String basetype = getConfValue(cycle, CocoSiteConstants.PRODUCT_BASETYPE);
        List<Product> matchingProducts = getProductDirectoryClient(cycle).searchProducts(userId,
                term, basetype);

        List<Product> products = filterGrantedProducts(matchingProducts, orgProds);

        return processProducts(cycle, products, orgId, orgProds, strOrgId);
    }

    private static List<Product> getGrantedProducts(RequestCycle cycle, List<OrgProduct> orgProds) {
        List<String> ids = CollectionsUtil.transformList(orgProds, OrgProductTransformers.
                getProductIdTransformer());
        List<Product> products = getProducts(cycle, ids);
        return products;
    }

    @WebAction
    public RequestTarget onNewOrgMatLink(RequestCycle cycle) {
        //TODO: Security check here
        long materialId = Long.valueOf(cycle.getRequest().getParameter("orgmatid"));

        final CocoboxCoordinatorClient client = getCocoboxCordinatorClient(cycle);

        long userId = getCurrentUser(cycle);

        long linkId = client.addOrgMaterialLink(userId, materialId);
        //TODO: Fetch all, is this really the best way to do it?
        OrgMaterialLink link = getLink(client, materialId, linkId);

        String deliveryBase = getDeliveryBase(cycle);
        return jsonTarget(toJsonLinks(cycle, Collections.singletonList(link), deliveryBase));
    }

    @WebAction
    public RequestTarget onDeleteOrgMatLink(RequestCycle cycle) {
        //TODO: Security check here
        long orgmatlinkid = Long.valueOf(cycle.getRequest().getParameter("orgmatlinkid"));

        final CocoboxCoordinatorClient client = getCocoboxCordinatorClient(cycle);

        client.deleteOrgMaterialLink(LoginUserAccountHelper.getUserId(cycle), orgmatlinkid);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onDeleteOrgMat(RequestCycle cycle) {
        long orgmatid = Long.valueOf(cycle.getRequest().getParameter("orgmatid"));

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        OrgMaterial orgMat;
        try {
            orgMat = ccbc.getOrgMaterial(orgmatid);
        } catch (NotFoundException nfe){
            LOGGER.info("Tried to delete an orgmat that doesn't exist: {}", orgmatid);

            return createDeleteMaterialResponse(orgmatid, 0, Collections.<String>emptyList(), true);
        }
        checkOrgPermission(cycle, orgMat.getOrgId(), CocoboxPermissions.CP_DELETE_ORGMAT);

        int activeLinks = getActiveLinks(ccbc, orgMat);
        List<String> projectNames = getLinkedProjectNames(ccbc, orgMat);

        boolean canDelete = activeLinks == 0 && projectNames.isEmpty();


        if (canDelete) {
            ccbc.deleteOrgMaterial(orgmatid, LoginUserAccountHelper.getUserId(cycle));
        }

        return createDeleteMaterialResponse(orgmatid, activeLinks, projectNames, canDelete);
    }

    private RequestTarget createDeleteMaterialResponse(long orgmatid, int activeLinks,
            List<String> projectNames, boolean canDelete) {
        Map<String, Object> map = createMap();

        map.put("orgmatid", orgmatid);
        map.put("activeLinkCount", activeLinks);
        map.put("linkedProjectNames", projectNames);

        map.put("status", canDelete ? "OK" : "DENIED");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onListOrgMatLinks(RequestCycle cycle, String strOrgId) {

        MiniOrgInfo orgUnit = secureGetMiniOrg(cycle, strOrgId);

        String deliveryBase = getDeliveryBase(cycle);

        String materialId = cycle.getRequest().getParameter("orgmatid");
        String[] matId = MaterialUtils.splitCompositeId(materialId);

        List<? extends MaterialLink> links;
        if (matId[0].equals(OrgMaterialConstants.NATIVE_SYSTEM)) {
            long orgMatId = Long.parseLong(matId[1]);

            links = getOrgLinksOrCreateLink(cycle, orgMatId);

        } else if (matId[0].equals(ProductMaterialConstants.NATIVE_SYSTEM)) {
            String productId = matId[1];

            links = getProductLinksOrCreateLink(cycle, productId, orgUnit);
        } else {
            throw new IllegalStateException("Unknown material type: "+materialId);
        }

        ByteArrayOutputStream baos = toJsonLinks(cycle, links, deliveryBase);

        return jsonTarget(baos);
    }

    public static String getDeliveryBase(RequestCycle cycle) {
        return CdsUtil.getDeliveryBase(cycle);
    }

    public static ByteArrayOutputStream toJsonLinks(RequestCycle cycle,
            final List<? extends MaterialLink> links,
            final String deliveryBase) {

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, userLocale);

        ByteArrayOutputStream baos =
                new JsonEncoding(df) {
                    @Override
                    protected void encodeData(JsonGenerator generator) throws IOException {
                        generator.writeStartObject();
                        generator.writeArrayFieldStart("aaData");

                        for (MaterialLink link : links) {
                            generator.writeStartObject();

                            if (link instanceof OrgMaterialLink) {
                                OrgMaterialLink oml = (OrgMaterialLink) link;
                                generator.writeStringField("linkid", "O-"+oml.getId());
                            } else if (link instanceof OrgProductLink) {
                                OrgProductLink opl = (OrgProductLink) link;
                                generator.writeStringField("linkid", "P-"+opl.getLinkId());
                            } else {
                                throw new IllegalStateException("Unknown link type: "+link.getClass().getName());
                            }
                            generator.writeBooleanField("active", link.isActive());
                            generator.writeStringField("deeplink",
                                    deliveryBase + link.getCdLinkId());
                            generator.writeStringField("activeto", formatDate(link.
                                    getActiveTo()));

                            if (link.getActiveTo() != null) {
                                String activeToStr = df.format(link.getActiveTo());
                                generator.writeStringField("activetoStr", activeToStr);
                            } else {
                                generator.writeNullField("activetoStr");
                            }

                            generator.writeEndObject();

                        }

                        generator.writeEndArray();
                        generator.writeEndObject();
                    }
                }.encodeToStream();
        return baos;
    }

    @WebAction
    public RequestTarget onChangeLinkStatus(RequestCycle cycle, String strOrgId) {

        MiniOrgInfo orgUnit = secureGetMiniOrg(cycle, strOrgId);

        String strLinkId = cycle.getRequest().getParameter("linkid");
        String[] split = strLinkId.split("-");

        Long linkid = Long.valueOf(split[1]);
        boolean active = Boolean.valueOf(cycle.getRequest().getParameter("active"));

        switch (split[0]) {
            case "O":
                {
                    UpdateOrgMaterialLink update = new UpdateOrgMaterialLink(linkid, getCurrentUser(cycle));
                    update.setActive(active);
                    getCocoboxCordinatorClient(cycle).updateOrgMaterialLink(update);
                    break;
                }
            case "P":
                {
                    long userId = getCurrentUser(cycle);
                    UpdateOrgProductLinkRequest update = new UpdateOrgProductLinkRequest(userId, linkid);
                    update.setActive(active);
                    getCocoboxCordinatorClient(cycle).updateOrgProductLink(update);
                    break;
                }
            default:
                throw new IllegalStateException("Invalid link id: "+strLinkId);
        }

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onChangeLinkActiveTo(RequestCycle cycle, String strOrgId) {

        secureGetMiniOrg(cycle, strOrgId);


        String strLinkId = cycle.getRequest().getParameter("pk");
        String[] split = strLinkId.split("-");
        Long linkid = Long.valueOf(split[1]);

        Date activeTo = getActiveTo(cycle);

        if (activeTo == null) {
            cycle.getResponse().setStatus(400);
            return new StringRequestTarget("Date not specified");
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(activeTo);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);

        switch (split[0]) {
            case "O": {
                UpdateOrgMaterialLink update = new UpdateOrgMaterialLink(linkid,
                        getCurrentUser(cycle));
                update.setActiveTo(cal.getTime());
                getCocoboxCordinatorClient(cycle).updateOrgMaterialLink(update);
            }
            break;
            case "P": {
                UpdateOrgProductLinkRequest update = new UpdateOrgProductLinkRequest(getCurrentUser(cycle),
                        linkid);
                update.setActiveTo(cal.getTime());
                getCocoboxCordinatorClient(cycle).updateOrgProductLink(update);
            }
            break;
            default:
                throw new IllegalStateException("Unknown linkid: " + strLinkId);
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, userLocale);

        return new StringRequestTarget(df.format(activeTo));
    }

    private static void sortOrgMats(RequestCycle cycle, List<OrgMaterial> materials) {
        final Collator collator = Collator.getInstance(
                CocositeUserHelper.getUserLocale(cycle));
        collator.setStrength(Collator.SECONDARY);

        Collections.sort(materials, (OrgMaterial o1, OrgMaterial o2) -> {
            int diff = collator.compare(o1.getTitle(), o2.getTitle());
            if (diff != 0) {
                return diff;
            }

            if (o1.getOrgMaterialId() < o2.getOrgMaterialId()) {
                return -1;
            }
            return 1;
        });
    }

    public static ByteArrayOutputStream toJsonObject(final RequestCycle cycle,
            final List<OrgMaterial> materials) {

        ByteArrayOutputStream baos = new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();

                generator.writeArrayFieldStart("aaData");

                String orgId = null;
                if (!materials.isEmpty()) {
                    orgId = Long.toString(materials.get(0).getOrgId());
                }

                OrgMaterialConverter converter = new OrgMaterialConverter(cycle);

                for (OrgMaterial orgMat : materials) {
                    generator.writeStartObject();

                    final Material material = converter.convert(orgMat);

                    generator.writeStringField("materialId", material.getCompositeId());
                    generator.writeNumberField("id", orgMat.getOrgMaterialId());
                    generator.writeStringField("title", orgMat.getTitle());
                    generator.writeStringField("type", orgMat.getType());
                    generator.writeStringField("desc", orgMat.getDescription());
                    generator.writeStringField("crlink", orgMat.getCrlink());
                    generator.writeStringField("weblink", orgMat.getWeblink());
                    generator.writeNumberField("createdBy", orgMat.getCreatedBy());
                    generator.writeNumberField("created", orgMat.getCreatedBy());
                    writeLongNullField(generator, "updatedBy",
                            orgMat.getUpdatedBy());
                    generator.writeNumberField("updated", orgMat.getUpdatedBy());
                    generator.writeStringField("viewLink",
                            cycle.urlFor(CpMainModule.class.getName(),
                            "viewOrgMaterial",
                            orgId, Long.toString(orgMat.getOrgMaterialId())));
                    generator.writeNumberField("activeLinks", orgMat.getActiveLinks());
                    generator.writeNumberField("inactiveLinks", orgMat.getInactiveLinks());
                    generator.writeStringField("thumbnail", material.getThumbnail(64));
                    generator.writeEndObject();
                }

                generator.writeEndArray();

                generator.writeEndObject();
            }
        }.encodeToStream();

        return baos;
    }

    private ByteArrayOutputStream toJsonObjectProducts(final RequestCycle cycle, final String orgId,
            final List<Product> products, final Map<String, AccountBalance> balances,
            final Map<String, Long> orgProdIdMap,
            final Set<Product> deeplinkSet) {

        final String cdnUrl = getConfValue(cycle, "contentrepo.cocoboxpub.puburl");

        final ThumbnailGeneratorFactory factory = new StandardThumbnailGeneratorFactory(cdnUrl);

        final Map<Long, List<OrgProductLink>> productLinkMap = getProductLinkMap(cycle,
                orgProdIdMap.values());

        final List<Product> sortedProducts = sortProducts(cycle, products);

        final LangBundle bundle = getLangBundle(cycle);


        return new DataTablesJson<Product>() {
            @Override
            protected void encodeItem(Product product) throws IOException {
                long opid = orgProdIdMap.get(product.getId().getId());
                generator.writeNumberField("opid", opid);
                generator.writeStringField("id", product.getId().getId());
                generator.writeStringField("title", getSingleValue(product, "webtitle"));
                generator.writeStringField("type", product.getProductTypeId().getId());
                generator.writeStringField("typeTitle", getProductTypeTitle(bundle, product.getProductTypeId()));
                generator.writeStringField("desc", getSingleValue(product, "webdescription"));
                generator.writeStringField("crlink", getSingleValue(product, "crurl"));

                generator.writeStringField("thumbnail", factory.createThumbnailGenerator(null,
                        product).getThumbnail(64));
                generator.writeStringField("thumbnailx256", factory.createThumbnailGenerator(null,
                        product).getThumbnail(256));

                generator.writeStringField("weblink", "");
                generator.writeNumberField("createdBy", 0);
                generator.writeStringField("viewLink",
                        cycle.urlFor(CpMainModule.class.getName(),
                        "viewOrgMaterial",
                        orgId, product.getId().getId()));

                List<OrgProductLink> links = productLinkMap.get(orgProdIdMap.get(product.getId().
                        getId()));
                if (links == null) {
                    links = Collections.emptyList();
                }
                int active = getActiveCount(links);
                int inactiveLinks = links.size() - active;
                generator.writeNumberField("activeLinks", active);
                generator.writeNumberField("inactiveLinks", inactiveLinks);

                generator.writeNumberField("linkCredits", calculateLinkCredits(links));

                AccountBalance balance = balances.get(product.getId().getId());
                if (balance == null) {
                    balance = FAKE_BALANCE;
                }
                generator.writeNumberField("availCredits", balance.getAvailable());
                generator.writeNumberField("expiredCredits", balance.getExpired());
                generator.writeNumberField("usedCredits", balance.getUsed());
                //TODO: JK: Verify correct with reserved?
                generator.writeNumberField("totalCredits", balance.getAvailable() + balance.
                        getExpired() + balance.getUsed());
                generator.writeBooleanField("allowDeeplink", deeplinkSet.contains(product));

                generator.writeBooleanField("anonymous", product.isAnonymous());
                generator.writeBooleanField("orgUnitProduct", product.isOrgUnitProduct());
                generator.writeBooleanField("projectProduct", product.isProjectProduct());
                generator.writeBooleanField("realmProduct", product.isRealmProduct());
            }

            private String getSingleValue(Product material, String name) {
                FieldValue<String> field = material.getFields().get(name);
                if (field == null) {

                    if (!material.getDimensions().isEmpty()) {
                        FieldValue<String> dimValue =
                                material.getDimensions().get(0).getFields().get(name);

                        if (dimValue != null) {
                            return dimValue.getSingleValue();
                        }
                    }

                    return "";
                }

                return field.getSingleValue();
            }

        }.encodeToStream(sortedProducts);
    }

    private static String getProductTypeTitle(LangBundle bundle, ProductTypeId productTypeId) {
        String productType = productTypeId.getId();

        String name = productType;

        String keyName = "pdweb.product.type." + productType;

        if (bundle.hasKey(keyName)) {
            name = bundle.getKey(keyName);
        }

        return name;
    }

    private static String getMaterialTypeTitle(LangBundle bundle, Material material) {

        if (OrgMaterialConstants.NATIVE_SYSTEM.equals(material.getNativeSystem())) {
            return bundle.getKey("cpweb.materialtype.orgmat");
        } else if (material instanceof ProductMaterial) {
            ProductMaterial prodMat = (ProductMaterial) material;
            return getProductTypeTitle(bundle, prodMat.getProduct().getProductTypeId());
        }

        return material.getNativeType();
    }

    private static LangBundle getLangBundle(RequestCycle cycle) {
                LangService lsClient = CacheClients.getClient(cycle, LangService.class);

                Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

                LangBundle bundle
                        = lsClient.getLangBundle(CocoSiteConstants.DEFAULT_LANG_BUNDLE, userLocale.
                                toString(), true);

                return bundle;
            }

    /**
     * Creates a material list json response. This method is used by other classes.
     *
     * @param cycle
     * @param strProjectId
     * @param materials
     * @return
     */
    public static ByteArrayOutputStream toJsonMaterials(final RequestCycle cycle,
            final String strProjectId,
            final List<Material> materials,
            final Map<String, OrgProduct> orgProds,
            List<OrgMaterialFolder> folders,
            Boolean allowDelete) {

        final boolean deleteFlag = determineDeleteFlag(allowDelete);

        final long projectId = toProjectIdLong(strProjectId);

        final LangBundle bundle = getLangBundle(cycle);



//        List<MatFolder> folders = createFolders();

//        List<Long> folderIds = new ArrayList<>(folders.size()+1);
//        folderIds.add(null);
//        for (MatFolder folder : folders) {
//            addFolderIds(folderIds, folder);
//        }

        PdwebProductEditorUrlFactory pdwebEditorFactory = new PdwebProductEditorUrlFactory(cycle);

        return new DataTablesJson<Material>() {
            @Override
            protected void writeExtraDataEnd() throws IOException {
                super.writeExtraDataEnd();

                generator.writeArrayFieldStart("folders");
                for (OrgMaterialFolder folder : folders) {
                    writeFolder(folder);
                }
                generator.writeEndArray();
            }

            @Override
            protected void encodeItem(Material material) throws IOException {
                generator.writeStringField("materialId", material.getCompositeId());
                generator.writeStringField("id", material.getId());
                generator.writeStringField("title", material.getTitle());
                generator.writeStringField("system", material.getNativeSystem());
                generator.writeStringField("type", material.getNativeType());
                generator.writeStringField("typeTitle", getMaterialTypeTitle(bundle, material));
                generator.writeStringField("desc", material.getDescription());
                generator.writeStringField("locale", material.getLocale().toString());
                generator.writeStringField("thumbnail", material.getThumbnail(64));
                generator.writeStringField("thumbnailx256", material.getThumbnail(256));
                CrispAdminLinkInfo crispAdminLink = getCrispAdminLink(material);
                generator.writeBooleanField("crispAdminAvailable", crispAdminLink != null);
                if (crispAdminLink != null) {
                    generator.writeStringField("crispAdminLink", crispAdminLink.link);
                    generator.writeStringField("crispName", crispAdminLink.name);
                }
                if (material instanceof ProductMaterial) {
                    ProductMaterial prodMat = (ProductMaterial) material;
                    final Product product = prodMat.getProduct();
                    generator.writeBooleanField("anonymous", product.isAnonymous());
                    generator.writeBooleanField("projectProduct", product.isProjectProduct());
                    generator.writeBooleanField("ouProduct", product.isOrgUnitProduct());
                    generator.writeBooleanField("realmProduct", product.isRealmProduct());
                    if (CocoboxProductUtil.hasPdwebEditor(product)) {
                        String editorUrl = pdwebEditorFactory.forProduct(product);
                        generator.writeStringField("editorUrl", editorUrl);
                    }
                    generator.writeBooleanField("crispConfigAvailable", hasCrispConfig(cycle, product));
                    generator.writeBooleanField("allowDelete", deleteFlag);

                    String adminLink = getAdminLink(cycle, projectId, product);
                    if (adminLink != null) {
                        generator.writeStringField("adminLink", adminLink);
                        generator.writeStringField("adminLinkTitle", "Administrate");
                    }
                    if(product.getUpdated() != null && product.getUpdated().getTimestamp() != null) {
                        writeDateField("updated", Date.from(product.getUpdated().getTimestamp()));
                    }

                    final ProductId prodId = product.getId();
                    final OrgProduct orgProduct = orgProds.get(prodId.getId());
                    if (orgProduct == null || orgProduct.getFolderId().getId() == null) {
                        generator.writeNullField("materialFolderId");
                    } else {
                        generator.writeNumberField("materialFolderId", orgProduct.getFolderId().getId());
                    }
                } else {
                    generator.writeNullField("materialFolderId");
                }
            }

            private CrispAdminLinkInfo getCrispAdminLink(Material material) {
                if (strProjectId == null) {
                    return null;
                }

                if (!ProductMaterialConstants.NATIVE_SYSTEM.equals(material.getNativeSystem())) {
                    return null;
                }

                String productId = material.getId();
                Product product = getProductDirectoryClient(cycle).getProduct(productId);

                if (product == null) {
                    return null;
                }

                CrispContext ctx =
                        DwsCrispContextHelper.getCrispContext(cycle, product);

                if (ctx == null) {
                    return null;
                }

                if (ctx.getDescription().getMethods().getGetProjectAdminUrl() == null) {
                    return null;
                }

                String link = cycle.urlFor(ProjectModule.class, "crispAdmin", strProjectId,
                        material.
                        getId());

                return new CrispAdminLinkInfo(link, ctx.getDescription().getInfo().getName());
            }

            private boolean hasCrispConfig(RequestCycle cycle, Product product) {
                CrispContext ctx = DwsCrispContextHelper.getCrispContext(cycle, product);

                return ctx != null &&
                        ctx.getDescription().getMethods().getGetProjectConfiguration() != null;
            }

            private String getAdminLink(RequestCycle cycle, long parentProjectId, Product product) {
                if (parentProjectId <= 0) {
                    return null;
                }

                if (CocoboxProductTypeConstants.LINKEDSUBPROJECT.equals(product.getProductTypeId())) {
                    FilterProjectRequest fpr = new FilterProjectRequestBuilder().setMasterProject(
                            parentProjectId).setProductId(product.getId().getId()).buildRequest();

                    List<OrgProject> projects
                            = getCocoboxCordinatorClient(cycle).listOrgProjects(fpr);

                    OrgProject project = CollectionsUtil.singleItemOrNull(projects);

                    if (project != null) {
                        return NavigationUtil.toProjectPageUrl(cycle, project.getProjectId());
                    }
                }

                return null;
            }

            private int hash(String key) {
                int h;
                return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
            }

            private void writeFolder(OrgMaterialFolder folder) throws IOException {
                generator.writeStartObject();
                if (folder.getId().getId() == null) {
                    generator.writeNullField("id");
                } else {
                    generator.writeNumberField("id", folder.getId().getId());
                }

                if (folder.getParentId().getId() == null) {
                    generator.writeNullField("parentId");
                } else {
                    generator.writeNumberField("parentId", folder.getParentId().getId());
                }

                generator.writeStringField("name", folder.getName());

                generator.writeEndObject();
            }

        }.encodeToStream(materials);
    }

    private static long toProjectIdLong(final String strProjectId) throws NumberFormatException {
        long projectId = -1;
        if (strProjectId != null) {
            projectId = Long.valueOf(strProjectId);
        }
        return projectId;
    }

    private static String formatDate(Date activeTo) {
        return DatePickerDateConverter.toDatePickerDate(activeTo);
    }

    private List<OrgMaterialLink> getOrgLinksOrCreateLink(RequestCycle cycle, long materialId) {
        final CocoboxCoordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient(cycle);
        List<OrgMaterialLink> links =
                cocoboxCordinatorClient.listOrgMaterialLinks(
                materialId);

        if (!links.isEmpty()) {
            return links;
        }

        cocoboxCordinatorClient.addOrgMaterialLink(getCurrentUser(cycle), materialId);

        return cocoboxCordinatorClient.listOrgMaterialLinks(materialId);
    }

    private List<OrgProductLink> getProductLinksOrCreateLink(RequestCycle cycle, String productId, MiniOrgInfo orgUnit) {
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        long orgProdId = getOrgProdId(ccbc, orgUnit, productId);

        List<OrgProductLink> links = ccbc.getOrgProductLinks(orgProdId);

        if (links.isEmpty()) {
            long userId = LoginUserAccountHelper.getCurrentCaller(cycle);
            ccbc.addOrgProductLink(new AddOrgProductLinkRequest(userId, orgProdId));
            links = ccbc.getOrgProductLinks(orgProdId);
        }

        return links;
    }

    private OrgMaterialLink getLink(CocoboxCoordinatorClient client, long materialId, long linkId) {
        List<OrgMaterialLink> links = client.listOrgMaterialLinks(materialId);

        for (OrgMaterialLink link : links) {
            if (link.getId() == linkId) {
                return link;
            }
        }

        return null;
    }

    public static Date getActiveTo(RequestCycle cycle) {
        String strActiveTo = cycle.getRequest().getParameter("activeTo");
        if (strActiveTo == null) {
            strActiveTo = cycle.getRequest().getParameter("value");
            if (strActiveTo == null) {
                return null;
            }
        }

        ConversionContext cc = new ConversionContext(CocositeUserHelper.getUserLocale(cycle));
        return DateConverter.getIso8601Converter().convert(cc, strActiveTo, Date.class);
    }

    private static List<Product> getProducts(RequestCycle cycle,
            List<String> productIds) {

        ProductDirectoryClient pdClient =
                getProductDirectoryClient(cycle);

        return pdClient.getProducts(true, productIds);
    }

    private int getActiveLinks(CocoboxCoordinatorClient ccbc, OrgMaterial orgMat) {
        return orgMat.getActiveLinks();
    }

    private List<String> getLinkedProjectNames(CocoboxCoordinatorClient ccbc, OrgMaterial orgMat) {
        List<OrgProject> projects = ccbc.listOrgProjectsUsingOrgMat(orgMat.getOrgMaterialId());

        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        final GetProjectAdministrativeName nameHelper = new GetProjectAdministrativeName(cycle);

        return CollectionsUtil.transformList(projects, nameHelper::getName);
    }

    private Map<String, AccountBalance> getAccountBalanceMap(RequestCycle cycle, long orgId,
            List<OrgProduct> prods) {
        TokenManagerClient tmClient =
                Clients.getClient(cycle, TokenManagerClient.class);

        List<Long> accountIds =
                CollectionsUtil.transformList(prods, OrgProductTransformers.
                getTokenAccountTransformer());

        List<AccountBalance> accountBalances = tmClient.getAccountBalances(accountIds);

        Map<Long, AccountBalance> balanceMap = CollectionsUtil.createMap(accountBalances,
                AccountBalanceTransformers.toAccountId());

        Map<String, AccountBalance> prodBalance = new HashMap<>();
        for (OrgProduct orgProduct : prods) {
            String prodId = orgProduct.getProdId();
            AccountBalance balance =
                    balanceMap.get(orgProduct.getTokenManagerAccountId());
            prodBalance.put(prodId, balance);
        }

        return prodBalance;
    }

    private Map<String, Long> getOrgProdIdMap(List<OrgProduct> orgProds) {
        Map<String, Long> map = new HashMap<>();

        for (OrgProduct orgProduct : orgProds) {
            map.put(orgProduct.getProdId(), orgProduct.getOrgProductId());
        }

        return map;
    }

    private static int getActiveCount(List<OrgProductLink> links) {
        return CollectionsUtil.countMatching(links, OrgProductLink::isActive);
    }

    private static long calculateLinkCredits(List<OrgProductLink> links) {
        long credits = 0;

        for (OrgProductLink orgProductLink : links) {
            credits += orgProductLink.getBalance();
        }

        return credits;
    }

    private RequestTarget processProducts(RequestCycle cycle,
            List<Product> products, long orgId,
            List<OrgProduct> orgProds, String strOrgId) {
        ProductDirectoryClient pdClient = getProductDirectoryClient(cycle);
        ProductTypeUtil.setTypes(pdClient, products);

        Set<Product> deeplinkSet = CollectionsUtil.subset(products, DeeplinkProductFilter.
                getInstance());

        Map<String, AccountBalance> balances = Collections.emptyMap();

        if (hasOrgPermission(cycle, orgId, CocoboxPermissions.CP_VIEW_ACCOUNTBALANCE)) {
            balances = getAccountBalanceMap(cycle, orgId, orgProds);
            products = maybeFilterZeroBalanceProducts(cycle, balances, products);
        }
        Map<String, Long> orgProdIdMap = getOrgProdIdMap(orgProds);

        ByteArrayOutputStream baos = toJsonObjectProducts(cycle, strOrgId, products, balances,
                orgProdIdMap, deeplinkSet);

        return jsonTarget(baos);
    }

    private List<Product> filterGrantedProducts(List<Product> matchingProducts,
            List<OrgProduct> orgProds) {
        final Set<String> grantedIds = CollectionsUtil.transform(orgProds, OrgProductTransformers.
                getProductIdTransformer());

        return CollectionsUtil.sublist(matchingProducts, (Product item) ->
                grantedIds.contains(item.getId().getId()));
    }

    private Map<Long, List<OrgProductLink>> getProductLinkMap(RequestCycle cycle,
            Collection<Long> orgProductIds) {

        List<OrgProductLink> links =
                getCocoboxCordinatorClient(cycle).getOrgProductsLinks(orgProductIds);

        return CollectionsUtil.createMapList(links, OrgProductLink::getOrgProductId);

    }

    public static List<Material> getOrgMaterials(final RequestCycle cycle, final long orgId,
            OrgProject project, ProjectType projectType) {

        List<Product> products = getOrgProducts(cycle, orgId, project, projectType);

        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        List<OrgMaterial> orgMats =
                ccbc.listOrgMaterial(orgId);

        MaterialListFactory mlf = new MaterialListFactory(cycle, CocositeUserHelper.getUserLocale(
                cycle));
        mlf.addOrgMaterials(orgMats);
        mlf.addProducts(products);
        List<Material> materials = mlf.getList();
        return materials;
    }

    public static List<Product> getOrgProducts(final RequestCycle cycle, final long orgId,
            OrgProject project, ProjectType projectType) {
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        List<Product> products = getGrantedProducts(cycle, ccbc.listOrgProducts(orgId));
        ProductTypeUtil.setTypes(getProductDirectoryClient(cycle), products);

        if (project == null) {
            //Remove project products
            products = CollectionsUtil.sublist(products, new NotPredicate<>(ProductPredicates.
                    getProjectOwnedProductPredicate()));
        } else {
            products = new GetProjectCompatibleProducts().getValid(project, products);
        }

        if (projectType != null) {
            products = new GetProjectCompatibleProducts().getValid(projectType, products);
        }

        if (project != null) {
            products = filterValidProjectProducts(cycle, project, products);
        }

        return products;
    }

    private static List<Product> filterValidProjectProducts(RequestCycle cycle, OrgProject project,
            List<Product> products) {
        if (!project.getSubtype().equals(ProjectSubtypeConstants.IDPROJECT)) {
            return products;
        }

        return CollectionsUtil.sublist(products, ProductPredicates.getIdProjectValidProduct());
    }

    private List<Product> sortProducts(RequestCycle cycle, List<Product> products) {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        final Collator col = Collator.getInstance(userLocale);
        col.setStrength(Collator.PRIMARY);

        List<Product> sortedList = new ArrayList<>(products);
        Collections.sort(sortedList, (Product o1, Product o2) -> {
            int diff = col.compare(o1.getTitle(), o2.getTitle());

            if (diff == 0) {
                diff = o1.getId().compareTo(o2.getId());
            }

            return diff;
        });

        return sortedList;
    }

    private List<Product> filterProducts(RequestCycle cycle, List<Product> products) {
        Set<String> excludes = getExcludes(cycle);

        List<Predicate<Product>> predicates = new ArrayList<>();

        if (excludes.contains("project")) {
            predicates.add(ProductPredicates.getProjectOwnedProductPredicate());
        }

        if (excludes.contains("orgunit")) {
            predicates.add(ProductPredicates.getOrgUnitOwnedProductPredicate());
        }

        if (predicates.isEmpty()) {
            return products;
        }

        return CollectionsUtil.sublist(products,
                new NotPredicate<>(new OrListPredicate<>(predicates)));
    }

    private Set<String> getExcludes(RequestCycle cycle) {
        String param = cycle.getRequest().getParameter("exclude");
        if (StringUtils.isBlank(param)) {
            return Collections.emptySet();
        }


        String[] splits = StringUtils.split(param, ',');
        Set<String> excludes = new HashSet<>(MapUtil.calculateSizeWithLoad(splits.length));

        for (String split : splits) {
            String trimmed = StringUtils.trimToNull(split);
            if (trimmed != null) {
                excludes.add(trimmed);
            }
        }

        return excludes;
    }

    private long getOrgProdId(CocoboxCoordinatorClient cocoboxCordinatorClient, MiniOrgInfo orgUnit,
            String productId) {
        List<OrgProduct> orgProducts = cocoboxCordinatorClient.listOrgProducts(orgUnit.getId());

        for (OrgProduct orgProduct : orgProducts) {
            if (orgProduct.getProdId().equals(productId)) {
                return orgProduct.getOrgProductId();
            }
        }

        throw new IllegalStateException("Unable to find orgprodid for "+productId+" in "+orgUnit.getId());
    }

    private List<Product> getOrgMatProducts(RequestCycle cycle, List<OrgProduct> orgProds) {
        List<Product> products = getGrantedProducts(cycle, orgProds);

        return CollectionsUtil.sublist(products, p -> p.isOrgUnitProduct());
    }


    private static boolean determineDeleteFlag(Boolean allowDelete) {
        if (allowDelete == null) {
            return false;
        }

        return allowDelete;
    }

    private Map<String,Object> toProjectIdAndName(OrgProject project) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();

        Map<String,Object> map = new Flat3Map<>();
        map.put("projectId", project.getProjectId());
        map.put("name", new GetProjectAdministrativeName(cycle).getName(project));

        return map;
    }

    private List<Product> maybeFilterZeroBalanceProducts(RequestCycle cycle,
            Map<String, AccountBalance> balances, List<Product> products) {

        String strParam = ValueUtils.coalesce(cycle.getRequest().getParameter("zero"), "true");
        boolean includeZeroBalanceProducts = Boolean.valueOf(strParam);

        if (includeZeroBalanceProducts) {
            return filterZeroBalanceProducts(balances, products);
        }

        return products;
    }

    private List<Product> filterZeroBalanceProducts(final Map<String, AccountBalance> balances,
            List<Product> products) {

        return CollectionsUtil.sublist(products, p -> {
            AccountBalance balance = balances.get(p.getId().getId());

            if (balance == null) {
                return false;
            }

            return !balance.isZeroBalance();
        });
    }

    private static class CrispAdminLinkInfo {

        public String link;
        public String name;

        public CrispAdminLinkInfo(String link, String name) {
            this.link = link;
            this.name = name;
        }
    }

    private static List<MatFolder> createFolders() {
        List<MatFolder> root = new ArrayList<>();

        long id = 0;

        MatFolder year = new MatFolder(id++, "years");
        root.add(year);
        year.getFolders().add(new MatFolder(id++, "January"));
        year.getFolders().add(new MatFolder(id++, "Foobruary"));

        MatFolder usr = new MatFolder(id++, "usr");
        root.add(usr);
        usr.getFolders().add(new MatFolder(id++, "bin"));
        usr.getFolders().add(new MatFolder(id++, "doc"));
        usr.getFolders().add(new MatFolder(id++, "lib"));

        MatFolder home = new MatFolder(id++, "home");
        root.add(home);

        home.getFolders().add(new MatFolder(id++, "amohamed"));
        home.getFolders().add(new MatFolder(id++, "jklang"));
        home.getFolders().add(new MatFolder(id++, "mandersson"));
        home.getFolders().add(new MatFolder(id++, "mborg"));

        return root;
    }


    private static void addFolderIds(List<Long> folderIds, MatFolder folder) {
        folderIds.add(folder.getId());

        for (MatFolder matFolder : folder.getFolders()) {
            addFolderIds(folderIds, matFolder);
        }
    }


    // TODO: Adding new operations below, may want to put them into some command class

    @WebAction
    public RequestTarget onCreateFolder(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);
        final long caller = LoginUserAccountHelper.getUserId(cycle);

        String folderIdStr = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "folderId");
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        FolderId folderId;
        if("0".equals(folderIdStr)) { // 0 = root in js land
            folderId = FolderId.forRoot();
        } else {
            folderId = FolderId.valueOf(Long.valueOf(folderIdStr));
        }

        Map<String, Object> map = createMap();
        try {
            getOrgMaterialFolderClient(cycle).mkdir(caller, orgId, folderId, new FolderName(name));
            map.put("status", "ok");
        } catch(NotFoundException e) {
            map.put("status", "error");
            map.put("msg", "Folder not found.");
        } catch(AlreadyExistsException e) {
            map.put("status", "error");
            map.put("msg", "Folder name already exists.");
        }
        return new JsonRequestTarget(JsonUtils.encode(map));
    }

    @WebAction
    public RequestTarget onMoveToFolder(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);
        final long caller = LoginUserAccountHelper.getUserId(cycle);

        final List<String> folderIds = getArray(cycle, "folderIds");
        final List<String> itemIds = getArray(cycle, "itemIds");
        String toFolderIdStr = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "toFolderId");
        FolderId toFolderId = FolderId.valueOf(Long.valueOf(toFolderIdStr));

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        List<Map<String, Object>> folders = new ArrayList<>();
        for(String strFolderId: folderIds) {
            final FolderId folderId = FolderId.valueOf(Long.valueOf(strFolderId));
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", folderId.getId());
            try {
//                getOrgMaterialFolderClient(cycle).move(caller, folderId, toFolderId);
                entry.put("status", "OK");
            } catch (NotFoundException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Not found exception");
            } catch (AlreadyExistsException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Name already exists in target folder");
            } catch (InvalidTargetException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Invalid move");
            }
            folders.add(entry);
        }
        map.put("folders", folders);

        List<Map<String, Object>> items = new ArrayList<>();
        for(String strItemId: itemIds) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", strItemId);
            try {
//                opc.move(caller, id, toFolderId); // TODO: Plug in real service call here.
                entry.put("status", "OK");
            } catch (NotFoundException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Not found exception");
            } catch (AlreadyExistsException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Name already exists in target folder");
            } catch (InvalidTargetException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Invalid move");
            }
            items.add(entry);
        }
        map.put("items", items);

        final String reply = JsonUtils.encode(map);
        return new JsonRequestTarget(reply);
    }

    @WebAction
    public RequestTarget onRemoveFoldersItems(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);
        final long caller = LoginUserAccountHelper.getUserId(cycle);

        final List<String> folderIds = getArray(cycle, "folderIds");
        final List<String> itemIds = getArray(cycle, "itemIds");

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        List<Map<String, Object>> folders = new ArrayList<>();
        for(String strFolderId: folderIds) {
            final FolderId folderId = FolderId.valueOf(Long.valueOf(strFolderId));
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", folderId.getId());
            try {
                getOrgMaterialFolderClient(cycle).rmdir(caller, folderId);
                entry.put("status", "OK");
            } catch (NotFoundException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Not found exception");
            }
            folders.add(entry);
        }
        map.put("folders", folders);

        List<Map<String, Object>> items = new ArrayList<>();
        for(String strItemId: itemIds) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", strItemId);
            try {
//                getOrgProductClient(cycle).deleteOrgProduct(caller, orgId, strItemId); <-- bool
                entry.put("status", "OK");
            } catch (NotFoundException e) {
                entry.put("status", "ERROR");
                entry.put("msg", "Not found exception");
            }
            items.add(entry);
        }
        map.put("items", items);

        final String reply = JsonUtils.encode(map);
        return new JsonRequestTarget(reply);
    }

    @WebAction
    public RequestTarget onRenameFolder(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);
        final long caller = LoginUserAccountHelper.getUserId(cycle);

        String folderIdStr = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "folderId");
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        FolderId folderId = FolderId.valueOf(Long.valueOf(folderIdStr));

        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        getOrgMaterialFolderClient(cycle).rename(caller, folderId, new FolderName(name));
        Map<String, Object> map = createMap();
        map.put("status", "OK");
        return new JsonRequestTarget(JsonUtils.encode(map));
    }

    @WebAction
    public RequestTarget onRenameItem(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);
        final long caller = LoginUserAccountHelper.getUserId(cycle);

        String folderId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "itemId");
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");

        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
//        final OrgProductClient opc = CacheClients.getClient(cycle, OrgProductClient.class);
//        opc.rename(caller, folderId, name);
        Map<String, Object> map = createMap();
        map.put("status", "OK");
        return new JsonRequestTarget(JsonUtils.encode(map));
    }

    private List<String> getArray(RequestCycle cycle, String fieldName) {
        final WebRequest request = cycle.getRequest();

        final String[] vals = request.getParameterValues(fieldName + "[]");
        final String val = request.getParameter(fieldName);

        List<String> fieldValues;
        if(val != null) {
            return Arrays.asList(val);
        } else if (vals != null) {
            return new ArrayList<>(Arrays.asList(vals));
        } else {
            return Collections.emptyList();
        }
    }
}
