/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.sipfoundry.sipxconfig.common.DaoUtils;
import org.sipfoundry.sipxconfig.common.Replicable;
import org.sipfoundry.sipxconfig.common.ReplicableProvider;
import org.sipfoundry.sipxconfig.common.SipxHibernateDaoSupport;
import org.sipfoundry.sipxconfig.common.UserException;
import org.sipfoundry.sipxconfig.device.ProfileLocation;
import org.sipfoundry.sipxconfig.dialplan.DialPlanContext;
import org.sipfoundry.sipxconfig.dialplan.DialingRule;
import org.sipfoundry.sipxconfig.logging.AuditLogContext;
import org.sipfoundry.sipxconfig.logging.AuditLogContext.CONFIG_CHANGE_TYPE;
import org.sipfoundry.sipxconfig.sbc.SbcDevice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class GatewayContextImpl extends SipxHibernateDaoSupport implements GatewayContext, BeanFactoryAware,
        ReplicableProvider {
    private static final String QUERY_GATEWAY_ID_BY_SERIAL_NUMBER = "gatewayIdsWithSerialNumber";
    private static final String AUDIT_LOG_CONFIG_TYPE = "Gateway";

    private static class DuplicateNameException extends UserException {
        private static final String ERROR = "&error.duplicateNameException";
        public DuplicateNameException(String name) {
            super(ERROR, name);
        }
    }

    private static class DuplicateSerialNumberException extends UserException {
        private static final String ERROR = "&error.duplicateSerialNumberException";
        public DuplicateSerialNumberException(String name) {
            super(ERROR, name);
        }
    }

    private DialPlanContext m_dialPlanContext;
    private BeanFactory m_beanFactory;
    private AuditLogContext m_auditLogContext;

    public GatewayContextImpl() {
        super();
    }

    public List<Gateway> getGateways() {
        return getHibernateTemplate().loadAll(Gateway.class);
    }

    public Collection<Integer> getAllGatewayIds() {
        return getHibernateTemplate().findByNamedQuery("gatewayIds");
    }

    public Gateway getGateway(Integer id) {
        return (Gateway) getHibernateTemplate().load(Gateway.class, id);
    }

    public FxoPort getPort(Integer id) {
        return (FxoPort) getHibernateTemplate().load(FxoPort.class, id);
    }

    public void saveGateway(Gateway gateway) {
        // Before storing the gateway, make sure that it has a unique name.
        // Throw an exception if it doesn't.
        HibernateTemplate hibernate = getHibernateTemplate();
        DaoUtils.checkDuplicates(hibernate, Gateway.class, gateway, "name",
                new DuplicateNameException(gateway.getName()));
        DaoUtils.checkDuplicates(hibernate, Gateway.class, gateway, "serialNumber",
                new DuplicateSerialNumberException(gateway.getSerialNumber()));
        // Find if we are about to save a new gateway
        boolean isNew = gateway.isNew();
        // Store the updated gateway
        hibernate.saveOrUpdate(gateway);
        hibernate.flush();

        if (isNew) {
            m_auditLogContext.logConfigChange(CONFIG_CHANGE_TYPE.ADDED, AUDIT_LOG_CONFIG_TYPE, gateway.getName());
        } else {
            m_auditLogContext.logConfigChange(CONFIG_CHANGE_TYPE.MODIFIED, AUDIT_LOG_CONFIG_TYPE, gateway.getName());
        }

        SbcDevice sbc = gateway.getSbcDevice();
        if (sbc != null) {
            sbc.generateProfiles(sbc.getProfileLocation());
            sbc.restart();
        }
    }

    public void storePort(FxoPort port) {
        getHibernateTemplate().saveOrUpdate(port);
    }

    public boolean deleteGateway(Integer id) {
        Gateway gateway = getGateway(id);
        ProfileLocation location = gateway.getModel().getDefaultProfileLocation();
        gateway.removeProfiles(location);
        getHibernateTemplate().delete(gateway);
        getDaoEventPublisher().publishDelete(gateway);
        m_auditLogContext.logConfigChange(CONFIG_CHANGE_TYPE.DELETED, AUDIT_LOG_CONFIG_TYPE, gateway.getName());
        return true;
    }

    public void deleteGateways(Collection<Gateway> selectedRows) {
        // remove gateways from rules first
        Collection<Integer> ids = new ArrayList<Integer>();
        for (Gateway gateway : selectedRows) {
            ids.add(gateway.getId());
        }
        m_dialPlanContext.removeGateways(ids);
        Set<SbcDevice> sbcSet = new LinkedHashSet<SbcDevice>(0);
        // remove gateways from the database
        for (Gateway gw : selectedRows) {
            SbcDevice sbc = getGateway(gw.getId()).getSbcDevice();
            // Since this is a set duplicate SBCs will not be added.
            if (sbc != null) {
                sbcSet.add(sbc);
            }
            getDaoEventPublisher().publishDelete(gw);
            deleteGateway(gw.getId());
        }

        getHibernateTemplate().flush();
        for (Iterator i = sbcSet.iterator(); i.hasNext();) {
            SbcDevice sbc = (SbcDevice) i.next();
            sbc.generateProfiles(sbc.getProfileLocation());
            sbc.restart();
        }
    }

    public List<Gateway> getGatewayByIds(Collection<Integer> gatewayIds) {
        List<Gateway> gateways = new ArrayList<Gateway>(gatewayIds.size());
        for (Integer id : gatewayIds) {
            gateways.add(getGateway(id));
        }
        return gateways;
    }

    public <T> List<T> getGatewayByType(final Class<T> type) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session) {
                Criteria criteria = session.createCriteria(type);
                return criteria.list();
            }
        };
        return getHibernateTemplate().executeFind(callback);
    }

    /**
     * Returns the list of gateways available for a specific rule
     *
     * @param ruleId id of the rule for which gateways are checked
     * @return collection of available gateways
     */
    public Collection<Gateway> getAvailableGateways(Integer ruleId) {
        DialingRule rule = m_dialPlanContext.getRule(ruleId);
        if (null == rule) {
            return Collections.EMPTY_LIST;
        }
        List allGateways = getGateways();
        return rule.getAvailableGateways(allGateways);
    }

    public void addGatewaysToRule(Integer dialRuleId, Collection<Integer> gatewaysIds) {
        DialingRule rule = m_dialPlanContext.getRule(dialRuleId);
        for (Integer gatewayId : gatewaysIds) {
            Gateway gateway = getGateway(gatewayId);
            rule.addGateway(gateway);
        }
        m_dialPlanContext.storeRule(rule);
    }

    public void removeGatewaysFromRule(Integer dialRuleId, Collection<Integer> gatewaysIds) {
        DialingRule rule = m_dialPlanContext.getRule(dialRuleId);
        rule.removeGateways(gatewaysIds);
        m_dialPlanContext.storeRule(rule);
    }

    public void clear() {
        List gateways = getHibernateTemplate().loadAll(Gateway.class);
        getHibernateTemplate().deleteAll(gateways);
    }

    public Gateway newGateway(GatewayModel model) {
        Gateway gateway = (Gateway) m_beanFactory.getBean(model.getBeanId(), Gateway.class);
        gateway.setBeanId(model.getBeanId());
        gateway.setModelId(model.getModelId());
        return gateway;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        m_beanFactory = beanFactory;
    }

    public void setDialPlanContext(DialPlanContext dialPlanContext) {
        m_dialPlanContext = dialPlanContext;
    }

    public void setAuditLogContext(AuditLogContext auditLogContext) {
        m_auditLogContext = auditLogContext;
    }

    public void removePortsFromGateway(Integer gatewayId, Collection<Integer> portIds) {
        Gateway gateway = getGateway(gatewayId);
        for (Integer portId : portIds) {
            FxoPort port = getPort(portId);
            gateway.removePort(port);
        }
        getHibernateTemplate().saveOrUpdate(gateway);
    }

    public Integer getGatewayIdBySerialNumber(String serialNumber) {
        List objs = getHibernateTemplate().findByNamedQueryAndNamedParam(QUERY_GATEWAY_ID_BY_SERIAL_NUMBER, "value",
                serialNumber);
        return (Integer) DaoUtils.requireOneOrZero(objs, QUERY_GATEWAY_ID_BY_SERIAL_NUMBER);
    }

    @Override
    public List<Replicable> getReplicables() {
        List<Replicable> replicables = new ArrayList<Replicable>();
        for (Gateway gw : getGateways()) {
            replicables.add(gw);
        }
        return replicables;
    }
}
