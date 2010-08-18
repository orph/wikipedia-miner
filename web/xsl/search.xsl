<?xml version="1.0" encoding="utf8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0" encoding="utf8" indent="yes"/>

<xsl:variable name="service_name" select="/WikipediaMinerResponse/@service_name"/>
<xsl:variable name="server_path" select="/WikipediaMinerResponse/@server_path"/>
<xsl:variable name="term" select="/WikipediaMinerResponse/SearchResponse/@term"/>

<xsl:template match="WikipediaMinerResponse/SearchResponse">
  <html xslns="http://www.w3.org/TR/html4/strict.dtd">
  <head>
  	<title><xsl:value-of select="@term"/> | Wikipedia Miner search service</title>
  	<link rel="stylesheet" href="css/style.css" type="text/css"/> 
  </head>
  <body>
  
  <div class="box" style="float:right; width:310px; margin-right: 0em ; margin-top: 0em">
		<form method="get" action="{service_name}">
			<input type="hidden" name="task" value="search"></input>
			<input type="text" name="term" style="width: 200px" value="{@term}"></input>
			<input type="submit" value="Search" style="width: 75px; margin-left: 10px"></input>
		</form>
				
		<div class="tl"></div>
		<div class="tr"></div>
		<div class="bl"></div>
		<div class="br"></div>
	</div>
  
  <a href="http://wikipedia-miner.sf.net">Wikipedia Miner</a> | <a href="{$server_path}/{$service_name}"> services</a>  
		
  
  <xsl:choose>
  	
  	<xsl:when test="@unspecifiedParameters">
	  	
	  	<h1 style="margin-top:5px"> <em>Search</em> Service</h1>
	  	
	  	<p>
	  		Welcome to the Wikipedia Miner <em>search</em> service. Searching is done through article titles, redirects, and anchors 
	  		(the terms used to link to each page). This method of searching encodes synonymy, so you can find the article about 
	  		<a href="{$service_name}?task=search&amp;id=46770">fixed-wing aircraft</a> by searching for <i>airplanes</i>, 
	  		<i>aeroplanes</i> or <i>planes</i>. 
			It also encodes polysemy, so you can tell that <a href="{$service_name}?task=search&amp;term=plane">plane</a> could also refer to a 
			<a href="{$service_name}?task=search&amp;id=84029">theoretical surface of infinite area and zero depth</a>, 
			or <a href="{$service_name}?task=search&amp;id=452991">a tool for shaping wooden surfaces</a>. 
	  	</p>
	  	
	  	<p> 
	  		This will return either a list of candidate articles (if the term is ambiguous), or the details of a single article (if it is not). 
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
                                <a href="{$service_name}?task=search&amp;help">additional parameters</a> available if you do.
                                Bear in mind that we may restrict access if usage becomes excessive.
                                You can always run these services yourself by installing your own version of Wikipedia Miner.
                        </p>
                </div>
		
 	</xsl:when>
  	
  	<xsl:when test="@unknownTerm">
	  	
	  	<h1 style="margin-top:5px"> <em>Search</em> Service</h1>
	  	
	  	<p>
	  		I have no idea what you mean by <em><xsl:value-of select="@unknownTerm"/></em>.
	  	</p>
  	</xsl:when>
  	
  
  	<xsl:when test="SenseList">
  		| <a href="{$server_path}/{$service_name}?task=search">search service</a> 
  	
  		<h1 style="margin-top:5px">What do you mean by <em><xsl:value-of select="@term"/></em>?</h1>
  	
			<p class="explanation">
				WikipediaMiner uses the links found in Wikipedia's articles to identify different senses for terms. 
				It looks like <em><xsl:value-of select="@term"/></em> is ambiguous, because there are 
				multiple articles that it is used to link to. So, which one do you want?
			</p>
		
			<ul class="senses">
				<xsl:for-each select="SenseList/Sense">
		
					<li class="sense">
						<table>
							<tr>
								<td><a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}"><xsl:value-of select="@title"/></a></td>
								<td>
									<div class="bar_outer" title="{round(@commonness*100)}% of {$term} links go here">
										<div class="bar_inner" style="width: {floor(@commonness*50)}px"></div>
									</div>
								</td>
							</tr>	
							</table>
							<p class="definition" style="margin-left: 20px">
								<xsl:copy-of select="FirstSentence"/>
							</p>
					</li>
				</xsl:for-each>
			</ul>
		</xsl:when>
		
		
		
		
		
		
		<xsl:when test="Article">
			| <a href="{$server_path}/{$service_name}?task=search">search service</a> 
  	
  		<h1 style="margin-top:5px"><xsl:value-of select="Article/@title"/></h1>

			<p>
				<xsl:copy-of select="Article/FirstParagraph"/>	
			</p>
			<p class="more">
				<a href="http://www.en.wikipedia.org/wiki/{Article/@title}">more</a>
			</p>
			
			<xsl:if test="Article/CategoryList/EquivalentCategory">
				<h2>Equivalent Category</h2>
				<p class="explanation">Another source of related topics for this concept</p>
				<ul>
					<xsl:for-each select="Article/CategoryList/EquivalentCategory">
						<li><a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}"><xsl:value-of select="@title"/></a></li>
					</xsl:for-each>
				</ul>
			</xsl:if>
			
			<xsl:if test="Article/RedirectList/Redirect">
				<h2>Redirects</h2>

				<p class="explanation">
					Alternative titles for this article. These are usually synonyms or alternative spellings, 
					but can also be narrower topics that didn't deserve a separate article.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Article/RedirectList/Redirect">
					<xsl:sort select="@title"/>
						<li><xsl:value-of select="@title"/></li>
					</xsl:for-each>
				</ul>
				<div class="break"></div>	
			</xsl:if>
	
			<xsl:if test="Article/AnchorList/Anchor">
				<h2>Anchors</h2>
				<p class="explanation">
				  Terms that are used within links to this article. These are similar to redirects, but may also represent broader topics.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Article/AnchorList/Anchor">
						<li style="color:rgb({round(100-(100*@proportion))},{round(100-(100*@proportion))},{round(100-(100*@proportion))})" title="used {@occurances} times" >
							<xsl:value-of select="@text"/>
						</li>
					</xsl:for-each>
				</ul>
				<div class="break"></div>	
			</xsl:if>	
			
			<xsl:if test="Article/LanguageLinkList/LanguageLink">
				<h2>Language Links</h2>
				<p class="explanation">
				  Links to articles which describe the same concept in another language. These provide translations.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Article/LanguageLinkList/LanguageLink">
					<xsl:sort select="@lang"/>
						<li>
							<span style="color: rgb(150,150,150)"><xsl:value-of select="@lang"/>:</span>&#160;<xsl:value-of select="@text"/>
						</li>
					</xsl:for-each>
				</ul>
				<div class="break"></div>	
			</xsl:if>	
			
			
			<xsl:if test="Article/CategoryList">
				<h2>Parent Categories</h2>
				<p class="explanation">
				  Categories to which this article belongs. These represent broader topics or ways of organizing this one.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Article/CategoryList/*">
					<xsl:sort select="@title"/>
						<li>
							<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}"><xsl:value-of select="@title"/></a>
						</li>
					</xsl:for-each>
				</ul>
				<div class="break"></div>	
			</xsl:if>	
			
			<xsl:if test="Article/LinkOutList">
				<h2>Article links - out</h2>
				<p class="explanation">
				  Pages that this article links to. Some of these represent related topics, others are fairly random. 
				  WikipediaMiner provides relatedness measures&#8212;indicated here by the brightness of each link&#8212;to help separate them.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Article/LinkOutList/*">
					<xsl:sort select="@title"/>
						<li>
							<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}" style="color:rgb({round(150-(150*@relatedness))},{round(200-(200*@relatedness))},200)" title="{round(@relatedness*100)}% relatedness to {$term}">
								<xsl:value-of select="@title"/>
							</a>
						</li>
					</xsl:for-each>
				</ul>
				<div class="break"></div>	
			</xsl:if>	
			
			<xsl:if test="Article/LinkInList">
				<h2>Article links - in</h2>
				<p class="explanation">
				  Pages that link to this article. Some of these represent related topics, others are fairly random. 
				  WikipediaMiner provides relatedness measures&#8212;indicated here by the brightness of each link&#8212;to help separate them.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Article/LinkInList/*">
					<xsl:sort select="@title"/>
						<li>
							<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}" style="color:rgb({round(150-(150*@relatedness))},{round(200-(200*@relatedness))},200)" title="{round(@relatedness*100)}% relatedness to {$term}">
								<xsl:value-of select="@title"/>
							</a>
						</li>
					</xsl:for-each>
				</ul>
			</xsl:if>	
		</xsl:when>
		
		
		
		
		
		<xsl:when test="Category">
		
			| <a href="{$server_path}/{$service_name}?task=search">search service</a> 
  	
  		<h1 style="margin-top:5px">Category:<xsl:value-of select="Category/@title"/></h1>
		
			<p>
				<xsl:copy-of select="Category/FirstParagraph"/>
			</p>
			<p class="more">
				<a href="http://www.en.wikipedia.org/wiki/Category:{Category/@title}">more</a>
			</p>
			
			<xsl:if test="Category/EquivalentArticle">
				<h2>Equivalent Article</h2>
				<p class="explanation">Another source of related topics for this concept</p>
				<ul>
					<xsl:for-each select="Category/EquivalentArticle">
						<li><a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}"><xsl:value-of select="@title"/></a></li>
					</xsl:for-each>
				</ul>
			</xsl:if>
			
			<xsl:if test="Category/ParentCategoryList">
				<h2>Parent Categories</h2>

				<p class="explanation">
					Categories to which this category belongs. These represent broader topics or ways of organizing this one.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Category/ParentCategoryList/*">
					<xsl:sort select="@title"/>
						<li>
							<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
								<xsl:value-of select="@title"/>
							</a>
						</li>
					</xsl:for-each>
				</ul>	
				<div class="break"></div>
			</xsl:if>
			
		
			<xsl:if test="Category/ChildCategoryList">
				<h2>Child Categories</h2>

				<p class="explanation">
					Categories which belong to this category. These represent narrower topics or ways of organizing them. 
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Category/ChildCategoryList/*">
					<xsl:sort select="@title"/>
						<li>
							<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
								<xsl:value-of select="@title"/>
							</a>
						</li>
					</xsl:for-each>
				</ul>	
				<div class="break"></div>
			</xsl:if>	
				
			<xsl:if test="Category/ChildArticleList">
				<h2>Child Articles</h2>

				<p class="explanation">
					Articles which belong to this category. These represent narrower topics.
				</p>
				<ul class="horizontal">
					<xsl:for-each select="Category/ChildArticleList/*">
					<xsl:sort select="@title"/>
						<li>
							<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
								<xsl:value-of select="@title"/>
							</a>
						</li>
						</xsl:for-each>
					</ul>	
				<div class="break"></div>
			</xsl:if>	
		</xsl:when>	
		
		
		
		
		
		
		<xsl:when test="Disambiguation">
		
			| <a href="{$server_path}/{$service_name}?task=search">search service</a> 
  	
  		<h1 style="margin-top:5px">Disambiguation:<xsl:value-of select="Disambiguation/@title"/></h1>
			<p> 
				<a href="http://www.en.wikipedia.org/wiki/{Disambiguation/@title}">open in Wikipedia</a>
			</p>
		
			<h2>Possible senses</h2>
			<p class="explanation">Articles listed as possible senses of the term for which this page was created. </p>
			<ul>
				<xsl:for-each select="Disambiguation/SenseList/Sense">
					<li>
						<a href="{$service_name}?task=search&amp;id={@id}&amp;term={@title}">
							<xsl:value-of select="@title"/>
						</a>
						<p class="definition"><xsl:copy-of select="FirstSentence"/></p>
					</li>
				</xsl:for-each>
			</ul>
		
		</xsl:when>
		
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
