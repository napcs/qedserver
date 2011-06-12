// ========================================================================
// Copyright 2002-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.setuid;

/**
 * Class is the equivalent java class used for holding values from native c code structure group. for more information please see man pages for getgrnam and getgrgid
 * struct group {
 *             char   *gr_name;        // group name 
 *             char   *gr_passwd;     // group password
 *             gid_t   gr_gid;          // group ID 
 *             char  **gr_mem;        //  group members 
 *         };
 *
 * @author Leopoldo Lee Agdeppa III
 */

public class Group
{
    private String _grName; /* group name */
    private String _grPasswd; /* group password */
    private int _grGid; /* group id */
    private String[] _grMem; /* group members */
    
    

    public String getGrName()
    {
        return _grName;
    }
    
    public void setGrName(String grName)
    {
        _grName = grName;
    }    

    public String getGrPasswd()
    {
        return _grPasswd;
    }
    
    public void setGrPasswd(String grPasswd)
    {
        _grPasswd = grPasswd;
    }

    public int getGrGid()
    {
        return _grGid;
    }
    
    public void setGrGid(int grGid)
    {
        _grGid = grGid;
    }
    
    public String[] getGrMem()
    {
        return _grMem;
    }
    
    public void setGrMem(String[] grMem)
    {
        _grMem = grMem;
    }
    
}
