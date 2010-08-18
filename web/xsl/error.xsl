<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
  <html>
  <head>
  	<title>Error | Wikipedia Miner services</title>
  	<link rel="stylesheet" href="css/style.css" type="text/css"/> 
  </head>
  <body>
			<p class="centered">
				<em>Ooops!</em> Looks like we have a problem. 
			</p>
			
			<p>
			  <em>Message:</em> <xsl:value-of select="/Error/@message"/>
			</p> 
			
			<xsl:if test="/Error/StackTrace">
				<p>
			  	<em>StackTrace:</em>
				</p> 
				
				<ul>
					<xsl:for-each select="/Error/StackTrace/StackTraceElement">
						<li><xsl:value-of select="@message"/></li>				
					</xsl:for-each>
				</ul>
			</xsl:if>
  
  </body>
  </html>
</xsl:template>

</xsl:stylesheet>
