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
 * Class is the equivalent java class used for holding values from native c code structure passwd. for more information please see man pages for getpwuid and getpwnam
 *   struct passwd {
 *        char    *pw_name;      // user name
 *        char    *pw_passwd;   // user password
 *        uid_t   pw_uid;         // user id
 *        gid_t   pw_gid;         // group id
 *        char    *pw_gecos;     // real name
 *        char    *pw_dir;        // home directory
 *       char    *pw_shell;      // shell program
 *    };
 *
 * @author Leopoldo Lee Agdeppa III
 */

public class Passwd
{
    private String _pwName; /* user name */
    private String _pwPasswd; /* user password */
    private int _pwUid; /* user id */
    private int _pwGid; /* group id */
    private String _pwGecos; /* real name */
    private String _pwDir; /* home directory */
    private String _pwShell; /* shell program */
    

    public String getPwName()
    {
        return _pwName;
    }
    
    public void setPwName(String pwName)
    {
        _pwName = pwName;
    }    

    public String getPwPasswd()
    {
        return _pwPasswd;
    }
    
    public void setPwPasswd(String pwPasswd)
    {
        _pwPasswd = pwPasswd;
    }

    public int getPwUid()
    {
        return _pwUid;
    }
    
    public void setPwUid(int pwUid)
    {
        _pwUid = pwUid;
    }

    public int getPwGid()
    {
        return _pwGid;
    }
    
    public void setPwGid(int pwGid)
    {
        _pwGid = pwGid;
    }
    
    public String getPwGecos()
    {
        return _pwGecos;
    }
    
    public void setPwGid(String pwGecos)
    {
        _pwGecos = pwGecos;
    }
    
    public String getPwDir()
    {
        return _pwDir;
    }
    
    public void setPwDir(String pwDir)
    {
        _pwDir = pwDir;
    }
    
    public String getPwShell()
    {
        return _pwShell;
    }
    
    public void setPwShell(String pwShell)
    {
        _pwShell = pwShell;
    }
    
}
