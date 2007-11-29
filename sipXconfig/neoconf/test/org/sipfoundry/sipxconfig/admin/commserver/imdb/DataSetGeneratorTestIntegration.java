package org.sipfoundry.sipxconfig.admin.commserver.imdb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.sipfoundry.sipxconfig.IntegrationTestCase;

public class DataSetGeneratorTestIntegration extends IntegrationTestCase {
    
    private Map<String, DataSetGenerator> m_dataSetGeneratorMap;
    
    public DataSetGeneratorTestIntegration() {
        m_dataSetGeneratorMap = new HashMap<String, DataSetGenerator>();
    }
    
    public void testDataSetGeneratorBeans() {
        for (Iterator i = DataSet.iterator(); i.hasNext();) {
            DataSet set = (DataSet) i.next();
            // need to fix this test.  caller-alias bean can not be injected
            // automatically (setter method is commented out below)
            if ("caller-alias".equals(set.getName())) {
                continue;
            }
            DataSetGenerator gen = m_dataSetGeneratorMap.get(set.getName());
            assertNotNull(set.getName(), gen);
            assertNotNull(set.getName(), gen.getCoreContext());
        }
    }
    
    public void setExtensionDataSet(Extensions extensions) {
        m_dataSetGeneratorMap.put(DataSet.EXTENSION.getName(), extensions);
    }
    
    public void setCredentialDataSet(Credentials credentials) {
        m_dataSetGeneratorMap.put(DataSet.CREDENTIAL.getName(), credentials);
    }
    
    public void setAliasDataSet(Aliases aliases) {
        m_dataSetGeneratorMap.put(DataSet.ALIAS.getName(), aliases);
    }
    
    public void setPermissionDataSet(Permissions permissions) {
        m_dataSetGeneratorMap.put(DataSet.PERMISSION.getName(), permissions);
    }
}
