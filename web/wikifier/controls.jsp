<%@page contentType="text/html;charset=UTF-8"%>
<html>
  <head>
    <title>MW Wikifier Controls</title>
    <link rel="stylesheet" href="control.css" type="text/css"/>
    <script type="text/javascript" src="../js/slider.js"></script>
  </head>
  
  
  <% 
	String param = "?task=wikify&showTooltips=true&wrapInXml=false&sourceMode=1" ;
	
	String source = request.getParameter("source") ;
	if (source == null || source.equals("null"))
		source = "" ;

	if (source!=null && !source.trim().equals(""))
		param = param + "&source=" + java.net.URLEncoder.encode(source.trim(), "utf8") ;

	String baseColor=request.getParameter("baseColor") ;
	if (baseColor!=null && !baseColor.trim().equals(""))
		param = param + "&baseColor=" + baseColor ;
		
	String linkColor=request.getParameter("linkColor") ;
	if (linkColor!=null && !linkColor.trim().equals(""))
		param = param + "&linkColor=" + linkColor ;
	
	String minProbability=request.getParameter("minProbability") ;
	if (minProbability!=null && !minProbability.trim().equals(""))
		param = param + "&minProbability=" + minProbability ;

%>
  	
	<script>
		var slider ;

		function init() {
			 	
		 		//choose color
		 		pickColor("<%=linkColor%>") ;
		 		
		 		//setup slider
		 		slider = new Slider("slider", 75, 22); 
		  	slider.onNewPosition = function() {
		  		var value = Math.round(slider.position*100)/100 ;
				if (value < 0.01) value = 0 ;
				if (value > 0.85) value = 1 ;
		  	
		  		document.getElementById("minProbability").value = 1-value ; 
		  	}	
		 		slider.setPosition(1-<%=minProbability%>) ;
		 		document.getElementById("minProbability").value = <%=minProbability%> ; 
				
		
		}
		 	
		var currColor = null ;
		  	
		function pickColor(color) {
		  
		  if (currColor != null) {
		  	var currSwatch = document.getElementById("swatch_" + currColor) ;
		  	currSwatch.style.borderColor = "#f3f4f7";  
		  }
		  		
		  var swatch = document.getElementById("swatch_" + color) ;
		  swatch.style.borderColor = color ; 
		  			
		  var input = document.getElementById("linkColor") ;
		  input.value = color ;
		  			 	
		  currColor = color ;	
		}
		
	</script>
    
  <body onload="init()">
	
  	<form method="get" action="index.jsp" target="_top">
 
 		<a id="wm" href="http://wikipedia-miner.sf.net" target="_top">Wikipedia Miner</a>
 		
 		<div id="wikifier">Wikifier</div>
 		 		
 		<table>
 			<tr>
 				<td class="label">URL</td>
 				<td style="width:100%">
 					<input id="txtSource" type="text" name="source" value="<%=source%>"></input>
 				</td>
 				
 				<td class="label">Link Color</td>
 				<td>
 					<div style = "width: 55px ; padding: 0px ;">
 						<div class="swatch" id="swatch_rgb(255,0,0)" style="background-color:rgb(255,0,0)" onclick="pickColor('rgb(255,0,0)')"></div>
  						<div class="swatch" id="swatch_rgb(0,255,0)" style="background-color:rgb(0,255,0)" onclick="pickColor('rgb(0,255,0)')"></div>
  						<div class="swatch" id="swatch_rgb(0,0,255)" style="background-color:rgb(0,0,255)" onclick="pickColor('rgb(0,0,255)')"></div>
  						<input id="linkColor" type="hidden" name="linkColor"></input>
					</div>				
  					
  				</td>
  				
  				<td class="label">Link Density</td>
  				<td style="padding-left: 5px ;">
					<div class="slider_left"></div>
                                        <div class="slider_right"></div>
 					<div id="slider"></div>
  					<input id="minProbability" type="hidden" name="minProbability"></input>
  				</td>
  				
  				<td class="label">
  					<input id="button" type="submit" value="Wikify"></input>
  				</td>
  			</tr>
  		</table>
  			  		
</form>	

<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
var pageTracker = _gat._getTracker("UA-611266-9");
pageTracker._trackPageview();
</script> 
 		
  </body>
</html>
