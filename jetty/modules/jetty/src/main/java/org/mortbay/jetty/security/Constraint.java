// ========================================================================
// Copyright 200-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.security;

import java.io.Serializable;
import java.util.Arrays;


/* ------------------------------------------------------------ */
/** Describe an auth and/or data constraint. 
 *
 * @author Greg Wilkins (gregw)
 */
public class Constraint implements Cloneable, Serializable
{
    /* ------------------------------------------------------------ */
    public final static String __BASIC_AUTH= "BASIC";
    public final static String __FORM_AUTH= "FORM";
    public final static String __DIGEST_AUTH= "DIGEST";
    public final static String __CERT_AUTH= "CLIENT_CERT";
    public final static String __CERT_AUTH2= "CLIENT-CERT";

    /* ------------------------------------------------------------ */
    public final static int DC_UNSET= -1, DC_NONE= 0, DC_INTEGRAL= 1, DC_CONFIDENTIAL= 2;

    /* ------------------------------------------------------------ */
    public final static String NONE= "NONE";
    public final static String ANY_ROLE= "*";

    /* ------------------------------------------------------------ */
    private String _name;
    private String[] _roles;
    private int _dataConstraint= DC_UNSET;
    private boolean _anyRole= false;
    private boolean _authenticate= false;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public Constraint()
    {}

    /* ------------------------------------------------------------ */
    /** Conveniance Constructor. 
     * @param name 
     * @param role 
     */
    public Constraint(String name, String role)
    {
        setName(name);
        setRoles(new String[]{role});
    }

    /* ------------------------------------------------------------ */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name 
     */
    public void setName(String name)
    {
        _name= name;
    }
    
    /* ------------------------------------------------------------ */
    public void setRoles(String[] roles)
    {
        _roles=roles;
        _anyRole=false;
        if (roles!=null)
        for (int i=roles.length;!_anyRole&& i-->0;)
            _anyRole=ANY_ROLE.equals(roles[i]);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if any user role is permitted.
     */
    public boolean isAnyRole()
    {
        return _anyRole;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return List of roles for this constraint.
     */
    public String[] getRoles()
    {
        return _roles;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param role 
     * @return True if the constraint contains the role.
     */
    public boolean hasRole(String role)
    {
        if (_anyRole)
            return true;
        if (_roles!=null)
            for (int i=_roles.length;i-->0;)
                if (role.equals(_roles[i]))
                    return true;
        return false;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param authenticate True if users must be authenticated 
     */
    public void setAuthenticate(boolean authenticate)
    {
        _authenticate= authenticate;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if the constraint requires request authentication
     */
    public boolean getAuthenticate()
    {
        return _authenticate;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if authentication required but no roles set
     */
    public boolean isForbidden()
    {
        return _authenticate && !_anyRole && (_roles==null || _roles.length == 0);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param c Data constrain indicator: 0=DC+NONE, 1=DC_INTEGRAL & 2=DC_CONFIDENTIAL
     */
    public void setDataConstraint(int c)
    {
        if (c < 0 || c > DC_CONFIDENTIAL)
            throw new IllegalArgumentException("Constraint out of range");
        _dataConstraint= c;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Data constrain indicator: 0=DC+NONE, 1=DC_INTEGRAL & 2=DC_CONFIDENTIAL
     */
    public int getDataConstraint()
    {
        return _dataConstraint;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if a data constraint has been set.
     */
    public boolean hasDataConstraint()
    {
        return _dataConstraint >= DC_NONE;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "SC{"
            + _name
            + ","
            + (_anyRole ? "*" : (_roles == null ? "-" : Arrays.asList(_roles).toString()))
            + ","
            + (_dataConstraint == DC_UNSET ? "DC_UNSET}":
               (_dataConstraint == DC_NONE
                ? "NONE}"
                : (_dataConstraint == DC_INTEGRAL ? "INTEGRAL}" : "CONFIDENTIAL}")));
    }

    
}
