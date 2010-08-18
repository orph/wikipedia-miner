var wmw_xmlHttp ;
var wmw_loadStep = 0 ;
var wmw_loadIntervalId ;
var wmw_definitionCache = new Array() ;

function wmw_showTooltip(tooltipIndex, articleId, serverPath, serviceName) {
	
	var link = document.getElementById("wmwtt_" + tooltipIndex) ;
	var tooltip = document.getElementById("wmtt") ;

	
	tooltip.style.visibility = "hidden" ;
	
	var pageWidth = 0, pageHeight = 0;
		
	if( typeof( window.innerWidth ) == 'number' ) {
	   //Non-IE
	   pageWidth = window.innerWidth;
	   pageHeight = window.innerHeight;
	} else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
	   //IE 6+ in 'standards compliant mode'
	   pageWidth = document.documentElement.clientWidth;
	   pageHeight = document.documentElement.clientHeight;
	} else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
	   //IE 4 compatible
	   pageWidth = document.body.clientWidth;
	   pageHeight = document.body.clientHeight;
	}
		
	var x = wmw_findPosX(link) ;
	var y = wmw_findPosY(link) ;

	if ((x+420) < pageWidth) {
		tooltip.style.left = x + "px" ;
		tooltip.style.right = "auto" ;
	} else {
		tooltip.style.left = "auto" ;
		tooltip.style.right = "10px" ;
	}
	
	tooltip.style.top = y + link.offsetHeight + "px" ;
	
	clearInterval(wmw_loadIntervalId) ; 
	xmlHttp = null ;
	
	// = x + 20 + "px" ;
	
	var definition = wmw_definitionCache[articleId] ;
	
	if (definition == null) {	
		tooltip.innerHTML = "loading" ; 
		wmw_loadIntervalId = setInterval(new Function("wmw_animateLoading()"), 500) ;
	} else {
		tooltip.innerHTML = definition ;
	}
	
	tooltip.style.visibility = "visible" ;
	
	wmw_requestDefinition(articleId, serverPath, serviceName) ;
}


function wmw_hideTooltip() {
	  var tooltip = document.getElementById("wmtt") ;
		tooltip.style.visibility = "hidden" ;
}

function wmw_animateLoading() {
	var tooltip = document.getElementById("wmtt") ;
				
	if(wmw_loadStep++ >= 3) 
		wmw_loadStep = 0 ;
				
	var label = "loading" ;
	for (var i=0; i<wmw_loadStep ; i++) 
		label = label + "." ;
									
	tooltip.innerHTML = label ;
}

function wmw_requestDefinition(articleId, serverPath, serviceName){
	try {
	 	wmw_xmlHttp = wmw_getHTTPObject();
  		wmw_xmlHttp.open("GET", serverPath + serviceName + "?task=define&getImages=true&linkDestination=0&maxImageWidth=100&maxImageHeight=100&id=" + articleId, true);
  		wmw_xmlHttp.onreadystatechange = new Function("wmw_processDefinitionResponse() ;") ;
 
  		wmw_xmlHttp.send(null);
	} catch(e) {
		throw (e) ;	
	}
}

function wmw_processDefinitionResponse(){
	
	if (wmw_xmlHttp.readyState == 4) {
		clearInterval(wmw_loadIntervalId) ;
		var tooltip = document.getElementById("wmtt") ;
		
		try {
		
			var msg ;
			var xmlDefinitionResponse = wmw_xmlHttp.responseXML.getElementsByTagName("DefinitionResponse")[0] ;
		
			var id = xmlDefinitionResponse.getAttribute("id") ;
			
			var xmlDefinition = xmlDefinitionResponse.getElementsByTagName("Definition")[0] ;

			msg = (new XMLSerializer()).serializeToString(xmlDefinition);
											
			var sp = msg.indexOf(">") ;
			var ep = msg.lastIndexOf("<") ;

			if (sp>0 && ep>0 && sp<ep)
				msg = msg.substring(sp+1, ep) ;
			else
				msg = "(no definition)" ;
				
			var xmlImages = xmlDefinitionResponse.getElementsByTagName("Image") ;
			
			if (xmlImages != null && xmlImages.length > 0) 
				msg = "<p> <img src=" + xmlImages[0].getAttribute("url") + "></img> " + msg + "</p>" ;
			
			msg = msg + "<div class='clear'></div>" ;
			
			wmw_definitionCache[id] = msg ;
			tooltip.innerHTML = msg ;	
			
		} catch(e) {
			tooltip.innerHTML = "(error retrieving definition)" ;
			throw e ;
		}	
	}
}

function wmw_findPosX(obj) {
    var curleft = 0;
    if(obj.offsetParent)
        while(1) 
        {
          curleft += obj.offsetLeft;
          if(!obj.offsetParent)
            break;
          obj = obj.offsetParent;
        }
    else if(obj.x)
        curleft += obj.x;
    return curleft;
 			}

function wmw_findPosY(obj) {
    var curtop = 0;
    if(obj.offsetParent)
        while(1)
        {
          curtop += obj.offsetTop;
          if(!obj.offsetParent)
            break;
          obj = obj.offsetParent;
        }
    else if(obj.y)
        curtop += obj.y;
    return curtop;
}

function wmw_getHTTPObject() { if (typeof XMLHttpRequest != 'undefined') { return new XMLHttpRequest(); } try { return new ActiveXObject("Msxml2.XMLHTTP"); } catch (e) { try { return new ActiveXObject("Microsoft.XMLHTTP"); } catch (e) {} } return false; }


