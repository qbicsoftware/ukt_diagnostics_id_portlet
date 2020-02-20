package life.qbic;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

public class OpenBisSession {

    public String url;
    public String user;
    public String pw;
    public String token;

    public IApplicationServerApi api;

    OpenBisSession(String url, String user, String pw) {
        this.url = url + IApplicationServerApi.SERVICE_URL;
        this.user = user;
        this.pw = pw;
        api = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, this.url, 10000);
        token = api.login(this.user, this.pw);
    }

}
