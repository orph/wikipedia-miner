<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:variable name="service_name" select="/WikipediaMinerResponse/@service_name"/>
<xsl:variable name="server_path" select="/WikipediaMinerResponse/@server_path"/>

<xsl:template match="/">
 <html>
  <head>
  	<title>Help | Wikipedia Miner services</title>
  	<link rel="stylesheet" href="css/style.css" type="text/css"/> 
  </head>
  <body>
  
  	<a href="http://wikipedia-miner.sf.net">Wikipedia Miner</a> | <a href="{$server_path}/{$service_name}"> services</a>  
		  
  	<xsl:choose>
  	<xsl:when test="/WikipediaMinerResponse/Description/@task"> 
	  	&#160;|&#160;<a href="{$server_path}/{$service_name}?help">help</a>
	  	<h1 style="text-transform:capitalize;margin-top:5px"> <em><xsl:value-of select="/WikipediaMinerResponse/Description/@task"/></em> Service</h1>  	
  	</xsl:when>
  	<xsl:otherwise>
  		<h1 style="margin-top:5px">Help</h1>
  	</xsl:otherwise>
  	</xsl:choose>
 
  	<xsl:copy-of select="/WikipediaMinerResponse/Description/Details" />
  	
  	<h2>Parameters</h2>
  	
  	<ul>
  		<xsl:for-each select="/WikipediaMinerResponse/Description/ParameterGroup">
  		
  			<li>
  				<i style="font-size:1.25em">
  					<xsl:choose> <xsl:when test="position()=1">either</xsl:when><xsl:otherwise>or</xsl:otherwise></xsl:choose>
  				</i>
  				<ul>
  					<xsl:for-each select="Parameter">
  	
  						<li class="parameter">
  							<em><xsl:value-of select="@name"/></em>
  							<xsl:if test="@optional">&#160;<i style="font-size:0.75em">(optional)</i></xsl:if>
								<p><xsl:copy-of select="."/></p> 
								<xsl:if test="@default">
									<p><i>default:</i>&#160;<xsl:value-of select="@default"/></p>
								</xsl:if>
							</li>	
  					</xsl:for-each>
  				</ul>
  			</li> 
  		</xsl:for-each>
  	
  		<xsl:for-each select="/WikipediaMinerResponse/Description/Parameter">
  	
  			<li class="parameter">
  				<em><xsl:value-of select="@name"/></em>
  				<xsl:if test="@optional">&#160;<i style="font-size:0.75em">(optional)</i></xsl:if>
					<p><xsl:copy-of select="."/></p> 
					<xsl:if test="@default">
						<p><i>default:</i>&#160;<xsl:value-of select="@default"/></p>
					</xsl:if>
				</li>	
  		</xsl:for-each>
  	</ul>
  </body>
 </html>
</xsl:template>  

</xsl:stylesheet>
