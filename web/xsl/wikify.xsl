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
		<link type="text/css" href="css/smoothness/jquery-ui-1.7.1.custom.css" rel="stylesheet" />	
		 
		<script type="text/javascript" src="js/wikify.js"></script>
		<script type="text/javascript" src="js/jquery-1.3.2.min.js"></script>
		<script type="text/javascript" src="js/jquery-ui-1.7.1.custom.min.js"></script>
		
		<script type="text/javascript">
			
			var sourceMode=<xsl:value-of select="@sourceMode"/> ;
			var repeatMode=<xsl:value-of select="@repeatMode"/> ;
			var minProbability = <xsl:value-of select="@minProbability"/> ;
			var bannedTopics = "<xsl:value-of select="@bannedTopics"/>" ;
			
			
			$(function(){
				$('#tabs').tabs();	
				
				$('#slider').slider({value: Math.round((1-minProbability)*100) , step: 10, change: function(event, ui){
						$("#minProbability").val((100-ui.value)/100) ;
					}
				});
			});
		</script>
		
		
	</head>
	<body onload="init()" onresize="resize()">
		
		<a href="http://wikipedia-miner.sf.net">Wikipedia Miner</a> | <a href="{$server_path}/{$service_name}"> services</a>
		
		<h1 style="margin-top:5px"> <em>Wikify</em> Service</h1>
	  	
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
				
			<h2>Text to Wikify</h2>
			
			<div class="ui-widget-content ui-corner-all" style="padding: 5px ;">
		
				<div id="options" class="ui-widget-content ui-corner-all">
		
					<fieldset title="What should happen when the same topic is mentioned multiple times?">
					<legend>Markup</legend>
						<input id="sourceMode_html" name="sourceMode" type="radio" value="2"></input>
						<label for="sourceMode_html">HTML</label>
						<input id="sourceMode_wiki" name="sourceMode" type="radio" value="3"></input>
						<label for="sourceMode_wiki">MediaWiki</label>
						<input id="sourceMode_auto" name="sourceMode" type="radio" value="0"></input>
						<label for="sourceMode_auto">detect automatically</label>
					</fieldset>
					<fieldset title="What should happen when the same topic is mentioned multiple times?">
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
								<td><div id="slider" style="width:150px"></div></td>
								<td style="vertical-align:middle">many links</td>
							</tr>
						</table>
			
						<input id="minProbability" name="minProbability" type="hidden"></input>
					</fieldset>
					<fieldset title="Are there any topics that you don't want? (seperate with ';')">
						<legend>Banned topics</legend>
						<input id="bannedTopics" name="bannedTopics" type="text" value="{@bannedTopics}"></input>
					</fieldset>
				</div>
			
				<div id="inputBox_container" style="margin-right:12px ;">
					<textarea name="source" class="inputBox"><xsl:value-of select="Source"/></textarea>
				</div>
				
				<br style="clear:both"/>
					
				<div style="margin-top: 10px ; text-align: right ;">
					<a id="showOptions" onclick="showOptions()">show options</a>
					<a id="hideOptions" onclick="hideOptions()">hide options</a>
		
					<input id="submit" type="submit" value="Wikify" style="padding: 10px 20px; cursor:pointer" class="ui-state-default ui-corner-all" onmouseover="$('#submit').addClass('ui-state-hover')" onmouseout="$('#submit').removeClass('ui-state-hover')"></input>
				</div>
			
			</div>
				
		
			<xsl:choose>
				<xsl:when test="Result">
	
					<h2>Wikified Text</h2>
					
					<div id="tabs" style="margin-bottom: 20px">
						<ul>
							<xsl:choose>	
							<xsl:when test="Result/@outputMode=2">
								<li><a href="#tab_renderedHtml">Rendered HTML</a></li>
								<li><a href="#tab_rawHtml">Raw HTML</a></li>
							</xsl:when>
							<xsl:otherwise>	
								<li><a href="#tab_wiki">MediaWiki Markup</a></li>
							</xsl:otherwise>
							</xsl:choose>
							
							<li><a href="#tab_topics">Detected Topics</a></li>
						</ul>
						
						<xsl:choose>
							<xsl:when test="Result/@outputMode=2">
								<div id="tab_renderedHtml">
									<xsl:copy-of select="Result"/>
								</div>
								<div id="tab_rawHtml">
									<xsl:copy-of select="Result"/>
								</div>
							</xsl:when>
							<xsl:otherwise>	
								<div id="tab_wiki">
									<xsl:copy-of select="Result"/>
								</div>
							</xsl:otherwise>
						</xsl:choose>	
						
						
						<div id="tab_topics">
							<p class="explanation">
					 			The size of the topics below shows how well they fit the model of what a Wikipedian would link to. 
							</p>
							
							<ul class="topicList">
							<xsl:for-each select="DetectedTopicList/DetectedTopic">
							<xsl:sort select="@title"/>
								<li>
									<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}" style="font-size:{10+round(@weight*6)}px ; color:rgb({round(150-(150*@weight))},{round(200-(200*@weight))},200)" title="{round(@weight*100)}% probability of being a link">
										<xsl:value-of select="@title"/>
									</a>
								</li>
							</xsl:for-each>
							</ul>
							<div class="break"></div>
						</div>
					</div>
		
					<xsl:if test="3 > Result/@documentScore">
						<div class="ui-state-error ui-corner-all" style="padding: 0 .7em;"> 
							<p><span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span> 
							<strong>Warning:</strong> The text you have provided is short, or does not seem to have a central topic or theme. 
							</p>
							<p>
							This system has been trained to identify and link to the Wikipedia articles that the reader is most likely to be interested in, by considering how each topic relates to those around it. It works best if the text is reasonably long and focused.
							</p>
							
							
						</div>
				    </xsl:if>
					
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
 
