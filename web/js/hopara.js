var _serviceName ;
var _query ;
var _filterId ;
var _filterText ;
var _textFilterTimeout ;


function init(serviceName, query, filterId, filterText) {

	_serviceName = serviceName ;
	_query = query ;
	_filterId = filterId ;
	_filterText = filterText ;
	_textFilterTimeout = -1 ;
}


function addTopic(topicId) {

	submitQuery(_query + " [[" + topicId + "]]") ;

}

function removeTopic(topicId) {

	submitQuery(_query.replace("[[" + topicId + "]]", "")) ;

}

function selectSense(senseId, text, startIndex) {

	var query = "" ;
	
	if (startIndex > 0)
		query = _query.substring(0,startIndex) ;
		
	query = query + "[[" + senseId + "]]" + _query.substring(startIndex + text.length) ;

	submitQuery(query) ;
}

function selectCategoryFilter(cf) {

	_filterId = cf ;
	submitQuery(_query) ;
}

function setTextFilter(tf) {
	_filterText = tf ;
	submitQuery(_query) ;
}

function submitQuery(query) {

	var params = "?task=hopara&suggestionLimit=50&showRelations=false" ;

	if (_filterText != null)
		params = params + "&filterText=" + _filterText ;

	if (_filterId >= 0)
		params = params + "&filterId=" + _filterId ;
	
	window.location = _serviceName + params + "&query=" + query ;
}


function handleTextFilterChanged() {

	//alert("DING!") ;	

	if (_textFilterTimeout >= 0)
		clearTimeout(_textFilterTimeout) ;

	_textFilterTimeout = setTimeout(new Function("updateTextFilter()"), 2000) ;

}

function updateTextFilter() {

	var txtFilter = document.getElementById("filterText") ;
	_filterText = txtFilter.value ;

	submitQuery(_query) ;
}




