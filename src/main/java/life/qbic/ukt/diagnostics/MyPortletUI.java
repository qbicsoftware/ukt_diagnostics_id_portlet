package life.qbic.ukt.diagnostics;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.config.openbis.OpenbisGeneralSettingsConfig;
import life.qbic.ukt.diagnostics.barcode.*;
import life.qbic.ukt.diagnostics.helpers.PortalUtils;
import life.qbic.ukt.diagnostics.portlet.PortletInformation;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;


@Theme("uktdiag")
@SuppressWarnings("serial")
@Component(
        service = UI.class,
        configurationPid = {
                "life.qbic.portal.config.openbis.OpenbisGeneralSettingsConfig"},
        property = {
                "com.liferay.portlet.display-category=qbic",
                "javax.portlet.name=ukt-diagnostics_1.3.0",
                "javax.portlet.display-name=QBiC UKT Diagnostics ID Portlet",
                "javax.portlet.security-role-ref=power-user,user",
                "javax.portlet.resource-bundle=content.Language",
                "com.vaadin.osgi.liferay.portlet-ui=true"},
        scope = ServiceScope.PROTOTYPE)
public class MyPortletUI extends UI {

    /* ----------------------------------------------------------------- */
    /* ----- Global Static Variables ----------------------------------- */
    /* ----------------------------------------------------------------- */
    private static final Log log = LogFactoryUtil.getLog(MyPortletUI.class);

    public static final PortletInformation info = new PortletInformation();

    private static final Boolean testing = !PortalUtils.isLiferayPortlet();


    /* ----------------------------------------------------------------- */
    /* ----- Global Dynamic Variables ---------------------------------- */
    /* ----------------------------------------------------------------- */
    private OpenBisClient openbis;


    /* ----------------------------------------------------------------- */
    /* ----- UI Component References ----------------------------------- */
    /* ----------------------------------------------------------------- */
    private Label footer;


    /* ----------------------------------------------------------------- */
    /* ----- Liferay Configuration Provider ---------------------------- */
    /* ----------------------------------------------------------------- */
    @Reference
    private ConfigurationProvider configProvider;
    private OpenbisGeneralSettingsConfig openbis_config;


    /* ----------------------------------------------------------------------------------------- */
    /* ----- Initialization -------------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */
    @Override
    protected void init(VaadinRequest request) {
        log.info("Initializing " + info.getPortletName() + " " + info.getPortletVersion());
        VerticalLayout content = new VerticalLayout();
        content.setMargin(false);
        setContent(content);

        if (!testing) {
            if (!PortalUtils.userLoggedIn(request)) {
                content.addComponent(PortalUtils.errorLayout("<p>You have to be logged in to use the " + info.getPortletName() + ".</p>", true));
                content.addComponent(getPortletInfoFooter());
                return;

            } else if (!getPortletConfiguration()) {
                content.addComponent(PortalUtils.errorLayout("<p>Could not load configuration. " +
                        "Please notify your systems administrator.</p>", true));
                content.addComponent(getPortletInfoFooter());
                return;
            }

            try {
                openbis = new OpenBisClient(
                        openbis_config.openbisUser(),
                        openbis_config.openbisPassword(),
                        openbis_config.openbisHost());

            } catch (Exception e) {
                content.addComponent(PortalUtils.errorLayout("<p>Could not establish connection to openBIS. " +
                        "Please notify your systems administrator.</p>", true));
                content.addComponent(getPortletInfoFooter());
                return;
            }

            final BarcodeRequestView requestView = new BarcodeRequestViewImpl();
            final BarcodeRequestModel barcodeRequestModel = new BarcodeRequestModelImpl(openbis);
            final BarcodeRequestPresenter barcodeRequestPresenter = new BarcodeRequestPresenter(requestView, barcodeRequestModel, PortalUtils.getNonNullScreenName());

            content.addComponents(requestView.getFullView(), getPortletInfoFooter());
            content.setComponentAlignment(footer, Alignment.MIDDLE_RIGHT);

        } else {
            try {
                String[] creds = getLocalCredentials();

                if (creds == null)
                    throw new FileNotFoundException("Local credentials file not found.");

                openbis = new OpenBisClient( creds[0], creds[1], creds[2]);

                final BarcodeRequestView requestView = new BarcodeRequestViewImpl();
                final BarcodeRequestModel barcodeRequestModel = new BarcodeRequestModelImpl(openbis);
                final BarcodeRequestPresenter barcodeRequestPresenter = new BarcodeRequestPresenter(requestView, barcodeRequestModel, creds[0]);

                content.addComponents(requestView.getFullView(), getPortletInfoFooter());
                content.setComponentAlignment(footer, Alignment.MIDDLE_RIGHT);

            } catch (FileNotFoundException fnfe) {
                content.addComponent(PortalUtils.errorLayout("<p>"+fnfe.getMessage()+"</p>", true));
                content.addComponent(getPortletInfoFooter());

            } catch (Exception e) {
                content.addComponent(PortalUtils.errorLayout(
                        "<p>Could not establish connection to openBIS. " +
                        "Please notify your systems administrator.</p>", true));
                content.addComponent(getPortletInfoFooter());
            }
        }
    }

    private Label getPortletInfoFooter() {
        String info = String.format("%s %s (<a href=\"%s\">%s</a>)",
                MyPortletUI.info.getPortletName(),
                MyPortletUI.info.getPortletVersion(),
                MyPortletUI.info.getPortletRepoURL(),
                MyPortletUI.info.getPortletRepoURL());

        footer = new Label(info, ContentMode.HTML);
        footer.setId("qbic-portlet-info-label");
        footer.addStyleName("portlet-footer");

        return footer;
    }

    private boolean getPortletConfiguration() {

        try {
            this.openbis_config = configProvider.getSystemConfiguration(OpenbisGeneralSettingsConfig.class);
            return true;

        } catch (ConfigurationException e) {
            log.fatal("Cannot access config of portlet.", e);
            return false;
        }
    }


    /* ----------------------------------------------------------------------------------------- */
    /* ----- Local testing helper -------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */
    /**
     * Acquire the openBIS settings from local properties file.
     *
     * @return An array with the credentials
     *      String[0] = loginID
     *      String[1] = password
     *      String[2] = serverURL
     */
    private String[] getLocalCredentials() {
        final String[] credentials = new String[3];

        try {
            final BufferedReader propertiesFile = Files.newBufferedReader(Paths.get("/etc/openbis.properties"));
            final Properties prop = new Properties();
            prop.load(propertiesFile);
            credentials[0] = prop.getProperty("openbisuser");
            credentials[1] = prop.getProperty("openbispw");
            credentials[2] = prop.getProperty("openbisURI");

        } catch (Exception exc){
            log.error(exc);
            return null;
        }

        return credentials;
    }
}
