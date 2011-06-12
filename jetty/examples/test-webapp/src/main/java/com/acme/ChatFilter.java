//========================================================================
//$Id: ChatFilter.java,v 1.4 2005/11/14 11:00:33 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package com.acme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mortbay.util.ajax.AjaxFilter;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * @author gregw
 * @deprecated Use Cometd
 */
public class ChatFilter extends AjaxFilter
{       
    private Map chatrooms;
    

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.ajax.AjaxFilter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        chatrooms=new HashMap();
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.ajax.AjaxFilter#destroy()
     */
    public void destroy()
    {
        super.destroy();
        chatrooms.clear();
        chatrooms=null;
    }


    /* ------------------------------------------------------------ */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpSession session = ((HttpServletRequest)request).getSession(true);
        super.doFilter(request,response,chain);
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.ajax.AjaxFilter#handle(java.lang.String, javax.servlet.http.HttpServletRequest, org.mortbay.ajax.AjaxFilter.AjaxResponse)
     */
    public void handle(String method, String message, HttpServletRequest request, AjaxResponse response)
    {
        request.getSession(true);
        
        String roomName=request.getParameter("room");
        if (roomName==null)
            roomName="0";
        Map room = null;
        synchronized(this)
        {
            room=(Map)chatrooms.get(roomName);
            if (room==null)
            {
                room=new HashMap();
                chatrooms.put(roomName,room);
            }
        }
            
        if ("join".equals(method))
            doJoinChat(room,message,request, response);
        else if ("chat".equals(method))
            doChat(room,message,request,response);
        else if ("poll".equals(method))
            doPoll(room,request,response);
        else if ("leave".equals(method))
            doLeaveChat(room,message,request,response);
        else
            super.handle(method, message,request, response);   
    }

    /* ------------------------------------------------------------ */
    private void doJoinChat(Map room, String name, HttpServletRequest request, AjaxResponse response)
    {
        HttpSession session = request.getSession(true);
        String id = session.getId();
        if (name==null || name.length()==0)
            name="Newbie";
        Member member=null;
        
        synchronized (room)
        {
            member=(Member)room.get(id);
            if (member==null)
            {
                member = new Member(session,name);
                room.put(session.getId(),member);
            }
            else
                member.setName(name);
            
            // exists already, so just update name
            sendMessage(room,member,"has joined the chat",true);
            
            //response.objectResponse("joined", "<joined from=\""+name+"\"/>");
            sendMembers(room,response);
        }
    }
    

    /* ------------------------------------------------------------ */
    private void doLeaveChat(Map room, String name, HttpServletRequest request, AjaxResponse response)
    {
        HttpSession session = request.getSession(true);
        String id = session.getId();

        Member member=null;
        synchronized (room)
        {
            member = (Member)room.get(id);
            if (member==null)
                return;
            if ("Elvis".equals(member.getName()))
                sendMessage(room,member,"has left the building",true);
            else
                sendMessage(room,member,"has left the chat",true);
            room.remove(id);
            member.setName(null);
        }
        //response.objectResponse("left", "<left from=\""+member.getName()+"\"/>");
        sendMembers(room,response);
    }


    /* ------------------------------------------------------------ */
    private void doChat(Map room, String text, HttpServletRequest request, AjaxResponse response)
    {
        HttpSession session = request.getSession(true);
        String id = session.getId();
        
        Member member=null;
        synchronized (room)
        {
            member = (Member)room.get(id);
            
            if (member==null)
                return;
            sendMessage(room, member, text, false);
        }
    }


    /* ------------------------------------------------------------ */
    private void doPoll(Map room, HttpServletRequest request, AjaxResponse response)
    {
        HttpSession session = request.getSession(true);
        String id = session.getId();
        long timeoutMS = 60000L; 
        if (request.getParameter("timeout")!=null)
            timeoutMS=Long.parseLong(request.getParameter("timeout"));
        if (session.isNew())
            timeoutMS=1;
        
        Member member=null;
        synchronized (room)
        {
            member = (Member)room.get(id);
            if (member==null)
            {
                member = new Member(session,null);
                room.put(session.getId(),member);
            }

            Continuation continuation = ContinuationSupport.getContinuation(request, room);
            
            if (!member.hasMessages())
            {   
                if (member.getContinuation()!=null && member.getContinuation()!=continuation)
                {
                    // duplicate frames!
                    Message duplicate = new Message("System","Multiple frames/tabs/windows from same browser!",true);
                    Message action = new Message("System","Please use only one frame/tab/window",true);
                    member.addMessage(duplicate);
                    member.addMessage(action);
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch(Exception e)
                    {}
                }
                else
                {
                    member.setContinuation(continuation);
                    continuation.suspend(timeoutMS);
                }
            }
            
            if (member.getContinuation()==continuation)
                member.setContinuation(null);

            
            if (member.sendMessages(response))
                sendMembers(room,response);
        }
        
    }

    /* ------------------------------------------------------------ */
    private void sendMessage(Map room, Member member, String text, boolean alert)
    {
        Message event=new Message(member.getName(),text,alert);
        
        ArrayList invalids=null;
        synchronized (room)
        {
            Iterator iter = room.values().iterator();
            while (iter.hasNext())
            {
                Member m = (Member)iter.next();
                
                try 
                {
                    m.getSession().getAttribute("anything");
                    m.addMessage(event);
                }
                catch(IllegalStateException e)
                {
                    if (invalids==null)
                        invalids=new ArrayList();
                    invalids.add(m);
                    iter.remove();
                }
            }
        }
            
        for (int i=0;invalids!=null && i<invalids.size();i++)
        {
            Member m = (Member)invalids.get(i);
            sendMessage(room,m,"has timed out of the chat",true);
        }
    }
    
    private void sendMembers(Map room, AjaxResponse response)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<ul>\n");
        synchronized (room)
        {
            Iterator iter = room.values().iterator();
            while (iter.hasNext())
            {
                Member m = (Member)iter.next();
                if (m.getName()==null)
                    continue;
                buf.append("<li>");
                buf.append(encodeText(m.getName()));
                buf.append("</li>\n");
            }
        }
        buf.append("</ul>\n");
        response.elementResponse("members", buf.toString());
    }

    
    private static class Message
    {
        private String _from;
        private String _text;
        private boolean _alert;
        
        Message(String from, String text, boolean alert)
        {
            _from=from;
            _text=text;
            _alert=alert;
        }
        
        boolean isAlert()
        {
            return _alert;
        }
        
        public String toString()
        {
            return "<chat from=\""+_from+"\" alert=\""+_alert+"\">"+encodeText(_text)+"</chat>";
        }
        
        
        
    }

    private class Member
    {
        private HttpSession _session;
        private String _name;
        private List _messages = new ArrayList();
        private Continuation _continuation;
        
        Member(HttpSession session, String name)
        {
            _session=session;
            _name=name;
        }
        
        /* ------------------------------------------------------------ */
        /**
         * @return Returns the name.
         */
        public String getName()
        {
            return _name;
        }

        /* ------------------------------------------------------------ */
        /**
         * @param name The name to set.
         */
        public void setName(String name)
        {
            _name = name;
        }

        /* ------------------------------------------------------------ */
        /**
         * @return Returns the session.
         */
        public HttpSession getSession()
        {
            return _session;
        }


        /* ------------------------------------------------------------ */
        /**
         * @param continuation The continuation to set.
         */
        public Continuation getContinuation()
        {
            return _continuation;
        }
        
        /* ------------------------------------------------------------ */
        /**
         * @param continuation The continuation to set.
         */
        public void setContinuation(Continuation continuation)
        {
            if (continuation!=null && _continuation!=null && _continuation!=continuation)
                _continuation.resume();
            _continuation=continuation;
        }
        
        /* ------------------------------------------------------------ */
        public void addMessage(Message event)
        {
            if (_name==null)
                return;
            _messages.add(event);
            if (_continuation!=null)
                _continuation.resume();
        }

        /* ------------------------------------------------------------ */
        public boolean hasMessages()
        {
            return _messages!=null && _messages.size()>0;
        }
        
        /* ------------------------------------------------------------ */
        public void rename(Map room,String name)
        {
            String oldName = getName();
            setName(name);
            if (oldName!=null)
                ChatFilter.this.sendMessage(room,this,oldName+" has been renamed to "+name,true);
        }

        /* ------------------------------------------------------------ */
        public boolean sendMessages(AjaxResponse response)
        {
            synchronized (this)
            {
                boolean alerts=false;
                for (int i=0;i<_messages.size();i++)
                {
                    Message event = (Message)_messages.get(i);
                    response.objectResponse("chat", event.toString());
                    alerts |= event.isAlert();
                }
                _messages.clear();
                return alerts;
            }
        }

    }
}
