// ========================================================================
// Copyright 2006-2007 Sabre Holdings.
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

package org.mortbay.jetty.ant.types;

import java.util.ArrayList;
import java.util.List;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * Specifies a jetty configuration <connectors/> element for Ant build file.
 * 
 * @author Jakub Pawlowicz
 */
public class Connectors
{

    public static final List DEFAULT_CONNECTORS = new ArrayList();

    static
    {
        org.mortbay.jetty.Connector defaultConnector = new SelectChannelConnector();
        defaultConnector.setPort(8080);
        defaultConnector.setMaxIdleTime(30000);
        DEFAULT_CONNECTORS.add(defaultConnector);
    }

    private List connectors = new ArrayList();

    public void add(Connector connector)
    {
        connectors.add(connector);
    }

    public List getConnectors()
    {
        return connectors;
    }
}
