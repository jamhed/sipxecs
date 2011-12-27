/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.backup;

import java.util.Date;

import org.dbunit.Assertion;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.sipfoundry.sipxconfig.test.SipxDatabaseTestCase;
import org.sipfoundry.sipxconfig.test.TestHelper;

public class DailyBackupScheduleTestDb extends SipxDatabaseTestCase {

    private BackupManager m_backupManager;

    @Override
    protected void setUp() throws Exception {
        m_backupManager = (BackupManager) TestHelper.getApplicationContext().getBean(
                BackupManager.CONTEXT_BEAN_NAME);
    }

    public void testStoreJob() throws Exception {
        TestHelper.cleanInsert("ClearDb.xml");

        BackupPlan plan = m_backupManager.getBackupPlan(LocalBackupPlan.TYPE);
        BackupPlan ftpPlan = m_backupManager.getBackupPlan(FtpBackupPlan.TYPE);
        DailyBackupSchedule dailySchedule = new DailyBackupSchedule();
        DailyBackupSchedule ftpDailySchedule = new DailyBackupSchedule();

        plan.addSchedule(dailySchedule);
        ftpPlan.addSchedule(ftpDailySchedule);

        m_backupManager.storeBackupPlan(plan);
        m_backupManager.storeBackupPlan(ftpPlan);


        ITable actual = TestHelper.getConnection().createDataSet().getTable("daily_backup_schedule");

        IDataSet expectedDs = TestHelper.loadDataSetFlat("backup/SaveDailyBackupScheduleExpected.xml");
        ReplacementDataSet expectedRds = new ReplacementDataSet(expectedDs);
        expectedRds.addReplacementObject("[backup_plan_id]", plan.getId());
        expectedRds.addReplacementObject("[daily_backup_schedule_id]", dailySchedule.getId());
        expectedRds.addReplacementObject("[time_of_day]", new Date(0));
        expectedRds.addReplacementObject("[null]", null);

        expectedRds.addReplacementObject("[backup_plan_id2]", ftpPlan.getId());
        expectedRds.addReplacementObject("[daily_backup_schedule_id2]", ftpDailySchedule.getId());
        expectedRds.addReplacementObject("[time_of_day2]", new Date(0));
        expectedRds.addReplacementObject("[null]", null);


        ITable expected = expectedRds.getTable("daily_backup_schedule");

        Assertion.assertEquals(expected, actual);
    }

}
