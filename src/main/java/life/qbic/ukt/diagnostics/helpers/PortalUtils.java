package life.qbic.ukt.diagnostics.helpers;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import java.util.Enumeration;


public class PortalUtils {
    private static final Log logger = LogFactoryUtil.getLog(PortalUtils.class);

    public enum NotificationType {
        ERROR, SUCCESS, DEFAULT
    }


    /* ----------------------------------------------------------------------------------------- */
    /* ----- Constructor ----------------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */
    private PortalUtils() {}


    /* ----------------------------------------------------------------------------------------- */
    /* ----- User related methods -------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */
    public static User getUser() {
        final String user = VaadinService.getCurrentRequest().getRemoteUser();

        try {
            return user == null ? null : UserLocalServiceUtil.getUser(Long.parseLong(user));

        } catch (PortalException pe) {
            logger.fatal("Liferays UserLocalServiceUtil went down the drain...");
            return null;
        } catch (NumberFormatException nfe) {
            logger.fatal("VaadinService returned non-long remote user... " + user);
            return null;
        }
    }

    public static String getScreenName() {
        final User user = getUser();
        return user == null ? null : user.getScreenName();
    }

    public static String getNonNullScreenName() {
        final String screenName = getScreenName();
        return screenName == null ? "Anonymous" : screenName;
    }

    public static boolean userLoggedIn() {
        return VaadinService.getCurrentRequest().getRemoteUser() != null;
    }

    public static boolean userLoggedIn(VaadinRequest request) {
        return request.getRemoteUser() != null;
    }


    /* ----------------------------------------------------------------------------------------- */
    /* ----- Validation methods -------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */
    public static boolean isLiferayPortlet() {
        VaadinSession.getCurrent().getService();
        Object PORTLET_ID = VaadinService.getCurrentRequest().getAttribute("PORTLET_ID");

        if (logger.isInfoEnabled()) {
            logger.info("PORTLET_ID = " + PORTLET_ID);
            final Enumeration<String> attributeNames = VaadinService.getCurrentRequest().getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                final String attributeName = attributeNames.nextElement();
                // logger.info("  {} = {}", attributeName, VaadinService.getCurrentRequest().getAttribute(attributeName));
            }
        }

        return PORTLET_ID != null;
    }


    /* ----------------------------------------------------------------------------------------- */
    /* ----- Layout/UI methods ----------------------------------------------------------------- */
    /* ----------------------------------------------------------------------------------------- */
    public static void notification(String title, String description, NotificationType type) {
        Notification notify = new Notification(title, description);
        notify.setPosition(Position.TOP_CENTER);
        switch (type) {
            case ERROR:
                notify.setDelayMsec(16000);
                notify.setIcon(VaadinIcons.EXCLAMATION_CIRCLE_O);
                notify.setStyleName(ValoTheme.NOTIFICATION_ERROR + " " + ValoTheme.NOTIFICATION_CLOSABLE);
                break;
            case SUCCESS:
                notify.setDelayMsec(8000);
                notify.setIcon(VaadinIcons.SMILEY_O);
                notify.setStyleName(ValoTheme.NOTIFICATION_SUCCESS + " " + ValoTheme.NOTIFICATION_CLOSABLE);
                break;
            default:
                notify.setDelayMsec(8000);
                notify.setIcon(VaadinIcons.INFO_CIRCLE_O);
                notify.setStyleName(ValoTheme.NOTIFICATION_TRAY + " " + ValoTheme.NOTIFICATION_CLOSABLE);
                break;
        }
        notify.show(Page.getCurrent());
    }

    public static Layout errorLayout(String msg, boolean html) {
        Label l = new Label(msg);
        l.setContentMode( html ? ContentMode.HTML : ContentMode.TEXT );
        l.addStyleNames("bigger", "redder");

        VerticalLayout v = new VerticalLayout();
        v.setSizeFull();
        v.setMargin(true);
        v.addComponent(l);
        v.setComponentAlignment(l, Alignment.MIDDLE_CENTER);

        return v;
    }
}
