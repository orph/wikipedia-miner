<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0" encoding="utf-8" indent="yes"/>

<xsl:variable name="service_name" select="/WikipediaMinerResponse/@service_name"/>
<xsl:variable name="server_path" select="/WikipediaMinerResponse/@server_path"/>

<xsl:template match="/WikipediaMinerResponse/WikifierResponse">
	

	

<html>
	<head>
		<title>Wikipedia Miner wikifier service</title>
		<link rel="stylesheet" href="css/style.css" type="text/css"/> 
		<link rel="stylesheet" href="css/wikify.css" type="text/css"/> 
		<script type="text/javascript" src="js/wikify.js"></script>
		<script type="text/javascript" src="js/slider.js"></script>
		<script>
			var sourceMode=<xsl:value-of select="@sourceMode"/> ;
			var repeatMode=<xsl:value-of select="@repeatMode"/> ;
			var minProbability = <xsl:value-of select="@minProbability"/> ;
		</script>
		
	</head>
	<body onload="init()" onresize="resize()">
		
		<a href="http://wikipedia-miner.sf.net">Wikipedia Miner</a> | <a href="{$server_path}/{$service_name}"> services</a>
		
		<h1 style="margin-top:0px"> <em>Wikify</em> Service</h1>
	  	
	  	<p>
	  		Welcome to the Wikipedia Miner <em>wikify</em> service. This service takes snippets of text 
			(plain text, html, or mediawiki markup) and 
			augments them with links to the relevant Wikipedia articles. 
		</p>
		<p>
			 If you want to wikify an entire web page, go <a href="wikifier/" target="_top">here</a>. 
		</p>

		<form name="form" method="post" action="{service_name}">
			<input type="hidden" name="task" value="wikify"></input>
			<input type="hidden" name="wrapInXml" value="true"></input>
			<input type="hidden" name="showTooltips" value="false"></input>
				
				
			<div name="container" class="container">
				<p><em>Text to Wikify</em></p>	
				<textarea id="inputBox" name="source" class="inputBox"><xsl:value-of select="Source"/></textarea>
				
				
				<a id="showOptions" onclick="showOptions()">options</a>
				<a id="hideOptions" onclick="hideOptions()">options</a>
			
				<div id="options">
					<fieldset title="What should happen when the same topic is mentioned multiple times?">
						<legend>Markup</legend> 
					
						<input id="sourceMode_html" name="sourceMode" type="radio" value="2"></input>
						<label for="sourceMode_html">HTML</label> 
						<input id="sourceMode_wiki" name="sourceMode" type="radio" value="3"></input>
						<label for="sourceMode_wiki">MediaWiki</label> 
						<input id="sourceMode_auto" name="sourceMode" type="radio" value="0"></input>
						<label for="sourceMode_auto">detect automatically</label> 
					</fieldset>
					
					<fieldset  title="What should happen when the same topic is mentioned multiple times?">
						<legend>Repeat occurences</legend>
					
						<input id="repeatMode_all" name="repeatMode" type="radio" value="0"></input>
						<label for="repeatMode_all">mark all</label> 
						<input id="repeatMode_first" name="repeatMode" type="radio" value="1"></input>
						<label for="repeatMode_first">mark first</label> 
						<input id="repeatMode_fis" name="repeatMode" type="radio" value="2"></input>
						<label for="repeatMode_fis">mark first in each section</label> 
					</fieldset>
					
					<fieldset title="How strict should we be when deciding which topics to link to?">
						<legend>Link density</legend>
						
						<table>
							<tr>
								<td style="vertical-align:middle">few links</td>
								<td>
									<div class="slider_left"></div>
									<div class="slider_right"></div>
									<div id="slider" class="slider"></div>
								</td>
								<td style="vertical-align:middle">many links</td>
							</tr>
						</table>
						<input id="minProbability" name="minProbability" type="hidden"></input>
					</fieldset>
					
					<fieldset title="Are there any topics that you don't want? (seperate with ';')">
						<legend >Banned topics</legend> 
						<input id="bannedTopics" name="bannedTopics" type="text" value="{@bannedTopics}"></input>
					</fieldset>
					
				</div>
				
				<div id="submit">
					<input type="submit" value="Wikify"></input>
				</div>
			</div>
	
			
			<xsl:choose>
			<xsl:when test="Result">
	
				<div name="container" class="container">
					<p><em>Wikified Text</em> </p>
					
					<div class="tabPanel">
						<div class="right"></div>
						<div name="tab" id="tab_topics" class="tabDeselected" onclick="selectTab('topics')"><div class="right"></div>Detected Topics</div>
						<xsl:choose>	
							<xsl:when test="Result/@outputMode=2">
								<div name="tab" id="tab_rawHtml" class="tabDeselected" onclick="selectTab('rawHtml')"><div class="right"></div>Raw Html</div>
								<div name="tab" id="tab_renderedHtml" class="tabSelected" onclick="selectTab('renderedHtml')"><div class="right"></div>Rendered Html</div>
							</xsl:when>
							<xsl:otherwise>
								<div name="tab" id="tab_wiki" class="tabSelected" onclick="selectTab('wiki')"><div class="right"></div>MediaWiki Markup</div>
							</xsl:otherwise>
						</xsl:choose>
					</div>
					
					<xsl:choose>	
						<xsl:when test="Result/@outputMode=2">
							<div name="outputBox" id="box_renderedHtml" class="outputBox" >
								<xsl:copy-of select="Result"/>
							</div>
							<div name="outputBox" id="box_rawHtml" class="outputBox" style="display:none">
								<xsl:copy-of select="Result"/>
							</div>
						</xsl:when>
						<xsl:otherwise>
							<div name="outputBox" id="box_wiki" class="outputBox">
								<xsl:copy-of select="Result"/>
							</div>
						</xsl:otherwise>
					</xsl:choose>
					
					
					<div name="outputBox" id="box_topics" class="outputBox" style="display:none">
						
						<p class="explanation">
					 	The brightness of the topics below shows how well they fit the model of what a Wikipedian would link to. 
						</p>
						
						<ul class="topicList">
							<xsl:for-each select="DetectedTopicList/DetectedTopic">
							<xsl:sort select="@title"/>
							<li>
								<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}" style="font-size:{5+round(@weight*10)}px ; color:rgb({round(150-(150*@weight))},{round(200-(200*@weight))},200)" title="{round(@weight*100)}% probability of being a link">
									<xsl:value-of select="@title"/>
								</a>
							</li>
							</xsl:for-each>
						</ul>
						<div class="break">
						</div>
						
						
					</div>
					<xsl:if test="3 > Result/@documentScore">
                                                <div class="warning">
                                                        <em>Warning:</em> The text you have provided is short, or does not seem to have a central topic or theme. This system has been trained to identify and link to the Wikipedia articles that the reader is most likely to be interested in, by considering how each topic relates to those around it. It works best if the text is reasonably long and focused.
                                                </div>
                                        </xsl:if>

				</div>
			</xsl:when>
			<xsl:otherwise>
				<div class="note">
	                        <p>
        	                        <em>Note:</em> This service is machine-readable.
                	        </p>
                       		<p class="small">
                                	It can be made to return XML by appending <i>&amp;xml</i> to the request.
                        	</p>
                        	<p class="small">
                                	Feel free to point a bot or a service here (via POST or GET, it doesn't matter). There are some
                                	<a href="{$service_name}?task=wikify&amp;help">additional parameters</a> available if you do.
                                	Bear in mind that we may restrict access if usage becomes excessive.
                                	You can always run these services yourself by installing your own version of Wikipedia Miner.
                        	</p>
                		</div>
			</xsl:otherwise>
			</xsl:choose>
		</form>
	</body>
</html>
</xsl:template>

</xsl:stylesheet>
 
