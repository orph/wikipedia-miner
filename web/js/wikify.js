
function init(){
	
	//set option values
	switch(sourceMode) {
		case 2:
			document.getElementById("sourceMode_html").checked = true ;
			break ;
		case 3:
			document.getElementById("sourceMode_wiki").checked = true ;
			break ;
		default:
			document.getElementById("sourceMode_auto").checked = true ;	 
	}
	
	switch(repeatMode) {
		case 0:
			document.getElementById("repeatMode_all").checked = true ;
			break ;
		case 1:
			document.getElementById("repeatMode_first").checked = true ;
			break ;
		default:
			document.getElementById("repeatMode_fis").checked = true ;	 
	}
	
	//setup slider
	slider = new Slider("slider", 100, 22, "white", "#687898", "rgb(200,200,200)"); 
	slider.onNewPosition = function() {
		var value = Math.round(slider.position*100)/100 ;
		if (value < 0.01) value = 0 ;
		if (value > 0.89) value = 1 ;
		
		document.getElementById("minProbability").value = 1- value ; 
	}

	slider.setPosition(1-minProbability) ;
	document.getElementById("minProbability").value = minProbability ; 
	
	
	var rawHtmlContainer = document.getElementById("box_rawHtml");
	if (rawHtmlContainer != null) {
		var html = rawHtmlContainer.innerHTML;
	
		//remove result tags
		var start = html.indexOf(">") ;
		if (start > 0)
			html = html.substring(start+1) ; 
			
		var end = html.lastIndexOf("<") ;
		if (end > 0) 
			html = html.substring(0, end) ;
			
			
	
		//escape html tags
		html = html.replace(/</g, "&lt;");
		html = html.replace(/>/g, "&gt;");
		
		rawHtmlContainer.innerHTML = html;
	}
	
	var wikiContainer = document.getElementById("box_wiki");
	if (wikiContainer != null) {
		var markup = wikiContainer.innerHTML;
	
		//remove result tags
		var start = markup.indexOf(">") ;
		if (start > 0)
			markup = markup.substring(start+1) ; 
			
		var end = markup.lastIndexOf("<") ;
		if (end > 0) 
			markup = markup.substring(0, end) ;
			
		var temp = "" ;
		var lastIndex = 0 ;
		
		var pattern=/\[\[(.*?)(|.*?)\]\]/g ;
		var result;
		while ((result = pattern.exec(markup)) != null) {			
			temp = temp + markup.substring(lastIndex, result.index) ;
			temp = temp + "<a href=\"http://en.wikipedia.org/wiki/" + getDestination(result[0]) + "\">" + result[0] + "</a>" ;
			
			lastIndex = pattern.lastIndex ;
		}
		
		temp = temp + markup.substring(lastIndex) ;
		wikiContainer.innerHTML = temp ;
	
		
		//rawHtmlContainer.innerHTML = html;
		
		
		
		
		
	}
	
	resize() ;
}

function getDestination(wikiLink) {
	
	var pos = wikiLink.indexOf("|") ;
	
	if (pos > 0)
		return wikiLink.substring(2, pos) ;
	else
		return wikiLink.substring(2, wikiLink.length - 2) ;
	
}

function selectTab(id) {
				
	var tabs = document.getElementsByName("tab")
	
	for (var i = 0 ; i<tabs.length ; i++)
		tabs[i].className = "tabDeselected" ;
				
	var boxes = document.getElementsByName("outputBox") ;
				
	for (var i = 0 ; i<tabs.length ; i++)
		boxes[i].style.display = "none" ; 	
				
	var tab = document.getElementById("tab_" + id) ;
	tab.className = "tabSelected" ;
				
	var box = document.getElementById("box_" + id) ;
	box.style.display = "block" ; 	
	
}

function showOptions() {
	document.getElementById("options").style.display = "block" ;
	document.getElementById("showOptions").style.display = "none" ;
	document.getElementById("hideOptions").style.display = "inline" ;
}

function hideOptions() {
	document.getElementById("options").style.display = "none" ;
	document.getElementById("showOptions").style.display = "inline" ;
	document.getElementById("hideOptions").style.display = "none" ;
}

function resize() {
	
	var pd = getPageDimensions() ;
	
	
	var containers = document.getElementsByName("container")
	for (var i = 0 ; i<containers.length ; i++)
		containers[i].style.width = (pd.width - 70) + "px" ;
		
	var boxes = document.getElementsByName("outputBox") ;
	for (var i = 0 ; i<boxes.length ; i++)
		boxes[i].style.width = (pd.width - 82) + "px" ;
		
	var inputBox = document.getElementById("inputBox") ;
	inputBox.style.width = (pd.width - 82) + "px" ;
}

function getPageDimensions() {
	
	var width = 0, height = 0;

	if( typeof( window.innerWidth ) == 'number' ) {
		//Non-IE
		width = window.innerWidth;
		height = window.innerHeight;
	} else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
		//IE 6+ in 'standards compliant mode'
		width = document.documentElement.clientWidth;
		height = document.documentElement.clientHeight;
	} else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
		//IE 4 compatible
		width = document.body.clientWidth;
		height = document.body.clientHeight;
    }

	return new Dimension(width, height);
}


 //a helper object for storing locations
 function Dimension(width, height){
 	this.width = width;
 	this.height = height;
 }

