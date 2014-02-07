/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.badge;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.BytesRequestTarget;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.cocosite.converter.cds.CdsUtil;
import se.dabox.dws.client.ApiHelper;
import se.dabox.service.common.DwsConstants;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterialLink;
import se.dabox.service.common.cds.ContentDeliveryServiceClient;
import se.dabox.service.common.cds.ContentDeliveryServiceClientImpl;
import se.dabox.service.common.cds.Endpoint;
import se.dabox.service.common.context.DwsExecutionContext;
import se.dabox.service.common.context.DwsExecutionContextHelper;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.context.command.LazyCacheConfigurationValueCmd;
import se.dabox.service.common.io.RuntimeIOException;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/jsb")
public class ContentBadgeModule extends AbstractModule {
    private final DwsExecutionContext dwsExecutionContext;

    public ContentBadgeModule() {
        dwsExecutionContext = DwsExecutionContextHelper.getContext();
        ParamUtil.required(dwsExecutionContext, "dwsExecutionContext");
    }

    @DefaultWebAction
    @WebAction
    @WebActionMountpoint("/jsbadge")
    public RequestTarget onServe(RequestCycle cycle, String linkId) throws IOException, TemplateException {
        return serveBadge(cycle, linkId, "/jsbadge.ftl");
    }

    @WebAction
    public RequestTarget onB(RequestCycle cycle, String linkId) throws IOException, TemplateException {
        return serveBadge(cycle, linkId, "/jsbadge-styleb.ftl");
    }

    private Configuration getNewConfig(final String realmId) throws IOException {
        final se.dabox.service.common.context.Configuration cfg =
                getRealmConfig(realmId);

        return new LazyCacheConfigurationValueCmd<Configuration>(cfg).get("fmconfig",
                new Transformer<String, Configuration>() {

                    @Override
                    public Configuration transform(String obj) {
                        try {
                            String basedir = cfg.getValue("contentdelivery.dir");
                            if (basedir == null) {
                                throw new IllegalStateException(
                                        "Failed to get config value contentdelivery.dir");
                            }

                            if (!basedir.endsWith(File.separator)) {
                                basedir += File.separator;
                            }

                            TemplateLoader realmLoader = new FileTemplateLoader(new File(basedir
                                    + realmId));
                            TemplateLoader globalLoader = new FileTemplateLoader(new File(basedir
                                    + "global"));
                            TemplateLoader loader = new MultiTemplateLoader(new TemplateLoader[]{
                                        realmLoader,
                                        globalLoader});

                            Configuration config = new Configuration();
                            config.setTemplateLoader(loader);

                            return config;
                        } catch (IOException ex) {
                            throw new RuntimeIOException("Failed to get freemarker config", ex);
                        }
                    }
                });
    }

    private se.dabox.service.common.context.Configuration getRealmConfig(final String realmId) {
        final se.dabox.service.common.context.Configuration cfg =
                dwsExecutionContext.getConfiguration(realmId);
        return cfg;
    }

    private WriterOutput getWriterOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new BufferedWriter(new OutputStreamWriter(baos, DwsConstants.UTF8_CHARSET),
                128);

        return new WriterOutput(baos, writer);
    }

    private Endpoint getEndpoint(RequestCycle cycle, String linkId) {
        return getCDSClient(cycle).getEndpointByLinkId(linkId);
    }

    private ContentDeliveryServiceClient getCDSClient(RequestCycle cycle) {
        ApiHelper apiHelper = DwsRealmHelper.getRealmApiHelper(cycle);
        String url = DwsRealmHelper.getRealmConfiguration(cycle).getValue("contentdelivery.url");

        return new ContentDeliveryServiceClientImpl(apiHelper, url);
    }

    private RequestTarget serveBadge(RequestCycle cycle, String linkId, String jsbadgeftl) {
        try {
            Endpoint endpoint = getEndpoint(cycle, linkId);

            if (endpoint == null) {
                return new ErrorCodeRequestTarget(404);
            }

            //TODO: This is not right. We need the realm from the endpoint somehow
            String realmId = DwsRealmHelper.determineRequestRealm(cycle);
            //We have the link, let's build the badge

            CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
            OrgMaterialLink link = ccbc.getOrgMatLinkByLinkid(linkId);
            OrgMaterial orgmat = ccbc.getOrgMaterial(link.getOrgMaterialId());

            Configuration config = getNewConfig(realmId);

            Template template = config.getTemplate("/jsbadge.ftl");
            WriterOutput wo = getWriterOutput();

            HashMap<String, Object> map = new HashMap<>();
            map.put("endpoint", endpoint);
            map.put("link", link);
            map.put("orgmat", orgmat);
            map.put("downloadurl",
                    CdsUtil.getDeliveryBase(cycle).concat(linkId));
            map.put("resUrl", getRealmConfig(realmId).getValue("cocobox.cdnurl"));
            map.put("scriptid", "ccbjs-" + linkId);

            template.process(map, wo.getWriter());
            IOUtils.closeQuietly(wo.getWriter());

            return new BytesRequestTarget(wo.getOutputStream().toByteArray(), "text/javascript");
        } catch (TemplateException | IOException ex) {
            throw new RuntimeIOException("Failed to generate badge", ex);
        }
    }
}
