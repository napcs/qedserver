package org.mortbay.jetty.servlet.wadi;


public interface SessionListener
{
    /**
     * Calls when the ownership of the provided Session is acquired by the SessionManager to which this listener
     * is attached.
     * 
     * @param session New Session now owned by the attached SessionManager.
     */
    void notifyInboundSessionMigration(WadiSession session);
    
    /**
     * Calls when the ownership of the provided Session is relinquished to another SessionManager.
     * 
     * @param session Session now owned by another SessionManager.
     */
    void notifyOutboundSessionMigration(WadiSession session);
    
    /**
     * Calls when a Session is destroyed.
     * 
     * @param session Destroyed session.
     */
    void notifySessionDestruction(WadiSession session);

}
