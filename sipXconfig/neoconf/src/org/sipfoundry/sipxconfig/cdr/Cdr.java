/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.cdr;

import java.util.Date;

import org.sipfoundry.sipxconfig.admin.dialplan.CallTag;
import org.sipfoundry.sipxconfig.common.SipUri;

public class Cdr {
    public enum Termination {
        UNKNOWN, REQUESTED, IN_PROGRESS, COMPLETED, FAILED, TRANSFER;

        public static Termination fromString(String t) {
            switch (t.charAt(0)) {
            case 'R':
                return REQUESTED;
            case 'C':
                return COMPLETED;
            case 'F':
                return FAILED;
            case 'I':
                return IN_PROGRESS;
            case 'U':
                return UNKNOWN;
            case 'T':
                return TRANSFER;
            default:
                return UNKNOWN;
            }
        }
    }

    public static final String CALL_INCOMING = "INCOMING";
    public static final String CALL_OUTGOING = "OUTGOING";
    public static final String CALL_TANDEM = "TANDEM";
    public static final String CALL_INTERNAL = "INTERNAL";
    public static final String CALL_UNKNOWN = "UNKNOWN";
    public static final String CALL_FAILED = "FAILED/ABANDONED";

    public static final String CALL_LOCAL = "LOCAL";
    public static final String CALL_MOBILE = "MOBILE";
    public static final String CALL_EMERGENCY = "EMERGENCY";
    public static final String CALL_CUST = "CUSTOM";

    private String m_callerAor;
    private String m_calleeAor;

    private Date m_startTime;
    private Date m_connectTime;
    private Date m_endTime;

    private Termination m_termination;
    private int m_failureStatus;

    private String m_caller;

    private String m_callee;

    private String m_callid;
    private String m_reference;
    private boolean m_callerInternal;
    private String m_calleeRoute;

    public String getCalleeAor() {
        return m_calleeAor;
    }

    public String getCallee() {
        return m_callee;
    }

    public void setCalleeAor(String calleeAor) {
        m_calleeAor = calleeAor;
        m_callee = SipUri.extractUser(calleeAor);
    }

    public String getCallerAor() {
        return m_callerAor;
    }

    public String getCaller() {
        return m_caller;
    }

    public void setCallerAor(String callerAor) {
        m_callerAor = callerAor;
        m_caller = SipUri.extractFullUser(callerAor);
    }

    public Date getConnectTime() {
        return m_connectTime;
    }

    public void setConnectTime(Date connectTime) {
        m_connectTime = connectTime;
    }

    public Date getEndTime() {
        return m_endTime;
    }

    public void setEndTime(Date endTime) {
        m_endTime = endTime;
    }

    public int getFailureStatus() {
        return m_failureStatus;
    }

    public void setFailureStatus(int failureStatus) {
        m_failureStatus = failureStatus;
    }

    public Date getStartTime() {
        return m_startTime;
    }

    public void setStartTime(Date startTime) {
        m_startTime = startTime;
    }

    public Termination getTermination() {
        return m_termination;
    }

    public void setTermination(Termination termination) {
        m_termination = termination;
    }

    public long getDuration() {
        if (m_endTime == null || m_connectTime == null) {
            return 0;
        }
        return m_endTime.getTime() - m_connectTime.getTime();
    }

    public String getCallId() {
        return m_callid;
    }

    public void setCallId(String callid) {
        m_callid = callid;
    }


    public String getReference() {
        return m_reference;
    }

    public void setReference(String reference) {
        m_reference = reference;
    }

    public boolean getCallerInternal() {
        return m_callerInternal;
    }

    public void setCallerInternal(boolean callerinternal) {
        m_callerInternal = callerinternal;
    }

    public String getCalleeRoute() {
        return m_calleeRoute;
    }

    public void setCalleeRoute(String calleeroute) {
        m_calleeRoute = calleeroute;
    }

    public String getCallDirection() {
        String direction = CALL_UNKNOWN;
        if (getTermination() == Termination.FAILED) {
            direction = CALL_FAILED;
        } else if (m_callerInternal) {
            direction = CALL_OUTGOING;
            if (m_calleeRoute != null) {
                if (m_calleeRoute.endsWith(CallTag.INT.toString())  
                    || m_calleeRoute.endsWith(CallTag.AA.toString()) 
                    || m_calleeRoute.endsWith(CallTag.VM.toString()) 
                    || m_calleeRoute.endsWith(CallTag.VMR.toString())
                    || m_calleeRoute.endsWith(CallTag.PAGE.toString())
                    || m_calleeRoute.endsWith(CallTag.AL.toString())) {
                    direction = CALL_INTERNAL;
                }
            }
        } else {
            direction = CALL_INCOMING;
            if (m_calleeRoute != null) {
                if (m_calleeRoute.endsWith(CallTag.STS.toString()) 
                    || m_calleeRoute.endsWith(CallTag.LD.toString()) 
                    || m_calleeRoute.endsWith(CallTag.TF.toString()) 
                    || m_calleeRoute.endsWith(CallTag.REST.toString()) 
                    || m_calleeRoute.endsWith(CallTag.LOCL.toString()) 
                    || m_calleeRoute.endsWith(CallTag.INTN.toString()) 
                    || m_calleeRoute.endsWith(CallTag.EMERG.toString()) 
                    || m_calleeRoute.endsWith(CallTag.MOB.toString()) 
                    || m_calleeRoute.endsWith(CallTag.CUST.toString())) {
                    direction = CALL_TANDEM;
                }
            }
        }
        return direction; 
    }
        
    public String getCallTypeName() {
        String callType = CallTag.UNK.getName();
        if (m_calleeRoute.endsWith(CallTag.AA.toString())) {
            callType = CallTag.AA.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.CUST.toString())) {
            callType = CallTag.CUST.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.EMERG.toString())) {
            callType = CallTag.EMERG.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.INTN.toString())) {
            callType = CallTag.INTN.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.LD.toString())) {
            callType = CallTag.LD.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.LOCL.toString())) {
            callType = CallTag.LOCL.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.MOH.toString())) {
            callType = CallTag.MOH.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.PAGE.toString())) {
            callType = CallTag.PAGE.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.RL.toString())) {
            callType = CallTag.RL.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.REST.toString())) {
            callType = CallTag.REST.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.STS.toString())) {
            callType = CallTag.STS.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.MOB.toString())) {
            callType = CallTag.MOB.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.TF.toString())) {
            callType = CallTag.TF.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.VM.toString())) {
            callType = CallTag.VM.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.VMR.toString())) {
            callType = CallTag.VMR.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.AL.toString())) {
            callType = CallTag.AL.getName();
        }
        if (m_calleeRoute.endsWith(CallTag.INT.toString())) {
            callType = CallTag.INT.getName();
        }
        return callType; 
    }
        

}
