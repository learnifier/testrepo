/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.project.productconfig.ExtraProductConfig;
import se.dabox.cocobox.cpweb.module.project.productconfig.ProductNameMapFactory;
import se.dabox.cocobox.cpweb.module.project.productconfig.SettingsFormInputProcessor;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeRunnable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.context.command.LazyCacheConfigurationValueCmd;
import se.dabox.service.common.locale.GetUserDefaultLocaleCommand;
import se.dabox.service.common.locale.GetUserLocalesCommand;
import se.dabox.service.common.material.MaterialUtils;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.util.HybridLocaleUtils;
import se.dabox.util.collections.CollectionsUtil;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import se.dabox.cocosite.timezone.FormTimeZone;
import se.dabox.cocosite.timezone.PlatformFormTimeZoneFactory;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.create")
public class NewProjectModule extends AbstractWebAuthModule {

    private static final Pattern ID_SPLIT_PATTERN = Pattern.compile(Pattern.quote("|"));
    private static final Logger LOGGER = LoggerFactory.getLogger(NewProjectModule.class);
    /**
     * Action that start the setup for creating a new project.
     *
     */
    public static final String SETUP = "setup";
    private static final String CREATE_MAT_LIST = "createMatList";
    private static final String MAT_LIST_DETAILS = "matListDetails";
    private static final String SPP_DETAILS = "singleProductProjectDetails";
    private static final String PROCESS_SETUP = "processSetup";
    private static final String ORGMAT = OrgMaterialConstants.NATIVE_SYSTEM;
    public static final String FORMSESS_ATTR = "createproject.formsess";

    /**
     * Setup basic details. Select a design (or a fake matlist design). Processing in
     * {@link #onProcessSetup(net.unixdeveloper.druwa.RequestCycle, java.lang.String)}. Redirects to
     * {@link #onMatListDetails(net.unixdeveloper.druwa.RequestCycle, java.lang.String, java.lang.String)}.
     *
     * @param cycle
     * @param strOrgId
     *
     * @return
     */
    @WebAction
    public RequestTarget onSetup(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(CreateProjectGeneral.class, cycle));
        map.put("org", org);

        map.put("langs", getProjectLocales(cycle));
        map.put("defaultLangLocale", getDefaultLangLocale(cycle));

        map.put("countries", getProjectCountries(cycle));
        map.put("defaultCountryLocale", getDefaultCountryLocale(cycle));

        map.put("timezones", OrgProjectTimezoneFactory.newRecentList(cycle, org.getId()));
        map.put("defaultTimezone", getDefaultTimezone(cycle));

        map.put("orgMatListSupport", getMatListSupportSetting(cycle, org));

        map.put("orgmats", getCocoboxCordinatorClient(cycle).listOrgMaterial(
                org.getId()));
        map.put("formLink", cycle.urlFor(NewProjectModule.class,
                NewProjectModule.PROCESS_SETUP, strOrgId));

        return new FreemarkerRequestTarget("/project/createProjectSelectDesign.html", map);
    }

    /**
     * Setup basic details. Select a design (or a fake matlist design). Processing in
     * {@link #onProcessSetup(net.unixdeveloper.druwa.RequestCycle, java.lang.String)}. Redirects to
     * {@link #onMatListDetails(net.unixdeveloper.druwa.RequestCycle, java.lang.String, java.lang.String)}.
     *
     * @param cycle
     * @param strOrgId
     *
     * @return
     */
    @WebAction
    public RequestTarget onSessionSetup(RequestCycle cycle, String strOrgId, String strCourseId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        int courseId = Integer.valueOf(strCourseId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(CatalogCourseId.valueOf(courseId)).build());
        if(courses == null || courses.size() != 1) {
            throw new IllegalStateException("Can not find course"); // TODO: Not sure how to signal error here.
        }
        final CatalogCourse course = courses.get(0);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(CreateProjectGeneral.class, cycle));
        map.put("org", org);

        map.put("langs", getProjectLocales(cycle));
        map.put("defaultLangLocale", getDefaultLangLocale(cycle));

        map.put("countries", getProjectCountries(cycle));
        map.put("defaultCountryLocale", getDefaultCountryLocale(cycle));

        map.put("timezones", OrgProjectTimezoneFactory.newRecentList(cycle, org.getId()));
        map.put("defaultTimezone", getDefaultTimezone(cycle));

        map.put("orgMatListSupport", getMatListSupportSetting(cycle, org));

        map.put("orgmats", getCocoboxCordinatorClient(cycle).listOrgMaterial(
                org.getId()));
        map.put("formLink", cycle.urlFor(NewProjectModule.class,
                NewProjectModule.PROCESS_SETUP, strOrgId));

        map.put("course", course);

        return new FreemarkerRequestTarget("/project/createSession.html", map);
    }

    /**
     * Material list page. Set user visible title and description and which materials this project
     * should contain.
     *
     * Processing is done in {@link #onCreateMatList(net.unixdeveloper.druwa.RequestCycle, java.lang.String, java.lang.String)
     * }. Redirects to {@link ProjectModule#onRoster(net.unixdeveloper.druwa.RequestCycle, java.lang.String)
     * }.
     *
     * @param cycle
     * @param strOrgId
     * @param strNpsId
     *
     * @return
     */
    @WebAction
    public RequestTarget onMatListDetails(RequestCycle cycle,
            String strOrgId, String strNpsId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT_MATERIAL);

        NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        } else if (nps.getDesignId() != 0) {
            LOGGER.warn("Invalid NPS for matlist configuration: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        Map<String, Object> map = createMap();

        boolean designProject = false;

        map.put("formsess", getFormsess(cycle));
        map.put("org", org);
        map.put("formLink",
                cycle.urlFor(NewProjectModule.class.getName(),
                        NewProjectModule.CREATE_MAT_LIST, strOrgId, strNpsId));
        map.put("designProject", designProject);

        map.put("langs", getProjectLocales(cycle));
        map.put("defaultLangLocale", getDefaultLangLocale(cycle));

        map.put("countries", getProjectCountries(cycle));
        map.put("defaultCountryLocale", getDefaultCountryLocale(cycle));

//        map.put("timezones", getTimezones(cycle));
//        map.put("defaultTimezone", getDefaultTimezone(cycle));
        map.put("nps", nps);

        return new FreemarkerRequestTarget("/project/createProjectGeneralMatList.html",
                map);
    }

    /**
     * Single Product Project page. Set user visible title and description this project should
     * contain.
     *
     * Processing is done in {@link #onCreateMatList(net.unixdeveloper.druwa.RequestCycle, java.lang.String, java.lang.String)
     * }. Redirects to {@link ProjectModule#onRoster(net.unixdeveloper.druwa.RequestCycle, java.lang.String)
     * }.
     *
     * @param cycle
     * @param strOrgId
     * @param strNpsId
     *
     * @return
     */
    @WebAction
    public RequestTarget onSingleProductProjectDetails(RequestCycle cycle,
            String strOrgId, String strNpsId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        } else if (ProjectType.valueOf(nps.getType()) != ProjectType.SINGLE_PRODUCT_PROJECT) {
            LOGGER.warn("Invalid NPS for SPP: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        Map<String, Object> map = createMap();

        boolean designProject = false;

        map.put("formsess", getFormsess(cycle));
        map.put("org", org);
        map.put("formLink",
                cycle.urlFor(NewProjectModule.class.getName(),
                        NewProjectModule.CREATE_MAT_LIST, strOrgId, strNpsId));
        map.put("designProject", designProject);

        map.put("langs", getProjectLocales(cycle));
        map.put("defaultLangLocale", getDefaultLangLocale(cycle));

        map.put("countries", getProjectCountries(cycle));
        map.put("defaultCountryLocale", getDefaultCountryLocale(cycle));

        map.put("nps", nps);

        return new FreemarkerRequestTarget("/project/createProjectGeneralSpp.html",
                map);
    }

    @WebAction
    public RequestTarget onCreateProjectRequired(RequestCycle cycle,
            String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(CreateProjectGeneral.class,
                cycle));
        map.put("org", org);
        map.put("formLink", "");
//        map.put("langs", "");

        return new FreemarkerRequestTarget(
                "/project/projectDesignData.html", map);
    }

    @WebAction
    public RequestTarget onProductExtraSettings(RequestCycle cycle,
            String strOrgId, String strNpsId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        Map<String, Object> map = createMap();

        map.put("org", org);

        map.put("nps", nps);
        map.put("npsId", strNpsId);
        map.put("extraConfig", nps.getExtraConfig());

        map.put("productNameMap", new ProductNameMapFactory().create(nps.getExtraConfig()));
        map.put("productValueSource",
                new ProductsValueSource(userLocale, nps.getExtraConfig()));

        return new FreemarkerRequestTarget(
                "/project/projectProductExtraSettings.html", map);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onProcessProductExtraSettings(RequestCycle cycle,
            String strOrgId, String strNpsId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        WebRequest req = cycle.getRequest();

        final List<ExtraProductConfig> extraConfigList = nps.getExtraConfig();

        Map<String, Map<String, String>> productsMap = new SettingsFormInputProcessor().
                processFormInput(extraConfigList, req);

        //Everything is valid(!)
        for (ExtraProductConfig extraConfig : extraConfigList) {
            final Map<String, String> settingsMap = productsMap.get(extraConfig.getProductId());
            extraConfig.setSettings(settingsMap);
        }

        return nps.process(cycle, null);
    }

    /**
     * Processes the options from the setup page. If design project then create the project and
     * redirect to the configure data details page. If matlist project then redirect to the
     * configure matlist details page
     * ({@link #onMatListDetails(net.unixdeveloper.druwa.RequestCycle, java.lang.String, java.lang.String) }).
     *
     * @param cycle
     * @param strOrgId
     *
     * @return
     */
    @WebAction
    public RequestTarget onProcessSetup(final RequestCycle cycle, final String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_CREATE_PROJECT);

        final DruwaFormValidationSession<CreateProjectGeneral> formsess = getValidationSession(
                CreateProjectGeneral.class, cycle);

        if (!formsess.process()) {
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        final Integer courseId;
        final String strCourseId = cycle.getRequest().getParameter("courseId");
        if(strCourseId != null) {
            courseId = Integer.parseInt(strCourseId);
        } else {
            courseId = null;
        }
        CreateProjectGeneral input = formsess.getObject();

        if (!getProjectLocales(cycle).contains(input.
                getProjectlang())) {
            formsess.addError(new ValidationError(ValidationConstraint.INVALID,
                    "projectlang", "invalidlang"));
        }

        if (!validCountry(cycle, input.getCountry())) {
            formsess.addError(new ValidationError(ValidationConstraint.INVALID,
                    "country", "invalidlang"));
        }

        if (formsess.isInError()) {
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        String typeId = formsess.getObject().getDesign();
        String productId = null;
        //Default value is important. Checked by other pages (like the matlist details)
        long designId = 0;

        ProjectType projType = null;

        if (typeId.startsWith(NewProjectJsonModule.CD_PREFIX)) {
            projType = ProjectType.DESIGNED_PROJECT;
            designId = Long.parseLong(typeId.substring(NewProjectJsonModule.CD_PREFIX.length()));
        } else if (typeId.startsWith(NewProjectJsonModule.MATLIST_PREFIX)) {
            projType = ProjectType.MATERIAL_LIST_PROJECT;
            designId = 0;
        } else if (typeId.startsWith(NewProjectJsonModule.SPP_PREFIX)) {
            projType = ProjectType.SINGLE_PRODUCT_PROJECT;
            productId = typeId.substring(NewProjectJsonModule.SPP_PREFIX.length());

            if (!isValidSppProductId(cycle, productId)) {
                formsess.addError(new ValidationError(ValidationConstraint.INVALID,
                        "designId", "invalid"));
                LOGGER.warn("Not a valid SPP product: {}", productId);
            }
        } else {
            formsess.addError(new ValidationError(ValidationConstraint.INVALID,
                    "designId", "invalid"));
        }

        if (formsess.isInError()) {
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        String type = projType.toString();

        List<String> products = null;
        List<Long> orgmats = null;

        if (projType == ProjectType.DESIGNED_PROJECT) {
            products = ProductsHelper.getDesignProducts(cycle, designId, org.getId());
            orgmats = ProductsHelper.getDesignOrgmats(cycle, designId);
        }

        CreateProjectSessionProcessor processor = new CreateProjectSessionProcessor(Long.valueOf(
                strOrgId));

        String cancelUrl = NavigationUtil.toOrgProjectsUrl(cycle, strOrgId);

        final NewProjectSession nps = new NewProjectSession(type, orgmats, products, processor,
                cancelUrl,
                designId,
                productId);

        nps.setCourseId(courseId);
        nps.setCreateProjectGeneral(input);
        nps.storeInSession(cycle.getSession());

        return ProjectTypeUtil.call(projType, new ProjectTypeCallable<RequestTarget>() {
            @Override
            public RequestTarget callDesignedProject() {
                return nps.process(cycle, null);
            }

            @Override
            public RequestTarget callMaterialListProject() {
                return new WebModuleRedirectRequestTarget(NewProjectModule.class,
                        MAT_LIST_DETAILS, strOrgId, nps.getUuid().toString());
            }

            @Override
            public RequestTarget callSingleProductProject() {
                return new WebModuleRedirectRequestTarget(NewProjectModule.class,
                        SPP_DETAILS, strOrgId, nps.getUuid().toString());
            }
        });
    }

    /**
     * Invoked to perform a creation of a matlist project.
     *
     * @param cycle
     * @param strOrgId
     * @param strNpsId
     *
     * @return
     */
    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onCreateMatList(final RequestCycle cycle, String strOrgId, String strNpsId) {
        checkOrgPermission(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_CREATE_PROJECT);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_CREATE_PROJECT_MATERIAL);

        final NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified (onCreate): {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        ProjectType type = ProjectType.valueOf(nps.getType());

        if (!(type == ProjectType.MATERIAL_LIST_PROJECT || type
                == ProjectType.SINGLE_PRODUCT_PROJECT)) {
            LOGGER.warn("Invalid project type for this action: {}/{}", strNpsId, type);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        final DruwaFormValidationSession<MatListProjectDetailsForm> formsess = getFormsess(cycle);

        if (!formsess.process()) {
            return toMatListDetails(strOrgId, strNpsId);
        }

        final MatListProjectDetailsForm input = formsess.getObject();

        ProjectTypeUtil.run(type, new ProjectTypeRunnable() {

            @Override
            public void runMaterialListProject() {
                String[] matIds = cycle.getRequest().getParameterValues("orgmat");

                List<Long> orgMatIds = getOrgMatIdList(matIds);
                List<String> prodIds = getProductIds(matIds);

                nps.setOrgmats(orgMatIds);
                nps.setProds(prodIds);
            }

            @Override
            public void runDesignedProject() {
                throw new UnsupportedOperationException("Not supported"); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void runSingleProductProject() {
                //Do nothing
            }
        });

        return nps.process(cycle, input);
    }

    public static RequestTarget toMatListDetails(String strOrgId, String npsId) {
        return new WebModuleRedirectRequestTarget(NewProjectModule.class,
                MAT_LIST_DETAILS, strOrgId, npsId);
    }

    public static List<Locale> getProjectLocales(RequestCycle cycle) {
        return new GetUserLocalesCommand().getLocales(cycle);
    }

    public static List<Locale> getProjectCountries(RequestCycle cycle) {
        String[] countries = Locale.getISOCountries();

        List<Locale> list
                = CollectionsUtil.transformList(Arrays.asList(countries), (String item) ->
                        new Locale("", item));

        final Locale sortLocale = CocositeUserHelper.getUserLocale(cycle);

        final Collator collator = Collator.getInstance(sortLocale);

        Collections.sort(list, (Locale o1, Locale o2) -> {
            String o1Country = o1.getDisplayCountry(sortLocale);
            String o2Country = o2.getDisplayCountry(sortLocale);
            int res = collator.compare(o1Country, o2Country);

            if (res != 0) {
                return res;
            }

            return o1.toLanguageTag().compareTo(o2.toLanguageTag());
        });

        return list;
    }

    private Locale getDefaultLangLocale(RequestCycle cycle) {
        return new GetUserDefaultLocaleCommand().getLocale(cycle);
    }

    private Locale getDefaultCountryLocale(RequestCycle cycle) {
        return new LazyCacheConfigurationValueCmd<Locale>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.countrylocale.default",
                        HybridLocaleUtils::toLocale);
    }

    private TimeZone getDefaultTimezone(RequestCycle cycle) {
//        String defaultTz = DwsRealmHelper.
//                getRealmConfiguration(cycle).get("cocobox.project.timezone.default", null);
//        if (defaultTz == null) {
//            return null;
//        }
        return new LazyCacheConfigurationValueCmd<TimeZone>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.timezone.default",
                        TimeZone::getTimeZone);
    }

    private DruwaFormValidationSession<MatListProjectDetailsForm> getFormsess(RequestCycle cycle) {

        DruwaFormValidationSession<MatListProjectDetailsForm> formsess = cycle.
                getAttribute(FORMSESS_ATTR);

        if (formsess != null) {
            LOGGER.debug("Formsess found in request cycle");
            return formsess;
        }

        return getValidationSession(MatListProjectDetailsForm.class,
                cycle);
    }

    public static List<FormTimeZone> getTimezones(RequestCycle cycle) {
        return new PlatformFormTimeZoneFactory(cycle, CocositeUserHelper.getUserLocale(cycle)).
                getTimeZoneList();
    }

    /**
     * This is used for the form input processor.
     *
     * @param cycle
     * @param nps
     *
     * @return
     */
    private DruwaFormValidationSession<CreateProjectGeneral> getCreateProjectGeneralFormsess(
            RequestCycle cycle,
            NewProjectSession nps) {

        final DruwaFormValidationSession<CreateProjectGeneral> formsess = getValidationSession(
                CreateProjectGeneral.class,
                cycle);

        ProjectType projType = ProjectType.valueOf(nps.getType());

        ProjectTypeUtil.run(projType, new ProjectTypeRunnable() {
            @Override
            public void runDesignedProject() {
                formsess.getMutableFieldDescription("userTitle", true).setOptional(true);

                formsess.getMutableFieldDescription("userDescription", true).setOptional(
                        true);
            }

            @Override
            public void runMaterialListProject() {
                //Do nothing
            }

            @Override
            public void runSingleProductProject() {
                //Do nothing
            }
        });

        return formsess;
    }

    private List<Long> getOrgMatIdList(String[] matIds) {
        List<String> list = matIds == null ? Collections.<String>emptyList() : Arrays.asList(matIds);

        return CollectionsUtil.transformListNotNull(list, (String item) -> {
            try {
                String[] split = MaterialUtils.splitCompositeId(item);

                if (split[0].equals(OrgMaterialConstants.NATIVE_SYSTEM)) {
                    return Long.parseLong(split[1]);
                }

                return null;
            } catch (IllegalArgumentException ex) {
                return null;
            }
        });
    }

    private List<String> getProductIds(String[] prodIds) {
        List<String> list = prodIds == null ? Collections.<String>emptyList() : Arrays.asList(
                prodIds);

        return CollectionsUtil.transformListNotNull(list, (String item) -> {
            try {
                String[] split = MaterialUtils.splitCompositeId(item);

                if (split[0].equals(ProductMaterialConstants.NATIVE_SYSTEM)) {
                    return split[1];
                }

                return null;
            } catch (IllegalArgumentException ex) {
                return null;
            }
        });
    }

    private boolean isValidSppProductId(RequestCycle cycle, String productId) {
        Product product = ProductFetchUtil.getProduct(getProductDirectoryClient(cycle), productId);

        if (product == null) {
            return false;
        }

        return ProductUtils.isSingleProductProjectProduct(product);
    }

    private Boolean getMatListSupportSetting(RequestCycle cycle, MiniOrgInfo miniOrg) {
        if (!WebFeatures.getFeatures(cycle).hasFeature(CocositeWebFeatureConstants.MATERIALLIST)) {
            return Boolean.FALSE;
        }

        if (!hasOrgPermission(cycle, miniOrg.getId(), CocoboxPermissions.CP_CREATE_PROJECT_MATERIAL)) {
            return Boolean.FALSE;
        }

        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        OrgUnitInfo org = odClient.getOrgUnitInfo(miniOrg.getId());

        String strValue = org.getProfileValue(CocoSiteConstants.ORG_PROFILE,
                CocoSiteConstants.ORG_MATLISTSUPPORT);

        if (strValue == null) {
            return Boolean.FALSE;
        }

        return Boolean.valueOf(strValue);
    }

    private boolean validCountry(RequestCycle cycle, Locale country) {
        List<Locale> countries = getProjectCountries(cycle);

        for (Locale locale : countries) {
            if (locale.equals(country)) {
                return true;
            }
        }

        return false;
    }

}
