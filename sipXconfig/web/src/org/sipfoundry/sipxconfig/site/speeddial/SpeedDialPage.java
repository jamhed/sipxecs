/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.site.speeddial;

import java.util.Collection;
import java.util.List;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.annotations.EventListener;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.InjectState;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.event.PageEvent;
import org.sipfoundry.sipxconfig.common.DataCollectionUtil;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.commserver.imdb.RegistrationItem;
import org.sipfoundry.sipxconfig.components.TapestryUtils;
import org.sipfoundry.sipxconfig.device.ProfileManager;
import org.sipfoundry.sipxconfig.permission.PermissionName;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.phone.PhoneContext;
import org.sipfoundry.sipxconfig.registrar.RegistrationContext;
import org.sipfoundry.sipxconfig.site.UserSession;
import org.sipfoundry.sipxconfig.site.user_portal.UserBasePage;
import org.sipfoundry.sipxconfig.speeddial.SpeedDial;
import org.sipfoundry.sipxconfig.speeddial.SpeedDialManager;

public abstract class SpeedDialPage extends UserBasePage {
    public static final String PAGE = "speeddial/SpeedDialPage";

    @InjectObject(value = "spring:speedDialManager")
    public abstract SpeedDialManager getSpeedDialManager();

    @InjectObject(value = "spring:phoneContext")
    public abstract PhoneContext getPhoneContext();

    @InjectObject(value = "spring:registrationContext")
    public abstract RegistrationContext getRegistrationContext();

    @InjectObject(value = "spring:phoneProfileManager")
    public abstract ProfileManager getProfileManager();

    @Override
    @InjectState(value = "userSession")
    public abstract UserSession getUserSession();

    @Persist
    public abstract Integer getSavedUserId();

    public abstract void setSavedUserId(Integer savedUserId);

    public abstract SpeedDial getSpeedDial();

    public abstract void setSpeedDial(SpeedDial speedDial);

    public abstract void setValidationEnabled(boolean enabled);

    public abstract boolean isValidationEnabled();

    public abstract boolean isGroupSynced();

    public abstract void setGroupSynced(boolean groupSynced);

    public abstract boolean isSubscribeToPresenceDisabled();

    public abstract void setSubscribeToPresenceDisabled(boolean subscribeToPresenceDisabled);

    public abstract User getLoadedUser();

    public abstract void setLoadedUser(User user);

    @Override
    public void pageBeginRender(PageEvent event) {
        super.pageBeginRender(event);
        if (getLoadedUser() == null) {
            setLoadedUser(getUser());
        }

        Integer userId = getUserId();
        if (userId.equals(getSavedUserId()) && getSpeedDial() != null) {
            // same user and we have cached buttons - nothing to do
            return;
        }

        SpeedDial speedDial = getSpeedDialManager().getSpeedDialForUserId(userId, true);
        setSpeedDial(speedDial);
        setSavedUserId(userId);
        setGroupSynced(!getSpeedDialManager().isSpeedDialDefinedForUserId(userId));
        UserSession user = getUserSession();
        if (!user.isAdmin()) {
            setSubscribeToPresenceDisabled(!getLoadedUser().hasPermission(PermissionName.SUBSCRIBE_TO_PRESENCE));
        }
    }

    @EventListener(events = "onclick", targets = "groupSync")
    public void addNumberActivation(IRequestCycle cycle) {
        cycle.getResponseBuilder().updateComponent("render");
    }

    public void onSubmit() {
        // XCF-1435 - Unless attempting to save data (e.g. onApply and the like)
        // clear all form errors
        // A.) user is probably not done and errors are disconcerting
        // B.) tapestry rewrites form values that are invalid on the button move operations
        // NOTE: This relies on the fact the the form listener is called BEFORE AND IN ADDITION TO
        // the button listener.
        if (!isValidationEnabled()) {
            TapestryUtils.getValidator(this).clearErrors();
        }
    }

    public void onApply() {
        setValidationEnabled(true);
        if (!TapestryUtils.isValid(this)) {
            return;
        }

        if (isGroupSynced()) {
            getSpeedDialManager().speedDialSynchToGroup(getLoadedUser());
            // force reload
            setSpeedDial(null);
        } else {
            SpeedDialManager speedDialManager = getSpeedDialManager();
            speedDialManager.saveSpeedDial(getSpeedDial());
        }
    }

    public void onUpdatePhones() {
        setValidationEnabled(true);
        if (TapestryUtils.isValid(this)) {
            onApply();
            Collection<Phone> phones = getPhoneContext().getPhonesByUserId(getUserId());
            List<RegistrationItem> registrations = getRegistrationContext().getRegistrationsByLineId(
                    getUser().getUserName());
            for (RegistrationItem reg : registrations) {
                Phone phone = getPhoneContext().getPhoneBySerialNumber(reg.getInstrument());
                if (phone != null) {
                    phones.add(phone);
                }
            }
            Collection<Integer> ids = DataCollectionUtil.extractPrimaryKeys(phones);
            getProfileManager().generateProfiles(ids, true, null);
        }
    }

}
