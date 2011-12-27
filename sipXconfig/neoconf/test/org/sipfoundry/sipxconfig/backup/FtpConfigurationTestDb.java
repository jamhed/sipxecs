/*
 * Copyright (C) 2010 Avaya, certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 */
package org.sipfoundry.sipxconfig.backup;

import org.dbunit.Assertion;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.sipfoundry.sipxconfig.ftp.FtpConfiguration;
import org.sipfoundry.sipxconfig.test.SipxDatabaseTestCase;
import org.sipfoundry.sipxconfig.test.TestHelper;

public class FtpConfigurationTestDb extends SipxDatabaseTestCase {

    private BackupManager m_backupManager;

    @Override
    protected void setUp() throws Exception {
        m_backupManager = (BackupManager) TestHelper.getApplicationContext().getBean(
                BackupManager.CONTEXT_BEAN_NAME);
    }

    public void testStoreJob() throws Exception {
        TestHelper.cleanInsert("ClearDb.xml");

        FtpBackupPlan ftpPlan = new FtpBackupPlan();
        m_backupManager.storeBackupPlan(ftpPlan);

        ITable actual = TestHelper.getConnection().createDataSet().getTable("backup_plan");

        IDataSet expectedDs = TestHelper.loadDataSetFlat("backup/SaveBackupPlanExpected.xml");

        ReplacementDataSet expectedRds = new ReplacementDataSet(expectedDs);

        expectedRds.addReplacementObject("[backup_plan_id]", ftpPlan.getId());
        expectedRds.addReplacementObject("[null]", null);
        expectedRds.addReplacementObject("[backup_type]","F");

        ITable expected = expectedRds.getTable("backup_plan");

        FtpConfiguration ftpConf = new FtpConfiguration();
        ftpConf.setHost("host");
        ftpConf.setUserId("userId");
        ftpConf.setPassword("password");

        ftpPlan.setFtpConfiguration(ftpConf);

        m_backupManager.storeBackupPlan(ftpPlan);

        ITable actualConf = TestHelper.getConnection().createDataSet().getTable("ftp_configuration");
        IDataSet expectedDsConf = TestHelper.loadDataSetFlat("backup/SaveFtpConfigurationExpected.xml");

        ReplacementDataSet expectedRdsConf = new ReplacementDataSet(expectedDsConf);

        expectedRdsConf.addReplacementObject("[id]",ftpConf.getId());
        expectedRdsConf.addReplacementObject("[host]",ftpConf.getHost());
        expectedRdsConf.addReplacementObject("[user_id]",ftpConf.getUserId());
        expectedRdsConf.addReplacementObject("[password]",ftpConf.getPassword());

        ITable expectedConf = expectedRdsConf.getTable("ftp_configuration");

        Assertion.assertEquals(expected, actual);
        Assertion.assertEquals(expectedConf, actualConf);
    }
}
