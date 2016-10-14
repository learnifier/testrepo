/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.DeferredFileOutputStream;
import se.dabox.cocobox.cpweb.module.report.StatusHolder;
import se.dabox.cocobox.cpweb.module.report.subproject.transformer.FetchExtendedActivityStatus;
import se.dabox.cocobox.cpweb.module.report.subproject.transformer.FetchMasterProjectId;
import se.dabox.cocobox.cpweb.module.report.subproject.transformer.FetchOrgProjectParticipants;
import se.dabox.cocobox.cpweb.module.report.subproject.transformer.FetchProjectNames;
import se.dabox.cocobox.cpweb.module.report.subproject.transformer.FetchUserDetails;
import se.dabox.cocobox.cpweb.module.report.subproject.transformer.SubprojectParticipant;
import se.dabox.service.common.RealmBackgroundCallable;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ajaxlongrun.StatusSource;
import se.dabox.service.common.json.DwsJacksonObjectMapperFactory;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.collections.Factory;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class OrgSubprojectReport extends RealmBackgroundCallable<DeferredFileOutputStream>
        implements StatusSource, StatusHolder {

    private static final ObjectMapper MAPPER = new DwsJacksonObjectMapperFactory().newInstance();

    private static final int MAXSIZE = 32 * 1024 * 1024;

    private final Product product;
    private final long orgId;
    private volatile Status status;

    public OrgSubprojectReport(DruwaApplication app, long orgId, Product product) {
        super(app);
        this.orgId = orgId;
        this.product = product;
    }

    @Override
    protected DeferredFileOutputStream callInCycle(ServiceRequestCycle cycle) throws IOException {
        Factory<List<SubprojectParticipant>> participantListFactory = createFactory();

        List<SubprojectParticipant> list = participantListFactory.create();

        File file = File.createTempFile("orgsubp", ".json.tmp");

        try {
            DeferredFileOutputStream fos = new DeferredFileOutputStream(MAXSIZE, file);

            Map<String, List<SubprojectParticipant>> map = Collections.singletonMap("list", list);

            MAPPER.writeValue(fos, map);

            fos.close();

            return fos;
        } catch (IOException ex) {
            FileUtils.deleteQuietly(file);
            throw ex;
        }
    }

    private Factory<List<SubprojectParticipant>> createFactory() {
        Factory<List<SubprojectParticipant>> participantListFactory
                = new FetchOrgProjectParticipants(this, orgId, product.getId());

        participantListFactory = new FetchMasterProjectId(this, participantListFactory);

        participantListFactory = new FetchUserDetails(this, participantListFactory);

        participantListFactory = new FetchProjectNames<>(this,
                participantListFactory,
                (p) -> p.getMasterProject(),
                (p, name) -> p.setMasterProjectName(name));

        participantListFactory = new FetchExtendedActivityStatus(this, participantListFactory);

        return participantListFactory;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

}
