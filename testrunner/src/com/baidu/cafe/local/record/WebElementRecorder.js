var Cafe = function() {};
Cafe.prototype.getElementFamilyString = function(element) {
    var familyString = '';
    for ( ; element && element !== document.body && element.nodeType == 1; element = element.parentNode ) {
        var children = element.parentNode.childNodes;
        var id = '';
        var i = 0;
        for(; i < children.length; i++) {
            if (children[i] === element) {
                break;
            }
        }
        familyString = '-' + i + familyString;
    }
    return familyString.substring(1);
}
Cafe.prototype.onEventCallback = function() {
	var event = arguments[0];
	if(this === event.target) {
		var rect = this.getBoundingClientRect();
		prompt("{'action':'" + event.type + "','time':'" + (new Date -0) + "','familyString':'" + Cafe.prototype.getElementFamilyString(event.target) + "','left':'" + rect.left + "','top':'" + rect.top + "','width':'" + rect.width + "','height':'" + rect.height + "','tag':'" +this.tagName + "'}");
		Cafe.prototype.finished();
	}
}
Cafe.prototype.onTouchmoveCallback = function() {
	var event = arguments[0];
	if(this === event.target) {
		var rect = this.getBoundingClientRect();
		prompt("{'action':'" + event.type + "','time':'" + (new Date -0) + "','familyString':'" + Cafe.prototype.getElementFamilyString(event.target) + "','left':'" + rect.left + "','top':'" + rect.top + "','width':'" + rect.width + "','height':'" + rect.height + "','tag':'" +this.tagName + "'}");
		Cafe.prototype.finished();
	}
}
Cafe.prototype.onChangeCallback = function() {
	var event = arguments[0];
	if(this === event.target) {
		var rect = this.getBoundingClientRect();
		prompt("{'action':'" + event.type + "','time':'" + (new Date -0) + "','familyString':'" +Cafe.prototype.getElementFamilyString(event.target) + "','left':'" + rect.left + "','top':'" + rect.top + "','width':'" + rect.width + "','height':'" + rect.height + "','value':'" + event.target.value + "','tag':'" +this.tagName + "'}");
		Cafe.prototype.finished();
	}
}
Cafe.prototype.finished = function() {
	prompt('WebElementRecorder-finished');
}
Cafe.prototype.hookWebDocument = function() {
	var walk=document.createTreeWalker(document.body,NodeFilter.SHOW_ALL,null,false); 
	while(n=walk.nextNode()){
        if(typeof n._cafe === "undefined") {
            n.removeEventListener('touchstart', this.onEventCallback);
            n.removeEventListener('touchend', this.onEventCallback);
            n.removeEventListener('touchmove', this.onTouchmoveCallback);
            n.removeEventListener('click', this.onEventCallback);

            n.addEventListener('touchstart', this.onEventCallback, false);
            n.addEventListener('touchend', this.onEventCallback, false);
            n.addEventListener('touchmove', this.onTouchmoveCallback, false);
            n.addEventListener('click', this.onEventCallback, false);
            n._cafe = new Object();
        }  
    }   
}
var _cafe = new Cafe();
_cafe.hookWebDocument();
setInterval("_cafe.hookWebDocument()", 200);

