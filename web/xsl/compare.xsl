<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:variable name="service_name" select="/WikipediaMinerResponse/@service_name"/>
<xsl:variable name="server_path" select="/WikipediaMinerResponse/@server_path"/>

<xsl:template match="/">
  <html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  	<title>Compare | Wikipedia Miner services</title>
  	<link rel="stylesheet" href="css/style.css" type="text/css"/> 
  </head>
  <body>
  
   <div class="box" style="float:right; width:420px; margin-right: 0em ; margin-top: 0em">
		<form method="get" action="{$service_name}">
			<input type="hidden" name="task" value="compare"></input>
			<input type="hidden" name="details" value="true"></input>
			<input type="text" name="term1" style="width: 150px" value="{/WikipediaMinerResponse/RelatednessResponse/@term1}"></input>&#160;
			<input type="text" name="term2" style="width: 150px" value="{/WikipediaMinerResponse/RelatednessResponse/@term2}"></input>&#160;
			<input type="submit" value="Compare" style="width: 75px;"></input>
		</form>
				
		<div class="tl"></div>
		<div class="tr"></div>
		<div class="bl"></div>
		<div class="br"></div>
	</div>
	
	<a href="http://wikipedia-miner.sf.net">Wikipedia Miner</a> | <a href="{$server_path}/{$service_name}"> services</a>  
	
	
	<xsl:choose>
	
		<xsl:when test="WikipediaMinerResponse/RelatednessResponse/@unspecifiedParameters">
	  	
	  	<h1 style="margin-top:5px"><em>Compare</em> Service</h1>
	  	
	  	<p>
	  		Welcome to the Wikipedia Miner compare service, for measuring the semantic relatedness between terms, 
	  		phrases, and Wikipedia concepts. From this you can tell, for example, that New Zealand has more to do with 
	  		<a href="{$service_name}?task=compare&amp;details=true&amp;term1=New Zealand&amp;term2=Rugby">Rugby</a> than 
	  		<a href="{$service_name}?task=compare&amp;details=true&amp;term1=New Zealand&amp;term2=Soccer">Soccer</a>, or that Geeks are more into 
	  		<a href="{$service_name}?task=compare&amp;details=true&amp;term1=Geek&amp;term2=Computer Games">Computer Games</a> than the 
	  		<a href="{$service_name}?task=compare&amp;details=true&amp;term1=Geek&amp;term2=Olympic Games">Olympic Games</a> 
	  	</p>
	  		
	  	<p>
	  		The relatedness measures are calculated from the links going into and out of each if the relevant Wikipedia pages. 
	  		Links that are common to both pages are used as evidence that they are related, while links that are unique to one 
	  		or the other indicate the opposite. 
	  	</p>	
	  	<p>	
	  		The measure is symmetric, so comparing <i>Rugby</i> to <i>Soccer</i> is the same as comparing <i>Soccer</i> to <i>Rugby</i>. 
	  	</p>
	  	
		<div class="note">
                        <p>
                                <em>Note:</em> This service is machine-readable.
                        </p>
                        <p class="small">
                                It can be made to return XML by appending <i>&amp;xml</i> to the request.
                        </p>
                        <p class="small">
                                Feel free to point a bot or a service here (via POST or GET, it doesn't matter). There are some
                                <a href="{$service_name}?task=compare&amp;help">additional parameters</a> available if you do.
                                Bear in mind that we may restrict access if usage becomes excessive.
                                You can always run these services yourself by installing your own version of Wikipedia Miner.
                        </p>
                </div>
  	</xsl:when>
	
	  <xsl:when test="WikipediaMinerResponse/RelatednessResponse/@unknownTerm">
	  
	  	<h1 style="margin-top:0px"><em>Compare</em> Service</h1>
	  	<p>
	  		I have no idea what you mean by <em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@unknownTerm"/></em>.
  		</p>
  	</xsl:when>
  	
  	<xsl:when test="not(/WikipediaMinerResponse/RelatednessResponse/Sense1)">
  		| <a href="{$server_path}/{$service_name}?task=compare">compare service</a> 
  		
  		<h1 style="margin-top:5px">
  			<em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@term1"/></em> and 
 				<em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@term2"/></em> are 
 				<em><xsl:value-of select="round(WikipediaMinerResponse/RelatednessResponse/@relatedness*100)"/>%</em> related.
  		</h1>
  	</xsl:when>	
  	
  	<xsl:otherwise>
  		| <a href="{$server_path}/{$service_name}?task=compare">compare service</a> 
	 		<h1 style="margin-top:5px">
	 			<em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@term1"/></em> and 
	 			<em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@term2"/></em> are 
	 			<em><xsl:value-of select="round(WikipediaMinerResponse/RelatednessResponse/@relatedness*100)"/>%</em> related.
	 		</h1>
	 				
	 		<p>
	 			There are <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/@candidates"/></em> 
	 			things you could have meant by <em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@term1"/></em> 
	 			(click <a href="{$service_name}?task=search&amp;term={WikipediaMinerResponse/RelatednessResponse/@term1}">here</a> to search for them). 
	 			We are assuming you mean 
	 			
	 			<ul class="senses">
					<li>
						<div class="sense">
							<a href="{$service_name}?task=search&amp;id={/WikipediaMinerResponse/RelatednessResponse/Sense1/@id}&amp;term={/WikipediaMinerResponse/RelatednessResponse/Sense1/@title}">
								<xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/@title"/>
							</a>
							<p class="definition"><xsl:copy-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/FirstSentence"/></p>
						</div>
					</li>
				</ul>
			</p>	
			
			<p>
	 			There are <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/@candidates"/></em> 
	 			things you could have meant by <em><xsl:value-of select="WikipediaMinerResponse/RelatednessResponse/@term2"/></em> 
	 			(click <a href="{$service_name}?task=search&amp;term={/WikipediaMinerResponse/RelatednessResponse/@term2}">here</a> to search for them). 
	 			We are assuming you mean 
	 			
	 			<ul class="senses">
					<li>
						<div class="sense">
							<a href="{$service_name}?task=search&amp;id={/WikipediaMinerResponse/RelatednessResponse/Sense2/@id}&amp;term={/WikipediaMinerResponse/RelatednessResponse/Sense2/@title}">
								<xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/@title"/>
							</a>
							<p class="definition"><xsl:copy-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/FirstSentence"/></p>
						</div>
					</li>
				</ul>
			</p>	
			
			
			
			<p>
			The relatedness measure was calculated from the links going into and out of each page. 
			Links that are common to both pages are used as evidence that they are related, while 
			links that are unique to one or the other indicate the opposite.
			</p>
			
			
			
			<h2>Links going out from these pages</h2>
	  	
	  	<h3>
				Shared by both <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/@title"/></em> 
				and <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/@title"/></em>
			</h3>
			<ul class="horizontal">
				<xsl:for-each select="/WikipediaMinerResponse/RelatednessResponse/LinksOut/SharedLinkList/SharedLink">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
					</li>
				</xsl:for-each>
			</ul>
			<div class="break"></div>
			
			<h3>
				Unique to <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/@title"/></em> 
			</h3>
			<ul class="horizontal">
				<xsl:for-each select="/WikipediaMinerResponse/RelatednessResponse/LinksOut/Link1List/Link1">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
					</li>
				</xsl:for-each>
			</ul>
			<div class="break"></div>
					
			<h3>
				Unique to <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/@title"/></em> 
			</h3>
			<ul class="horizontal">
				<xsl:for-each select="/WikipediaMinerResponse/RelatednessResponse/LinksOut/Link2List/Link2">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
					</li>
				</xsl:for-each>
			</ul>
			<div class="break"></div>
			
			
			<h2>Links going into these pages</h2>
			
			<h3>
				Shared by both <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/@title"/></em> 
				and <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/@title"/></em>
			</h3>
			<ul class="horizontal">
				<xsl:for-each select="/WikipediaMinerResponse/RelatednessResponse/LinksIn/SharedLinkList/SharedLink">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
					</li>
				</xsl:for-each>
			</ul>
			<div class="break"></div>
			
			<h3>
				Unique to <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense1/@title"/></em> 
			</h3>
			<ul class="horizontal">
				<xsl:for-each select="/WikipediaMinerResponse/RelatednessResponse/LinksIn/Link1List/Link1">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
					</li>
				</xsl:for-each>
			</ul>
			<div class="break"></div>
			
			<h3>
				Unique to <em><xsl:value-of select="/WikipediaMinerResponse/RelatednessResponse/Sense2/@title"/></em> 
			</h3>
			<ul class="horizontal">
				<xsl:for-each select="/WikipediaMinerResponse/RelatednessResponse/LinksIn/Link2List/Link2">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
					</li>
				</xsl:for-each>
			</ul>
			<div class="break"></div>
			
		</xsl:otherwise>
	</xsl:choose>

 		<script type="text/javascript">
			var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
			document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
		</script>
		<script type="text/javascript">
			var pageTracker = _gat._getTracker("UA-611266-7");
			pageTracker._initData();
			pageTracker._trackPageview();
		</script>



  </body>
  </html>
</xsl:template>

</xsl:stylesheet>
