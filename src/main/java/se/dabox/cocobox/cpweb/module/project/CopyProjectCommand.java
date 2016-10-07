package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductTransformers;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductTransformers;
import se.dabox.util.collections.CollectionsUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class CopyProjectCommand {

    private final RequestCycle cycle;
    public CopyProjectCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    Long execute(Project project) {

        final ProjectType type = project.getType();
        if(type != ProjectType.DESIGNED_PROJECT) {
            throw new UnsupportedOperationException("Can only copy projects of type designed");
        }
        final String subtype = project.getSubtype();
        OrgProject orgProj = (OrgProject)project; // How do i check project type?

        final Long designId = orgProj.getDesignId();
        List<String> products = products = getDesignProducts(cycle, designId, orgProj.getOrgId());
        List<Long> orgmats = getDesignOrgmats(cycle, designId);


        CreateProjectSessionProcessor processor = new CreateProjectSessionProcessor(orgProj.getOrgId());

        String cancelUrl = null;

        final NewProjectSession nps = new NewProjectSession(type.toString(), orgmats, products, processor,
                cancelUrl,
                designId,
                null);

        Integer courseId = null;
        nps.setCourseId(courseId);
        final CreateProjectGeneral input = new CreateProjectGeneral();
        input.setProjectname("roflname");
        input.setCountry(orgProj.getCountry());
        input.setDesign(String.valueOf(orgProj.getDesignId()));
        input.setProjectlang(orgProj.getLocale());
        input.setTimezone(orgProj.getTimezone());

        nps.setCreateProjectGeneral(input); // TODO: Should replace the form object
        nps.storeInSession(cycle.getSession());
        nps.process(cycle, null);

        return null;
    }


    private List<String> getDesignProducts(RequestCycle cycle, Long designId, Long orgId) {
        //TODO: Verify that the design is a project design
        CourseDesignClient cdClient = CacheClients.getClient(cycle, CourseDesignClient.class);
        CourseDesign design = cdClient.getDesign(designId);

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<String> productIds = cdd.getAllProductIdStringSet();

        verifyProjectProductsExists(cycle, orgId, productIds);

        return new ArrayList<>(productIds);
    }

    private List<Long> getDesignOrgmats(RequestCycle cycle, Long designId) {
        CourseDesignClient cdClient = CacheClients.getClient(cycle, CourseDesignClient.class);
        CourseDesign design = cdClient.getDesign(designId);

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<Long> orgmatIds = cdd.getAllOrgMatIdSet();

        return new ArrayList<>(orgmatIds);
    }


    private void verifyProjectProductsExists(RequestCycle cycle, long orgId,
                                             Collection<String> productIds) {

        List<OrgProduct> orgProds = getCocoboxCordinatorClient(cycle).listOrgProducts(orgId);
        List<String> orgProdIds = CollectionsUtil.transformList(orgProds, OrgProductTransformers.
                getProductIdTransformer());

        HashSet<String> missing = new HashSet<>(productIds);
        missing.removeAll(orgProdIds);

        if (missing.isEmpty()) {
            return;
        }

        final ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        //We have missing products
        List<Product> missingProducts = pdClient.getProducts(missing);
        //Create a writable copy
        missingProducts = new ArrayList<>(missingProducts);

        removeMissingRealmAnonProd(missingProducts);

        if (missingProducts.isEmpty()) {
            return;
        }

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
                toCreateProject(cycle, Long.toString(orgId)));

    }


    private void removeMissingRealmAnonProd(List<Product> missingProducts) {
        Map<String, Product> productMap = CollectionsUtil.createMap(missingProducts,
                ProductTransformers.getIdStringTransformer());

        for (Iterator<Product> iterator = missingProducts.iterator(); iterator.hasNext();) {
            Product prod = iterator.next();

            if (prod.isAnonymous() && prod.isRealmProduct()) {
                iterator.remove();
            }
        }

    }



}
