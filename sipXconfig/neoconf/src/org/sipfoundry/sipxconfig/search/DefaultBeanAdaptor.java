/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 */
package org.sipfoundry.sipxconfig.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.sipfoundry.sipxconfig.acccode.AuthCode;
import org.sipfoundry.sipxconfig.branch.Branch;
import org.sipfoundry.sipxconfig.callgroup.CallGroup;
import org.sipfoundry.sipxconfig.common.BeanWithId;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.conference.Bridge;
import org.sipfoundry.sipxconfig.conference.Conference;
import org.sipfoundry.sipxconfig.dialplan.AutoAttendant;
import org.sipfoundry.sipxconfig.dialplan.DialingRule;
import org.sipfoundry.sipxconfig.gateway.Gateway;
import org.sipfoundry.sipxconfig.parkorbit.ParkOrbit;
import org.sipfoundry.sipxconfig.phone.Phone;
import org.sipfoundry.sipxconfig.setting.Group;
import org.sipfoundry.sipxconfig.setting.ValueStorage;
import org.sipfoundry.sipxconfig.upload.Upload;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

public class DefaultBeanAdaptor implements BeanAdaptor, BeanFactoryAware {
    /** only those classes can be used to search by class */
    private static final Class[] CLASSES = {
        // TODO: inject externally
        User.class, Phone.class, Group.class, Gateway.class, CallGroup.class, DialingRule.class, Bridge.class,
        Conference.class, ParkOrbit.class, AutoAttendant.class, Upload.class, Branch.class, AuthCode.class
    };

    private static final Log LOG = LogFactory.getLog(DefaultBeanAdaptor.class);

    /**
     * List of fields that will be part of description for found entities
     */
    private static final String[] DESCRIPTION_FIELDS = {
        "description"
    };

    private static final String NAME = "name";

    /**
     * List of fields that will be part of index name
     */
    private static final String[] NAME_FIELDS = {
        "lastName", "firstName", NAME, "extension", "userName", "serialNumber", "host", "code",
        User.FAX_EXTENSION_SETTING, User.DID_SETTING
    };

    /**
     * Remaining fields - they are not displayed in name or description but one can order and
     * search for them.
     *
     * Please note that all String properties are indexed you only need to list a field
     * explicitely if you want to use its name in search queries.
     */
    private static final String[] OTHER_FIELDS = {
        "beanId", "modelId"
    };

    /**
     * Name of the fields that are stored in the index. All the remaining string fields are
     * indexed, but not stored.
     */

    private static final String[] FIELDS;

    /**
     * Sensitive fields contains the list of fields names that should nover be indexed.
     */
    private static final String[] SENSITIVE_FIELDS = {
        "pintoken", "sipPassword"
    };

    static {
        String[] fields = (String[]) ArrayUtils.addAll(NAME_FIELDS, DESCRIPTION_FIELDS);
        FIELDS = (String[]) ArrayUtils.addAll(fields, OTHER_FIELDS);
        Arrays.sort(FIELDS);
        Arrays.sort(SENSITIVE_FIELDS);
    }

    private ListableBeanFactory m_beanFactory;
    private Class[] m_indexedClasses;

    @Override
    public Class[] getIndexedClasses() {
        List<Class> classesList = new ArrayList<Class>(Arrays.asList(CLASSES));
        Map<String, BeanAdaptorPlugin> beanAdaptorPluginsMap = m_beanFactory.getBeansOfType(BeanAdaptorPlugin.class);
        if (beanAdaptorPluginsMap != null) {
            Collection<BeanAdaptorPlugin> beanAdaptorPluginsClasses = beanAdaptorPluginsMap.values();
            if (!beanAdaptorPluginsClasses.isEmpty()) {
                for (BeanAdaptorPlugin beanAdaptorPlugin : beanAdaptorPluginsClasses) {
                    List<Class> indexedClasses = beanAdaptorPlugin.getIndexedClasses();
                    classesList.addAll(indexedClasses);
                }
            }
        }
        return (Class[]) classesList.toArray(new Class[classesList.size()]);
    }

    public void setIndexedClasses(Class[] indexedClasses) {
        m_indexedClasses = indexedClasses;
    }

    /**
     * @return true if the document should be added to index
     */
    public boolean documentFromBean(Document document, Object bean, Serializable id, Object[] state,
            String[] fieldNames, Type[] types) {
        if (!indexClass(document, bean.getClass())) {
            return false;
        }
        document.add(new Field(BeanWithId.ID_PROPERTY, getKeyword(bean, id), Field.Store.YES,
                Field.Index.NOT_ANALYZED));
        for (int i = 0; i < fieldNames.length; i++) {
            Object value = state[i];
            if (value != null) {
                indexField(document, value, fieldNames[i], types[i]);
            }
        }
        return true;
    }

    private boolean indexField(Document document, Object state, String fieldName, Type type) {
        if (Arrays.binarySearch(FIELDS, fieldName) >= 0) {
            // index all fields we know about
            document.add(new Field(fieldName, (String) state, Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field(Indexer.DEFAULT_FIELD, (String) state, Field.Store.NO, Field.Index.ANALYZED));
            return true;
        } else if (type instanceof StringType) {
            // index all strings with the exception of the fields explicitly listed as sensitive
            if (Arrays.binarySearch(SENSITIVE_FIELDS, fieldName) < 0) {
                document
                        .add(new Field(Indexer.DEFAULT_FIELD, (String) state, Field.Store.NO, Field.Index.ANALYZED));
            }
            return true;
        } else if (fieldName.equals("aliases")) {
            Set aliases = (Set) state;
            for (Iterator a = aliases.iterator(); a.hasNext();) {
                String alias = (String) a.next();
                document.add(new Field("alias", alias, Field.Store.NO, Field.Index.ANALYZED));
                document.add(new Field(Indexer.DEFAULT_FIELD, alias, Field.Store.NO, Field.Index.ANALYZED));
            }
            return true;
        }  else if (state instanceof ValueStorage) {
            // handle settings values
            ValueStorage valueStorage = (ValueStorage) state;
            Map databaseValues = valueStorage.getDatabaseValues();
            Set<String> keySet = databaseValues.keySet();
            for (String key : keySet) {
                if (Arrays.binarySearch(FIELDS, key) >= 0) {
                    String value = databaseValues.get(key).toString();
                    document.add(new Field(key, value, Field.Store.YES, Field.Index.ANALYZED));
                    document.add(new Field(Indexer.DEFAULT_FIELD, value, Field.Store.NO, Field.Index.ANALYZED));
                }
            }
            return true;
        } else if (state instanceof IndexedBean) {
            IndexedBean indexedBean = (IndexedBean) state;
            document.add(new Field(NAME, indexedBean.getIndexValue(), Field.Store.YES, Field.Index.ANALYZED));
            document.add(new Field(Indexer.DEFAULT_FIELD,
                    indexedBean.getIndexValue(), Field.Store.NO,
                    Field.Index.ANALYZED));
            return true;
        } else if (fieldName.equals("conditions")) {
            Set conditions = (Set) state;
            for (Iterator a = conditions.iterator(); a.hasNext();) {
                IndexedBean condition = (IndexedBean) a.next();
                document.add(new Field(NAME, condition.getIndexValue(), Field.Store.NO, Field.Index.ANALYZED));
                document.add(new Field(Indexer.DEFAULT_FIELD,
                        condition.getIndexValue(), Field.Store.NO,
                        Field.Index.ANALYZED));
            }
            return true;
        }
        return false;
    }

    public boolean indexClass(Document doc, Class beanClass) {
        for (int i = 0; i < m_indexedClasses.length; i++) {
            Class klass = m_indexedClasses[i];
            if (klass.isAssignableFrom(beanClass)) {
                doc.add(new Field(Indexer.CLASS_FIELD, klass.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
                return true;
            }
        }
        return false;
    }

    private String getKeyword(Object bean, Serializable id) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(bean.getClass().getName());
        buffer.append(':');
        buffer.append(id.toString());
        return buffer.toString();
    }

    public Term getIdentityTerm(Object bean, Serializable id) {
        return new Term(BeanWithId.ID_PROPERTY, getKeyword(bean, id));
    }

    public Identity getBeanIdentity(Document document) {
        try {
            String docId = document.get("id");
            String[] ids = StringUtils.split(docId, ':');
            Class klass = Class.forName(ids[0]);
            Integer id = Integer.valueOf(ids[1]);
            Identity ident = new Identity(klass, id);
            ident.setName(fieldsToString(document, NAME_FIELDS));
            ident.setDescription(fieldsToString(document, DESCRIPTION_FIELDS));
            return ident;
        } catch (NumberFormatException e) {
            LOG.warn("invalid bean id", e);
            return null;
        } catch (ClassNotFoundException e) {
            LOG.warn("invalid bean class", e);
            return null;
        }
    }

    private String fieldsToString(Document doc, String[] fields) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < fields.length; i++) {
            Field field = (Field) doc.getField(fields[i]);
            if (field == null) {
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(field.stringValue());

        }
        return buffer.toString();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        m_beanFactory = (ListableBeanFactory) beanFactory;
        m_indexedClasses = getIndexedClasses();
    }
}
