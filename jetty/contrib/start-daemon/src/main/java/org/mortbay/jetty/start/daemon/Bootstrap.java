package org.mortbay.jetty.start.daemon;

//========================================================================
//Copyright 2003-2005 Mort Bay Consulting Pty. Ltd.
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

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.mortbay.start.Main;

public class Bootstrap implements Daemon {

  private Main main = new Main();

  public void init(DaemonContext context) {
    main.init(context.getArguments());
  }

  public void start() {
    main.start();
  }

  public void stop() {
    System.exit(0);
  }

  public void destroy() {
    System.exit(0);
  }
}

