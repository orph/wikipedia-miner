<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:variable name="service_name" select="/WikipediaMinerResponse/@service_name"/>
<xsl:variable name="server_path" select="/WikipediaMinerResponse/@server_path"/>

<xsl:template match="/">

  <html>
  <script>
		var serviceName = <xsl:value-of select="/WikipediaMinerResponse/@service_name"/> ;
		var serverPath = <xsl:value-of select="/WikipediaMinerResponse/@server_path"/> ;
		var query = <xsl:value-of select="/WikipediaMinerResponse/HoparaResponse/@internalQuery"/> ;
	</script>
  
  <head>
  	<title>loading (<xsl:value-of select="round(//loading/@progress*100)"/>%) | Wikipedia Miner services</title>
  	<link rel="stylesheet" href="css/style.css" type="text/css"/> 
  	<script type="text/javascript" src="js/loading.js"></script>
  </head>
  <body onload="init(&quot;{//loading/@progress}&quot;,&quot;{$server_path}&quot;,&quot;{$service_name}&quot;);">
  
  	<a href="http://wikipedia-miner.sf.net">Wikipedia Miner</a> | <a href="{$server_path}/{$service_name}"> services</a>  
		  
  	<xsl:if test="/WikipediaMinerResponse/@task"> 
	  	&#160;|&#160;<a href="{$server_path}/{$service_name}?help">help</a>
	  	<h1 style="text-transform:capitalize;margin-top:0px"> <em><xsl:value-of select="/WikipediaMinerResponse/Description/@task"/></em> Service</h1>  	
  	</xsl:if>
  	
	<div style="width: 520px">
  		<h1 style="margin-top:0px">Loading required data</h1>
  		
		<p class="progress">
			<div id="progressBar"></div>
			<div id="progressLabel"></div>
		</p>
						
		<p> 
			Don't worry, you won't have to wait through this delay every time. 
			It only occurs when the service has been interrupted or hasn't been used in a while. 
		</p>
		<p>			
			The page should update automatically. If not, then just <a onclick="window.location.reload(true)">refresh it</a>. 
		</p>  
  	</div>
  </body>
  </html>
</xsl:template>

</xsl:stylesheet>
