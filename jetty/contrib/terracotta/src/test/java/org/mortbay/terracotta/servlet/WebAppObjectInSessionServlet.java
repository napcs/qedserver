// ========================================================================
// Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.terracotta.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @version $Revision: 1319 $ $Date: 2008-11-14 10:55:54 +1100 (Fri, 14 Nov 2008) $
 */
public class WebAppObjectInSessionServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        try
        {
            String action = request.getParameter("action");
            if ("set".equals(action))
            {
                HttpSession session = request.getSession(true);
                session.setAttribute("staticAttribute", new TestSharedStatic());
//                session.setAttribute("objectAttribute", new TestSharedNonStatic());
                // The session itself is not shareable, since the implementation class
                // refers to the session manager via the hidden field this$0, and
                // it seems there is no way to mark the hidden field as transient.
//                session.setAttribute("sessionAttribute", session);
            }
            else if ("get".equals(action))
            {
                HttpSession session = request.getSession(false);
                Object staticAttribute = session.getAttribute("staticAttribute");
                assert staticAttribute instanceof TestSharedStatic;
//                Object objectAttribute = session.getAttribute("objectAttribute");
//                assert objectAttribute instanceof TestSharedNonStatic;
//                Object sessionAttribute = session.getAttribute("sessionAttribute");
//                assert sessionAttribute instanceof HttpSession;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Non static inner classes are not shareable, because even if this class is portable,
    // the hidden field this$0 refers to the servlet, which is a non portable class.
    public class TestSharedNonStatic
    {
    }

    // Static inner classes are shareable
    public static class TestSharedStatic
    {
    }
}
