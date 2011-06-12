<html><head>
<%@ page import="com.acme.*" %>
</head><body>
<h1>Echo EJB</h1>

<%
   String str = EchoTest.echo();
%>
 <%=str%>
</body></html>
