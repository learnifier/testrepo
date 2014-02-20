/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.codehaus.jackson.JsonGenerator;
import se.dabox.cocobox.cpweb.module.OrgMaterialJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.coursedesign.CourseDesignThumbnail;
import se.dabox.cocosite.coursedesign.GetCourseDesignBucketCommand;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.common.coursedesign.BucketCourseDesignInfo;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.material.Material;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project.createjs")
public class NewProjectJsonModule extends AbstractJsonAuthModule {
    public static final String CD_PREFIX = "cd-";
    public static final String SPP_PREFIX = "spp-";
    public static final String MATLIST_PREFIX = "ml-";

    @WebAction
    public RequestTarget onNewProjectTypes(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        List<NewProjectType> designTypes = getDesignTypes(cycle, org);
        List<NewProjectType> productTypes = getSingleProductTypes(cycle, org);

        List<NewProjectType> types = new ArrayList<>(designTypes);
        types.addAll(productTypes);

        final Collator collator = Collator.getInstance(CocositeUserHelper.getUserLocale(cycle));
        collator.setStrength(Collator.SECONDARY);

        Collections.sort(types, new Comparator<NewProjectType>() {

            @Override
            public int compare(NewProjectType o1, NewProjectType o2) {
                int diff = collator.compare(o1.getName(), o2.getName());

                if (diff != 0) {
                    return diff;
                }

                return o1.getId().compareTo(o2.getId());
            }
        });

        return jsonTarget(toJsonNewProjectTypes(cycle, types));
    }

    private List<NewProjectType> getDesignTypes(RequestCycle cycle, MiniOrgInfo org) {
        long bucketId = new GetCourseDesignBucketCommand(cycle).forOrg(org.getId());

        CourseDesignClient cdClient =
                Clients.getClient(cycle, CourseDesignClient.class);
        List<BucketCourseDesignInfo> designs = cdClient.listDesigns(bucketId);

        List<NewProjectType> types = new ArrayList<>(designs.size());

        for (BucketCourseDesignInfo bucketCourseDesignInfo : designs) {
            types.add(new CourseDesignNewProjectType(bucketCourseDesignInfo));
        }

        return types;
    }

    private List<NewProjectType> getSingleProductTypes(RequestCycle cycle, MiniOrgInfo org) {
        List<Product> products
                = OrgMaterialJsonModule.getOrgProducts(cycle, org.getId(), null, null);

        List<Product> sppProducts
                = CollectionsUtil.sublist(products, new Predicate<Product>() {

                    @Override
                    public boolean evalute(Product item) {
                        return "true".equals(item.getProductType().
                                getGlobalMetaValue("cocobox.singleproductproject"));
                    }
                });

        MaterialListFactory mlf = new MaterialListFactory(cycle, CocositeUserHelper.getUserLocale(
                cycle));

        mlf.addProducts(sppProducts);

        List<Material> mats = mlf.getList();

        List<NewProjectType> types = new ArrayList<>(mats.size());

        for (Material material : mats) {
            types.add(new MaterialNewProjectType(SPP_PREFIX, material));
        }

        return types;
    }

    private ByteArrayOutputStream toJsonNewProjectTypes(RequestCycle cycle, List<NewProjectType> types) {
        return new DataTablesJson<NewProjectType>() {

            @Override
            protected void encodeItem(NewProjectType item) throws IOException {
                final JsonGenerator g = generator;

                g.writeStringField("id", item.getId());
                g.writeStringField("name",item.getName());
                g.writeStringField("description",item.getDescription());
                g.writeStringField("thumbnail",item.getThumbnailUrl());
            }
        }.encodeToStream(types);
    }


    public static interface NewProjectType {
        public String getId();
        public String getName();
        public String getDescription();
        public String getThumbnailUrl();
    }

    private static class CourseDesignNewProjectType implements NewProjectType {
        private final BucketCourseDesignInfo design;

        public CourseDesignNewProjectType(BucketCourseDesignInfo design) {
            ParamUtil.required(design, "design");
            this.design = design;
        }

        @Override
        public String getId() {
            return CD_PREFIX + design.getDesignId();
        }

        @Override
        public String getName() {
            return design.getName();
        }

        @Override
        public String getDescription() {
            return design.getDescription();
        }

        @Override
        public String getThumbnailUrl() {
            return new CourseDesignThumbnail(DruwaApplication.getCurrentRequestCycle(), design.
                    getDesignId()).get();
        }
    }

    private static class MaterialNewProjectType implements NewProjectType {
        private final String prefix;
        private final Material material;

        public MaterialNewProjectType(String prefix, Material material) {
            ParamUtil.required(prefix, "prefix");
            ParamUtil.required(material, "material");

            this.prefix = prefix;
            this.material = material;
        }



        @Override
        public String getId() {
            return prefix + material.getId();
        }

        @Override
        public String getName() {
            return material.getTitle();
        }

        @Override
        public String getDescription() {
            return material.getDescription();
        }

        @Override
        public String getThumbnailUrl() {
            return material.getThumbnail(256);
        }

    }
}
