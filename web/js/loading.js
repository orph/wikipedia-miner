var intervalId ;

var seenError = false ;

function setProgress(progress) {

		$("#progressbar").progressbar('value', Math.round(progress * 100));
		
		document.title = "loading (" + Math.round(progress * 100) + "%) | Wikipedia Miner Services";
} 

function requestProgressUpdate(){
	$.get(serverPath + serviceName + "?task=progress&xml", function(response){
    	processUpdateResponse($(response));
	});
}

function processUpdateResponse(response){
		
	var xmlLoading = response.find("loading");
	
	var progress = xmlLoading.attr('progress') ;
	
	setProgress(progress) ;
						
	if (progress >= 1) {
		clearInterval(intervalId) ;
				
	if (getUrlParameter("task") != "progress")
			window.location.reload(true) ;
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

