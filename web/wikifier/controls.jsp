<%@page contentType="text/html;charset=UTF-8"%>
<html>
  <head>
    <title>MW Wikifier Controls</title>
    <link rel="stylesheet" href="control.css" type="text/css"/>
    <link type="text/css" href="../css/smoothness/jquery-ui-1.7.1.custom.css" rel="stylesheet" />	
    
    <script type="text/javascript" src="../js/jquery-1.3.2.min.js"></script>
	<script type="text/javascript" src="../js/jquery-ui-1.7.1.custom.min.js"></script>
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
			
		$(function(){
		
			pickColor("<%=linkColor%>") ;
		
			$('#slider').slider(
				{value: Math.round((1-<%=minProbability%>)*100) , step: 10, change: function(event, ui){
					$("#minProbability").val((100-ui.value)/100) ;
				}
			});				
		});
		 	
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
 					<div style = "width: 60px ; padding: 0px ;">
 						<div class="swatch" id="swatch_rgb(255,0,0)" style="background-color:rgb(255,0,0)" onclick="pickColor('rgb(255,0,0)')">&nbsp;&nbsp;&nbsp;</div>
  						<div class="swatch" id="swatch_rgb(0,255,0)" style="background-color:rgb(0,255,0)" onclick="pickColor('rgb(0,255,0)')">&nbsp;&nbsp;&nbsp;</div>
  						<div class="swatch" id="swatch_rgb(0,0,255)" style="background-color:rgb(0,0,255)" onclick="pickColor('rgb(0,0,255)')">&nbsp;&nbsp;&nbsp;</div>
  						<input id="linkColor" type="hidden" name="linkColor"></input>
					</div>				
  					
  				</td>
  				
  				<td class="label">Link Density</td>
  				<td style="padding-left: 5px ;">
					<div id="slider" style="width:100px"></div>
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
