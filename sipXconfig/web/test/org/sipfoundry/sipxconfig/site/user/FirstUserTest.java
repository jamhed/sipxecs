/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 */
package org.sipfoundry.sipxconfig.site.user;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.tapestry.test.Creator;
import org.sipfoundry.sipxconfig.common.CoreContext;
import org.sipfoundry.sipxconfig.components.TapestryContext;

public class FirstUserTest extends TestCase {
    private Creator m_pageMaker = new Creator();

    public void testGetRenderLicense() throws Exception {
        TapestryContext tapestry = createMock(TapestryContext.class);
        expect(tapestry.isLicenseRequired()).andReturn(false).andReturn(true).andReturn(true);
        expect(tapestry.getLicensesNumber()).andReturn(0).andReturn(1).andReturn(1).andReturn(2).andReturn(2).andReturn(2);
        expect(tapestry.getLicense(0)).andReturn("license text");
        expect(tapestry.getLicense(0)).andReturn("license text 1");
        expect(tapestry.getLicense(1)).andReturn("license text 2");

        CoreContext coreContext = createMock(CoreContext.class);
        expect(coreContext.getUsersCount()).andReturn(0).times(3);

        replay(tapestry, coreContext);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("tapestry", tapestry);
        properties.put("coreContext", coreContext);
        FirstUser page = (FirstUser) m_pageMaker.newInstance(FirstUser.class, properties);

        page.pageBeginRender(null);
        assertFalse(page.getRenderLicense());
        assertEquals(page.getLicensesNumber().size(), 0);

        page.pageBeginRender(null);
        assertTrue(page.getRenderLicense());
        assertEquals(page.getLicensesNumber().size(), 1);
        assertEquals(page.getLicense(0), "license text");

        page.pageBeginRender(null);
        assertTrue(page.getRenderLicense());
        assertEquals(page.getLicensesNumber().size(), 2);
        assertEquals(page.getLicense(0), "license text 1");
        assertEquals(page.getLicense(1), "license text 2");

        verify(tapestry, coreContext);
    }

    public void testGetRenderLicenseAccepted() throws Exception {
        TapestryContext tapestry = createMock(TapestryContext.class);
        expect(tapestry.isLicenseRequired()).andReturn(false).andReturn(true);
        CoreContext coreContext = createMock(CoreContext.class);
        expect(coreContext.getUsersCount()).andReturn(0).times(2);

        replay(tapestry, coreContext);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("tapestry", tapestry);
        properties.put("licenseAccepted", true);
        properties.put("coreContext", coreContext);

        FirstUser page = (FirstUser) m_pageMaker.newInstance(FirstUser.class, properties);
        page.pageBeginRender(null);
        assertFalse(page.getRenderLicense());

        page.pageBeginRender(null);
        assertFalse(page.getRenderLicense());

        verify(tapestry, coreContext);
    }
}
