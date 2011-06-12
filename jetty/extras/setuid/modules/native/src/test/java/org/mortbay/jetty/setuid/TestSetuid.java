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

/**
 * JUnit test for SetUID Class, used to check if native code is working.
 * @author Leopoldo Lee Agdeppa III
 */

package org.mortbay.jetty.setuid;

import junit.framework.TestCase;
import org.mortbay.setuid.SetUID;
import java.io.File;
import org.mortbay.setuid.Passwd;
import org.mortbay.setuid.Group;

public class TestSetuid extends TestCase
{

    public void testSetuid() throws Exception
    {
             
    
        try
        {        
            
            File lib = new File("target/libsetuid.so");
            String libPath = lib.getCanonicalPath();
            System.setProperty("jetty.libsetuid.path", libPath);   
            
            
            try
            {
                SetUID.getpwnam("TheQuickBrownFoxJumpsOverToTheLazyDog");
                assertTrue(false);
            }
            catch(SecurityException se)
            {
                assertTrue(true);
            }
            
            
            try
            {
                SetUID.getpwuid(-9999);
                assertTrue(false);
            }
            catch(SecurityException se)
            {
                assertTrue(true);
            }
            
            
            
            
            // get the passwd info of root
            Passwd passwd1 = SetUID.getpwnam("root");
            // get the roots passwd info using the aquired uid
            Passwd passwd2 = SetUID.getpwuid(passwd1.getPwUid());
            
            
            assertEquals(passwd1.getPwName(), passwd2.getPwName());
            assertEquals(passwd1.getPwPasswd(), passwd2.getPwPasswd());
            assertEquals(passwd1.getPwUid(), passwd2.getPwUid());
            assertEquals(passwd1.getPwGid(), passwd2.getPwGid());
            assertEquals(passwd1.getPwGecos(), passwd2.getPwGecos());
            assertEquals(passwd1.getPwDir(), passwd2.getPwDir());
            assertEquals(passwd1.getPwShell(), passwd2.getPwShell());
            
            
            try
            {
                SetUID.getgrnam("TheQuickBrownFoxJumpsOverToTheLazyDog");
                assertTrue(false);
            }
            catch(SecurityException se)
            {
                assertTrue(true);
            }
            
            
            try
            {
                SetUID.getgrgid(-9999);
                assertTrue(false);
            }
            catch(SecurityException se)
            {
                assertTrue(true);
            }
            
            
            
            
            // get the group using the roots groupid
            Group gr1 = SetUID.getgrgid(passwd1.getPwGid());
            // get the group name using the aquired name
            Group gr2 = SetUID.getgrnam(gr1.getGrName());
            
            assertEquals(gr1.getGrName(), gr2.getGrName());
            assertEquals(gr1.getGrPasswd(), gr2.getGrPasswd());
            assertEquals(gr1.getGrGid(), gr2.getGrGid());
            
            // search and check through membership lists
            if(gr1.getGrMem() != null)
            {
                assertEquals(gr1.getGrMem().length, gr2.getGrMem().length);
                for(int i=0; i<gr1.getGrMem().length; i++)
                {
                    assertEquals(gr1.getGrMem()[i], gr2.getGrMem()[i]);
                }
            }
            
            
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            assertTrue(false);
        }
    }    
    



}
