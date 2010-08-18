<%@page contentType="text/html;charset=UTF-8"%>

<% 
	String param = "?task=wikify&showTooltips=true&wrapInXml=false&sourceMode=1" ;
	
	String source = request.getParameter("source") ;
	if (source!=null && !source.trim().equals(""))
		param = param + "&source=" + java.net.URLEncoder.encode(source.trim(), "utf8") ;

	String baseColor=request.getParameter("baseColor") ;
	if (baseColor!=null && !baseColor.trim().equals(""))
		param = param + "&baseColor=" + baseColor ;
		
	String linkColor=request.getParameter("linkColor") ;
	if (linkColor!=null && !linkColor.trim().equals(""))
		param = param + "&linkColor=" + linkColor ;
	else
		param = param + "&linkColor=rgb(255,0,0)" ;
	
	String minProbability=request.getParameter("minProbability") ;
	if (minProbability!=null && !minProbability.trim().equals(""))
		param = param + "&minProbability=" + minProbability ;
	else
		param = param + "&minProbability=0.5" ;
		
	String mainFrame ;
	if (source!=null && !source.trim().equals(""))
		mainFrame = "../service" + param ;
	else 
		mainFrame = "blank.html" ;
	
	
		
%>

<html>
	<head>
		<title>WikipediaMiner | topic detection demo</title>
	</head>
		<frameset rows="50px,*" frameborder="yes">
			<frame src="controls.jsp<%=param%>" scrolling="no" noresize/>
			<frame src="<%=mainFrame%>" marginwidth=0 marginheight=0 noresize /> 
		</frameset>
</html>
