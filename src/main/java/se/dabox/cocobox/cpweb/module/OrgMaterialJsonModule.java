/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.codehaus.jackson.JsonGenerator;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.ProjectModule;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocosite.converter.DateConverter;
import se.dabox.cocosite.converter.cds.CdsUtil;
import se.dabox.cocosite.date.DatePickerDateConverter;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.product.GetProjectCompatibleProducts;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.dws.client.langservice.LangBundle;
import se.dabox.dws.client.langservice.LangService;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.ccbc.material.OrgMaterialLink;
import se.dabox.service.common.ccbc.material.UpdateOrgMaterialLink;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductLink;
import se.dabox.service.common.ccbc.org.OrgProductTransformers;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectSubtypeConstants;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductTypeUtil;
import se.dabox.service.common.proddir.filter.DeeplinkProductFilter;
import se.dabox.service.common.proddir.material.ProductMaterial;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;
import se.dabox.service.common.proddir.material.StandardThumbnailGeneratorFactory;
import se.dabox.service.common.proddir.material.ThumbnailGeneratorFactory;
import se.dabox.service.proddir.data.FieldValue;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductPredicates;
import se.dabox.service.proddir.data.ProductTypeId;
import se.dabox.service.tokenmanager.client.AccountBalance;
import se.dabox.service.tokenmanager.client.AccountBalanceTransformers;
import se.dabox.service.tokenmanager.client.TokenManagerClient;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;
import se.dabox.util.collections.Transformer;
import se.dabox.util.converter.ConversionContext;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/orgmats.json")
public class OrgMaterialJsonModule extends AbstractJsonAuthModule {
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

        return jsonTarget(toJsonMaterials(cycle, null, materials));
    }

    @WebAction
    public RequestTarget onListMatListMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<Material> materials = getOrgMaterials(cycle, orgId, null, ProjectType.MATERIAL_LIST_PROJECT);

        return jsonTarget(toJsonMaterials(cycle, null, materials));
    }
    
    @WebAction
    public RequestTarget onListPurchasedMats(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        List<OrgProduct> orgProds =
                getCocoboxCordinatorClient(cycle).listOrgProducts(orgId);

        List<Product> products = getGrantedProducts(cycle, orgProds);

        return processProducts(cycle, products, orgId, orgProds, strOrgId);
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

        final CocoboxCordinatorClient client = getCocoboxCordinatorClient(cycle);

        long userId = getCurrentUser(cycle);

        long linkId = client.addOrgMaterialLink(userId, materialId);
        //TODO: Fetch all, is this really the best way to do it?
        OrgMaterialLink link = getLink(client, materialId, linkId);

        String deliveryBase = getDeliveryBase(cycle);
        return jsonTarget(toJsonLinks(Collections.singletonList(link), deliveryBase));
    }

    @WebAction
    public RequestTarget onDeleteOrgMatLink(RequestCycle cycle) {
        //TODO: Security check here
        long orgmatlinkid = Long.valueOf(cycle.getRequest().getParameter("orgmatlinkid"));

        final CocoboxCordinatorClient client = getCocoboxCordinatorClient(cycle);

        client.deleteOrgMaterialLink(LoginUserAccountHelper.getUserId(cycle), orgmatlinkid);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onDeleteOrgMat(RequestCycle cycle) {
        long orgmatid = Long.valueOf(cycle.getRequest().getParameter("orgmatid"));

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        OrgMaterial orgMat = ccbc.getOrgMaterial(orgmatid);
        checkOrgPermission(cycle, orgMat.getOrgId());

        int activeLinks = getActiveLinks(ccbc, orgMat);
        List<String> projectNames = getLinkedProjectNames(ccbc, orgMat);

        Map<String, Object> map = createMap();

        map.put("orgmatid", orgmatid);
        map.put("activeLinkCount", activeLinks);
        map.put("linkedProjectNames", projectNames);

        boolean canDelete = activeLinks == 0 && projectNames.isEmpty();
        map.put("status", canDelete ? "OK" : "DENIED");

        if (canDelete) {
            ccbc.deleteOrgMaterial(orgmatid, LoginUserAccountHelper.getUserId(cycle));
        }

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onListOrgMatLinks(RequestCycle cycle) {

        String deliveryBase = getDeliveryBase(cycle);
        long materialId = Long.valueOf(cycle.getRequest().getParameter("orgmatid"));

        final List<OrgMaterialLink> links = getOrgLinksOrCreateLink(cycle, materialId);

        ByteArrayOutputStream baos =
                toJsonLinks(links, deliveryBase);

        return jsonTarget(baos);
    }

    public static String getDeliveryBase(RequestCycle cycle) {
        return CdsUtil.getDeliveryBase(cycle);
    }

    public static ByteArrayOutputStream toJsonLinks(final List<OrgMaterialLink> links,
            final String deliveryBase) {
        ByteArrayOutputStream baos =
                new JsonEncoding() {
                    @Override
                    protected void encodeData(JsonGenerator generator) throws IOException {
                        generator.writeStartObject();
                        generator.writeArrayFieldStart("aaData");

                        for (OrgMaterialLink orgMaterialLink : links) {
                            generator.writeStartObject();

                            generator.writeNumberField("linkid", orgMaterialLink.getId());
                            generator.writeBooleanField("active", orgMaterialLink.isActive());
                            generator.writeStringField("deeplink",
                                    deliveryBase + orgMaterialLink.getCdLinkId());
                            generator.writeStringField("activeto", formatDate(orgMaterialLink.
                                    getActiveTo()));

                            generator.writeEndObject();

                        }

                        generator.writeEndArray();
                        generator.writeEndObject();
                    }
                }.encodeToStream();
        return baos;
    }

    @WebAction
    public RequestTarget onChangeLinkStatus(RequestCycle cycle) {
        Long linkid = Long.valueOf(cycle.getRequest().getParameter("linkid"));
        boolean active = Boolean.valueOf(cycle.getRequest().getParameter("active"));

        UpdateOrgMaterialLink update = new UpdateOrgMaterialLink(linkid, getCurrentUser(cycle));
        update.setActive(active);
        getCocoboxCordinatorClient(cycle).updateOrgMaterialLink(update);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onChangeLinkActiveTo(RequestCycle cycle) {
        Long linkid = Long.valueOf(cycle.getRequest().getParameter("linkid"));
        Date activeTo = getActiveTo(cycle);

        if (activeTo == null) {
            Map<String, Object> map = createMap();
            map.put("status", "ERROR_INPUT");
            return jsonTarget(map);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(activeTo);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);


        UpdateOrgMaterialLink update = new UpdateOrgMaterialLink(linkid, getCurrentUser(cycle));
        update.setActiveTo(cal.getTime());
        getCocoboxCordinatorClient(cycle).updateOrgMaterialLink(update);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    private static void sortOrgMats(RequestCycle cycle, List<OrgMaterial> materials) {
        final Collator collator = Collator.getInstance(
                CocositeUserHelper.getUserLocale(cycle));
        collator.setStrength(Collator.SECONDARY);

        Collections.sort(materials, new Comparator<OrgMaterial>() {
            @Override
            public int compare(OrgMaterial o1, OrgMaterial o2) {
                int diff = collator.compare(o1.getTitle(), o2.getTitle());
                if (diff != 0) {
                    return diff;
                }

                if (o1.getOrgMaterialId() < o2.getOrgMaterialId()) {
                    return -1;
                }
                return 1;
            }
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

                for (OrgMaterial material : materials) {
                    generator.writeStartObject();
                    generator.writeNumberField("id", material.getOrgMaterialId());
                    generator.writeStringField("title", material.getTitle());
                    generator.writeStringField("type", material.getType());
                    generator.writeStringField("desc", material.getDescription());
                    generator.writeStringField("crlink", material.getCrlink());
                    generator.writeStringField("weblink", material.getWeblink());
                    generator.writeNumberField("createdBy", material.getCreatedBy());
                    generator.writeNumberField("created", material.getCreatedBy());
                    writeLongNullField(generator, "updatedBy",
                            material.getUpdatedBy());
                    generator.writeNumberField("updated", material.getUpdatedBy());
                    generator.writeStringField("viewLink",
                            cycle.urlFor(CpMainModule.class.getName(),
                            "viewOrgMaterial",
                            orgId, Long.toString(material.getOrgMaterialId())));
                    generator.writeNumberField("activeLinks", material.getActiveLinks());
                    generator.writeNumberField("inactiveLinks", material.getInactiveLinks());
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
            final List<Material> materials) {

        final LangBundle bundle = getLangBundle(cycle);

        return new DataTablesJson<Material>() {
            @Override
            protected void encodeItem(Material material) throws IOException {
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

        }.encodeToStream(materials);
    }

    private static String formatDate(Date activeTo) {
        return DatePickerDateConverter.toDatePickerDate(activeTo);
    }

    private List<OrgMaterialLink> getOrgLinksOrCreateLink(RequestCycle cycle, long materialId) {
        final CocoboxCordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient(cycle);
        List<OrgMaterialLink> links =
                cocoboxCordinatorClient.listOrgMaterialLinks(
                materialId);

        if (!links.isEmpty()) {
            return links;
        }

        cocoboxCordinatorClient.addOrgMaterialLink(getCurrentUser(cycle), materialId);

        return cocoboxCordinatorClient.listOrgMaterialLinks(materialId);
    }

    private OrgMaterialLink getLink(CocoboxCordinatorClient client, long materialId, long linkId) {
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
            return null;
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

    private int getActiveLinks(CocoboxCordinatorClient ccbc, OrgMaterial orgMat) {
        return orgMat.getActiveLinks();
    }

    private List<String> getLinkedProjectNames(CocoboxCordinatorClient ccbc, OrgMaterial orgMat) {
        List<OrgProject> projects = ccbc.listOrgProjectsUsingOrgMat(orgMat.getOrgMaterialId());

        return CollectionsUtil.transformList(projects, new Transformer<OrgProject, String>() {
            @Override
            public String transform(OrgProject obj) {
                return obj.getName();
            }
        });
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
        return CollectionsUtil.countMatching(links, new Predicate<OrgProductLink>() {
            @Override
            public boolean evalute(OrgProductLink obj) {
                return obj.isActive();
            }
        });
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

        return CollectionsUtil.sublist(matchingProducts, new Predicate<Product>() {
            @Override
            public boolean evalute(Product item) {
                return grantedIds.contains(item.getId().getId());
            }
        });
    }

    private Map<Long, List<OrgProductLink>> getProductLinkMap(RequestCycle cycle,
            Collection<Long> orgProductIds) {

        List<OrgProductLink> links =
                getCocoboxCordinatorClient(cycle).getOrgProductsLinks(orgProductIds);

        return CollectionsUtil.createMapList(links, new Transformer<OrgProductLink, Long>() {
            @Override
            public Long transform(OrgProductLink item) {
                return item.getOrgProductId();
            }
        });

    }

    public static List<Material> getOrgMaterials(final RequestCycle cycle, final long orgId,
            OrgProject project, ProjectType projectType) {

        List<Product> products = getOrgProducts(cycle, orgId, project, projectType);

        final CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

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
        final CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        List<Product> products = getGrantedProducts(cycle, ccbc.listOrgProducts(orgId));
        ProductTypeUtil.setTypes(getProductDirectoryClient(cycle), products);

        if (project != null) {
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
        Collections.sort(sortedList, new Comparator<Product>() {

            @Override
            public int compare(Product o1, Product o2) {
                int diff = col.compare(o1.getTitle(), o2.getTitle());

                if (diff == 0) {
                    diff = o1.getId().compareTo(o2.getId());
                }

                return diff;
            }
        });

        return sortedList;
    }

    private static class CrispAdminLinkInfo {

        public String link;
        public String name;

        public CrispAdminLinkInfo(String link, String name) {
            this.link = link;
            this.name = name;
        }
    }
}
