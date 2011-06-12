package org.mortbay.jetty.servlet.wadi;

import java.util.Map;

public interface WadiSession
{
        
    /**
     * Gets the sessionId.
     * 
     * @return sessionId.
     */
    String getSessionId();

    /**
     * Map like contract to manipulate state information.
     */
    Object addState(String key, Object value);

    /**
     * Map like contract to manipulate state information.
     */
    Object getState(String key);

    /**
     * Map like contract to manipulate state information.
     */
    Object removeState(String key);
    
    /**
     * Map like contract to manipulate state information.
     * <p>
     * The returned Map is mutable and is backed by the session.
     */
    Map getState();
    
    /**
     * Releases the session.
     * <p>
     * When a Session is released, it is released from the underlying set of SessionManagers. In other words, its
     * sessionId is unknown and its state is permanently lost. After the release of a Session, the behavior of
     * the other methods is undefined.
     */
    void release();
    
    /**
     * Notifies the session that state accesses are now completed. 
     * <p>
     * When state accesses end, the underlying local SessionManager may decide to replicate synchronously or
     * asynchronously the current state to remote SessionManagers.
     */
    void onEndAccess();
}
