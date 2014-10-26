/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.core;

import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.FormBeanContext;
import net.unixdeveloper.druwa.formbean.FormValidationSession;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.ServiceClientFactory;
import se.dabox.dws.client.ApiHelper;
import se.dabox.service.branding.client.BrandingClient;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.context.DwsExecutionContext;
import se.dabox.service.common.context.DwsExecutionContextHelper;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.cr.ContentRepoClient;
import se.dabox.service.common.cr.DwsContentRepoClient;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClientImpl;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.randdata.client.RandomDataClient;
import se.dabox.service.webutils.druwa.DwsWebsiteConstants;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public abstract class AbstractModule {

    private final FormBeanContext formBeanContext;
    protected final DwsExecutionContext executionContext;

    public AbstractModule() {
        formBeanContext = DruwaApplication.get().getAttribute(
                DwsWebsiteConstants.FORMBEAN_CONTEXT);
        executionContext = DwsExecutionContextHelper.getContext();
    }

    public FormBeanContext getFormBeanContext() {
        return formBeanContext;
    }

    public <T> DruwaFormValidationSession<T> getValidationSession(Class<T> clazz,
            RequestCycle cycle) {
        FormValidationSession<T> session =
                getFormBeanContext().getValidationSession(clazz);

        return new DruwaFormValidationSession<>(cycle, session);
    }

    protected Map<String,Object> createMap() {
        final Map<String, Object> map = new HashMap<>();

        return map;
    }

    public String getConfValue(RequestCycle cycle, String name, String defaultValue) {
        return DwsRealmHelper.getRealmConfiguration(cycle).getValue(name, defaultValue);
    }

    public String getConfValue(RequestCycle cycle, String name) {
        return DwsRealmHelper.getRealmConfiguration(cycle).getValue(name);
    }

    public static ServiceClientFactory getServiceClientFactory(RequestCycle cycle) {
        return new ServiceClientFactory(cycle);
    }

    public ContentRepoClient getContentRepoClient() {
        return new DwsContentRepoClient();
    }

    public static CocoboxCoordinatorClient getCocoboxCordinatorClient(
            RequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    public static ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient(
            RequestCycle cycle) {
        return CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
    }

    public static RandomDataClient getRandomDataClient(RequestCycle cycle) {
        return CacheClients.getClient(cycle, RandomDataClient.class);
    }

    public static MailTemplateServiceClient getMailTemplateClient(RequestCycle cycle) {
        ApiHelper helper = DwsRealmHelper.getRealmApiHelper(cycle);
        String serviceUrl = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                CocoSiteConstants.MAILTEMPLATE_URL);

        return new MailTemplateServiceClientImpl(helper, serviceUrl);
    }

    public static BrandingClient getBrandingClient(RequestCycle cycle) {
        return Clients.getClient(cycle, BrandingClient.class);
    }

    public static ProductDirectoryClient getProductDirectoryClient(RequestCycle cycle) {
        return CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }
    
}
