//========================================================================
//Copyright 2006-2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.client.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mortbay.jetty.client.HttpDestination;

/**
 * Simple Realm Resolver.
 * <p> A Realm Resolver that wraps a single realm.
 * @author gregw
 *
 */
public class SimpleRealmResolver implements RealmResolver
{
    private Realm _realm;
    
    public SimpleRealmResolver( Realm realm )
    {
        _realm=realm;
    }
    
    public Realm getRealm( String realmName, HttpDestination destination, String path ) throws IOException
    {
        return _realm;
    }
}
