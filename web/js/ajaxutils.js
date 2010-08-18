
function loadAsync(sUri, SOAPMessage) {
try {
 var xmlHttp = XmlHttp.create();
 var async = true;
 xmlHttp.open("POST", sUri, async);
 xmlHttp.onreadystatechange = function () {
 
  if (xmlHttp.readyState == 4){
		 var result = xmlHttp.responseText;
		 getTitle2(xmlHttp.responseXML, xmlHttp.responseText);
  }     
 }
 xmlHttp.setRequestHeader("SOAPAction", " ");
 xmlHttp.setRequestHeader("Content-Type", "Content-Type: text/xml; charset=utf-8");
 
 xmlHttp.send(SOAPMessage);
 
} catch (e) {
 output ( "gs_ajax_utils.loadAsync.error: " + e ); 
 throw (e) ;
}
}


function messageToSOAP(message) {
try {
 var soapBody = '<soapenv:Body>' + message + '</soapenv:Body>'
 var soap = '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">' + soapBody + '</soapenv:Envelope>'
 var x= '<?xml version="1.0" encoding="UTF-8"?>' + soap;
 return x;
 
} catch (e) {
 output ( "gs_ajax_utils.messageToSOAP.error: " + e ); 
 throw (e) ;
}
}

function getNodeText(element) { //get the text from possibly multiple text nodes
try {
 if (element.hasChildNodes()) {
	 var tempString = '';
  for (j=0; j < element.childNodes.length; j++) {
  	if (element.childNodes[j].nodeType == 3) { //Node.TEXT_NODE ) { // =2
  		tempString += element.childNodes[j].nodeValue;
  	} 
			else {
				tempString += 'non text node: ';
			}
  } 
	 return tempString;
 }
 else {
	 return 'getText: element has no ChildNodes from which to extract Text';
 }
 
} catch (e) {
 output ( "gs_ajax_utils.getText.error: " + e ); 
 throw (e) ;
}
}


function newOpenTag(name) {
try {
 return '<' + name + '>';
 
} catch (e) {
 output ( "gs_ajax_utils.newOpenTag.error: " + e ); 
 throw (e) ;
}
}

function newCloseTag(name) {
try {
	return '</' + name + '>';
} catch (e) {
 output ( "gs_ajax_utils.newCloseTag.error: " + e ); 
 throw (e) ;
}
}

function newEmptyTag(name) {
try {
	return '<' + name + '/>';
} catch (e) {
 output ( "gs_ajax_utils.newEmptyTag.error: " + e ); 
 throw (e) ;
}
}

function newElement(name, content) {
try {
 var e = '<' + name + '>' + content;
 e += '</' + name + '>';
 return e;
} catch (e) {
 output ( "gs_ajax_utils.newElement.error: " + e ); 
 throw (e) ;
}
}

function newElementAtt1(name, content, attName, attValue) {
try {
 var e = '<' + name + ' ' + attName + '="' + attValue +'">' + content;
 e += '</' + name + '>';
 return e;
 
} catch (e) {
 output ( "gs_ajax_utils.newElementAtt1.error: " + e ); 
 throw (e) ;
}
}

function newElementAtt(name, content, nameArray, valueArray) {
try {
 var e = '<' + name + ' ' ;
 for (var i=0; i < nameArray.length; i++) {
  e += newAttribute(nameArray[i], valueArray[i])
 }
 e += '>' + content;
 e += '</' + name + '>';
 return e;
 
} catch (e) {
 output ( "gs_ajax_utils.newElementAtt.error: " + e ); 
 throw (e) ;
}
}


function newAttribute(name, value) {
try {
	return ' ' + name + '="' + value + '"';
} catch (e) {
 output ( "gs_ajax_utils.newAttribute.error: " + e ); 
 throw (e) ;
}
}

function countElementChildren(node) {
try {
 var count= 0;
	var childList = node.childNodes;
	for(var i=0; i < (childList.length); i++) {
		var childNode = childList.item(i);
		if ((childNode.nodeType == 1))	{ // only count elements
			count++;
		}
	}
	return count;
} catch (e) {
 output ( "gs_ajax_utils.countElementChildren.error: " + e ); 
 throw (e) ;
}
}

function removeAllChildren(node) {
try {
	while (node.hasChildNodes()) {
		node.removeChild(node.firstChild);
	}
} catch (e) {
 output ( "gs_ajax_utils.removeAllChildren.error: " + e ); 
 throw (e) ;
}
}

function isElement(node) {
try {
 if (node.nodeType == 1) {
		return true; }
 else {
		return false;
 }
} catch (e) {
 output ( "gs_ajax_utils.isElement.error: " + e ); 
 throw (e) ;
}
}
