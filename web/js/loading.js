var xmlHttp ;
var intervalId ;

var server_path ;
var service_name ;

function init(progress, sp, sn) {
	setProgress(progress) ;
	
	server_path = sp ;
	service_name = sn ;
	
	
	intervalId = setInterval(new Function("requestProgressUpdate()"), 500) ;
}

function setProgress(progress) {
	var progressBar = document.getElementById("progressBar") ;
	progressBar.style.width = Math.round(progress*500) + "px" ;

	var progressLabel = document.getElementById("progressLabel") ;
	progressLabel.innerHTML = Math.round(progress*100) + "%" ;
	
	document.title = "loading (" + Math.round(progress*100) + "%) | Wikipedia Miner Services" ;
} 

function requestProgressUpdate(){
	try {
		
	 	xmlHttp = getHTTPObject();
  		xmlHttp.open("GET", server_path + service_name + "?task=progress&xml", true);
  		xmlHttp.onreadystatechange = new Function("processUpdateResponse() ;") ;
 
  		xmlHttp.send(null);
	} catch(e) {
		throw (e) ;	
	}
}

function processUpdateResponse(){
	try {
		if (xmlHttp.readyState == 4) {
			var xmlLoading = xmlHttp.responseXML.getElementsByTagName("loading")[0] ;
		
			var progress = xmlLoading.getAttribute('progress') ;
			setProgress(progress) ;
			
			if (progress >= 1) {
				clearInterval(intervalId) ;
				
				if (getUrlParameter("task") != "progress")
					window.location.reload(true)	 ;
			}
  	}
	} catch(e) {
		throw (e) ;	
	}
}

function getUrlParameter( name )
{
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  else
    return results[1];
}

function getHTTPObject() { if (typeof XMLHttpRequest != 'undefined') { return new XMLHttpRequest(); } try { return new ActiveXObject("Msxml2.XMLHTTP"); } catch (e) { try { return new ActiveXObject("Microsoft.XMLHTTP"); } catch (e) {} } return false; }
