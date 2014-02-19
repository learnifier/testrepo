/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductTransformers;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeRunnable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.context.command.LazyCacheConfigurationValueCmd;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.material.MaterialUtils;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project.create")
public class NewProjectModule extends AbstractWebAuthModule {

    private static final Pattern ID_SPLIT_PATTERN = Pattern.compile(Pattern.quote("|"));
    private static final Logger LOGGER =
            LoggerFactory.getLogger(NewProjectModule.class);
    /**
     * Action that start the setup for creating a new project.
     *
     */
    public static final String SETUP = "setup";
    private static final String CREATE_MAT_LIST = "createMatList";
    private static final String MAT_LIST_DETAILS = "matListDetails";
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

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(CreateProjectGeneral.class, cycle));
        map.put("org", org);

        map.put("langs", getProjectLocales(cycle));
        map.put("defaultLangLocale", getDefaultLangLocale(cycle));

        map.put("countries", getProjectCountries(cycle));
        map.put("defaultCountryLocale", getDefaultCountryLocale(cycle));

        map.put("timezones", OrgProjectTimezoneFactory.newRecentList(cycle, org.getId()));
        map.put("defaultTimezone", getDefaultTimezone(cycle));

        map.put("orgmats", getCocoboxCordinatorClient(cycle).listOrgMaterial(
                org.getId()));
        map.put("formLink", cycle.urlFor(NewProjectModule.class,
                NewProjectModule.PROCESS_SETUP, strOrgId));

        return new FreemarkerRequestTarget("/project/createProjectSelectDesign.html", map);
    }

    /**
     * Material list page. Set user visible title and description and which materials this project
     * should contain.
     *
     * Processing is done in {@link #onCreate(net.unixdeveloper.druwa.RequestCycle, java.lang.String, java.lang.String)
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

        NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified: {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        } else if (nps.getDesignId().longValue() != 0) {
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

        return new FreemarkerRequestTarget("/project/createProjectGeneral.html",
                map);
    }

    @WebAction
    public RequestTarget onCreateProjectRequired(RequestCycle cycle,
            String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(CreateProjectGeneral.class,
                cycle));
        map.put("org", org);
        map.put("formLink", "");
//        map.put("langs", "");

        return new FreemarkerRequestTarget(
                "/project/projectDesignData.html", map);
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

        final DruwaFormValidationSession<CreateProjectGeneral> formsess =
                getValidationSession(CreateProjectGeneral.class, cycle);

        if (!formsess.process()) {
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        CreateProjectGeneral input = formsess.getObject();

        if (!getProjectLocales(cycle).contains(input.
                getProjectlang())) {
            formsess.addError(new ValidationError(ValidationConstraint.INVALID,
                    "projectlang", "invalidlang"));
        }

        if (!getProjectCountries(cycle).contains(input.
                getCountry())) {
            formsess.addError(new ValidationError(ValidationConstraint.INVALID,
                    "country", "invalidlang"));
        }

        if (formsess.isInError()) {
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        long designId = formsess.getObject().getDesign();

        ProjectType projType = designId == 0 ? ProjectType.MATERIAL_LIST_PROJECT
                : ProjectType.DESIGNED_PROJECT;

        String type = projType.toString();

        List<String> products = null;
        List<Long> orgmats = null;

        if (projType == ProjectType.DESIGNED_PROJECT) {
            products = getDesignProducts(cycle, designId, org);
            orgmats = getDesignOrgmats(cycle, designId);
        }

        CreateProjectSessionProcessor processor = new CreateProjectSessionProcessor(Long.valueOf(
                strOrgId));

        String cancelUrl = NavigationUtil.toOrgProjectsUrl(cycle, strOrgId);

        final NewProjectSession nps = new NewProjectSession(type, orgmats, products, processor,
                cancelUrl,
                designId);

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
                //Product should already been chosen
                return nps.process(cycle, null);
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
    public RequestTarget onCreateMatList(RequestCycle cycle, String strOrgId, String strNpsId) {
        checkOrgPermission(cycle, strOrgId);

        NewProjectSession nps = (NewProjectSession) cycle.getSession().
                getAttribute(NewProjectSession.getSessionName(strNpsId));

        if (nps == null) {
            LOGGER.warn("Invalid NPS id specified (onCreate): {}", strNpsId);
            return NavigationUtil.toCreateProject(cycle, strOrgId);
        }

        DruwaFormValidationSession<MatListProjectDetailsForm> formsess = getFormsess(cycle);

        if (!formsess.process()) {
            return toMatListDetails(strOrgId, strNpsId);
        }

        final MatListProjectDetailsForm input = formsess.getObject();

        String[] matIds = cycle.getRequest().getParameterValues("orgmat");

        List<Long> orgMatIds = getOrgMatIdList(matIds);
        List<String> prodIds = getProductIds(matIds);

        nps.setOrgmats(orgMatIds);
        nps.setProds(prodIds);

        return nps.process(cycle, input);
    }

    public static RequestTarget toMatListDetails(String strOrgId, String npsId) {
        return new WebModuleRedirectRequestTarget(NewProjectModule.class,
                MAT_LIST_DETAILS, strOrgId, npsId);
    }

    private List<Long> getOrgMats(RequestCycle cycle) {
        String[] strIds = cycle.getRequest().getParameterValues(ORGMAT);

        if (strIds == null) {
            return Collections.emptyList();
        }

        List<Long> ids = CollectionsUtil.transformListNotNull(Arrays.asList(strIds),
                new Transformer<String, Long>() {
                    @Override
                    public Long transform(String obj) {
                        String[] selection = ID_SPLIT_PATTERN.split(obj);

                        if (!ORGMAT.equals(selection[0])) {
                            return null;
                        }
                        return Long.valueOf(selection[1]);
                    }
                });

        return ids;
    }

    private List<String> getProducts(RequestCycle cycle) {
        String[] strIds = cycle.getRequest().getParameterValues(ORGMAT);

        if (strIds == null) {
            return Collections.emptyList();
        }

        List<String> ids = CollectionsUtil.transformListNotNull(Arrays.asList(strIds),
                new Transformer<String, String>() {
                    
                    @Override
                    public String transform(String obj) {
                        String[] selection = ID_SPLIT_PATTERN.split(obj);

                        if (!ProductMaterialConstants.NATIVE_SYSTEM.equals(selection[0])) {
                            return null;
                        }
                        return selection[1];
                    }
                });

        return ids;
    }

    public static List<Locale> getProjectLocales(RequestCycle cycle) {
        return new LazyCacheConfigurationValueCmd<List<Locale>>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.langlocales",
                new Transformer<String, List<Locale>>() {
                    @Override
                    public List<Locale> transform(String value) {
                        String[] strLocales = value.split(" *, *");
                        return CollectionsUtil.transformList(Arrays.asList(strLocales),
                                new Transformer<String, Locale>() {
                                    @Override
                                    public Locale transform(String localeStr) {
                                        return LocaleUtils.toLocale(localeStr);
                                    }
                                });
                    }
                });
    }

    public static List<Locale> getProjectCountries(RequestCycle cycle) {
        return new LazyCacheConfigurationValueCmd<List<Locale>>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.countrylocales",
                new Transformer<String, List<Locale>>() {
                    @Override
                    public List<Locale> transform(String value) {
                        String[] strLocales = value.split(" *, *");
                        return CollectionsUtil.transformList(Arrays.asList(strLocales),
                                new Transformer<String, Locale>() {
                                    @Override
                                    public Locale transform(String localeStr) {
                                        return LocaleUtils.toLocale(localeStr);
                                    }
                                });
                    }
                });
    }

    private Locale getDefaultLangLocale(RequestCycle cycle) {
        return new LazyCacheConfigurationValueCmd<Locale>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.langlocale.default",
                new Transformer<String, Locale>() {
                    @Override
                    public Locale transform(String value) {
                        return LocaleUtils.toLocale(value);
                    }
                });
    }

    private Locale getDefaultCountryLocale(RequestCycle cycle) {
        return new LazyCacheConfigurationValueCmd<Locale>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.countrylocale.default",
                new Transformer<String, Locale>() {
                    @Override
                    public Locale transform(String value) {
                        return LocaleUtils.toLocale(value);
                    }
                });
    }

    private TimeZone getDefaultTimezone(RequestCycle cycle) {
        if (1>0) {
            return null;
        }

//        String defaultTz = DwsRealmHelper.
//                getRealmConfiguration(cycle).get("cocobox.project.timezone.default", null);
//        if (defaultTz == null) {
//            return null;
//        }

        return new LazyCacheConfigurationValueCmd<TimeZone>(DwsRealmHelper.
                getRealmConfiguration(cycle)).get("cocobox.project.timezone.default",
                new Transformer<String, TimeZone>() {
                    @Override
                    public TimeZone transform(String value) {
                        return TimeZone.getTimeZone(value);
                    }
                });
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

    public static List<TimeZone> getTimezones(RequestCycle cycle) {
        List<String> ids = Arrays.asList(TimeZone.getAvailableIDs());

        return CollectionsUtil.transformList(ids, new Transformer<String, TimeZone>() {
            @Override
            public TimeZone transform(String tzid) {
                return TimeZone.getTimeZone(tzid);
            }
        });
    }

    private List<String> getDesignProducts(RequestCycle cycle, Long designId, MiniOrgInfo org) {
        //TODO: Verify that the design is a project design
        CourseDesignClient cdClient =
                CacheClients.getClient(cycle, CourseDesignClient.class);
        CourseDesign design = cdClient.getDesign(designId);

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<String> productIds =
                cdd.getAllProductIdStringSet();

        verifyProjectProductsExists(cycle, org, productIds);

        return new ArrayList<>(productIds);
    }

    private List<Long> getDesignOrgmats(RequestCycle cycle, Long designId) {
        CourseDesignClient cdClient =
                CacheClients.getClient(cycle, CourseDesignClient.class);
        CourseDesign design = cdClient.getDesign(designId);

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<Long> orgmatIds = cdd.getAllOrgMatIdSet();

        return new ArrayList<>(orgmatIds);
    }

    private void verifyProjectProductsExists(RequestCycle cycle, MiniOrgInfo org,
            Collection<String> productIds) {

        List<OrgProduct> orgProds = getCocoboxCordinatorClient(cycle).listOrgProducts(org.getId());
        List<String> orgProdIds =
                CollectionsUtil.transformList(orgProds, OrgProductTransformers.
                getProductIdTransformer());

        HashSet<String> missing = new HashSet<>(productIds);
        missing.removeAll(orgProdIds);

        if (missing.isEmpty()) {
            return;
        }
        //We have missing products
        List<Product> missingProducts =
                CacheClients.getClient(cycle, ProductDirectoryClient.class).getProducts(missing);

        StringBuilder sb = new StringBuilder(512);
        sb.append(
                "The selected design includes the following materials that you do not have access to. Reach out to your contact person for any questions.\n\n");

        for (Product product : missingProducts) {
            sb.append(product.getTitle()).append(" (").append(product.getId().getId()).append(")\n");
            missing.remove(product.getId().getId());
        }

        for (String prodId : missing) {
            sb.append(prodId).append('\n');
        }

        cycle.getSession().setFlashAttribute(CpwebConstants.MISSING_PRODUCTS_FLASH, sb.toString());
        
        throw new RetargetException(NavigationUtil.
                toCreateProject(cycle, Long.toString(org.getId())));
        
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

        final DruwaFormValidationSession<CreateProjectGeneral> formsess =
                getValidationSession(CreateProjectGeneral.class,
                cycle);

        ProjectType projType =
                ProjectType.valueOf(nps.getType());

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

        return CollectionsUtil.transformListNotNull(list, new Transformer<String, Long>() {
            @Override
            public Long transform(String item) {
                try {
                    String[] split = MaterialUtils.splitCompositeId(item);

                    if (split[0].equals(OrgMaterialConstants.NATIVE_SYSTEM)) {
                        return Long.parseLong(split[1]);
                    }

                    return null;
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        });
    }

    private List<String> getProductIds(String[] prodIds) {
        List<String> list = prodIds == null ? Collections.<String>emptyList() : Arrays.asList(
                prodIds);

        return CollectionsUtil.transformListNotNull(list, new Transformer<String, String>() {
            @Override
            public String transform(String item) {
                try {
                    String[] split = MaterialUtils.splitCompositeId(item);

                    if (split[0].equals(ProductMaterialConstants.NATIVE_SYSTEM)) {
                        return split[1];
                    }

                    return null;
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        });
    }
}
