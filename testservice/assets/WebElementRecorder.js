<<<<<<< HEAD
var Cafe = function() {};
Cafe.prototype.getElementFamilyString = function(element) {
    var familyString = '';
    for ( ; element && element.nodeType == 1; element = element.parentNode ) {
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
	if(this._familyString === event.target._familyString) {
		var rect = this.getBoundingClientRect();
		prompt("{'action':'" + event.type + "','time':'" + (new Date -0) + "','familyString':'" + event.target._familyString + "','left':'" + rect.left + "','top':'" + rect.top + "','width':'" + rect.width + "','height':'" + rect.height + "','tag':'" +this.tagName + "'}");
		Cafe.prototype.finished();
	}
}
Cafe.prototype.onTouchmoveCallback = function() {
	var event = arguments[0];
	if(this._familyString === event.target._familyString) {
		var rect = this.getBoundingClientRect();
		prompt("{'action':'" + event.type + "','time':'" + (new Date -0) + "','familyString':'" + event.target._familyString + "','left':'" + rect.left + "','top':'" + rect.top + "','width':'" + rect.width + "','height':'" + rect.height + "','tag':'" +this.tagName + "'}");
		Cafe.prototype.finished();
	}
}
Cafe.prototype.onChangeCallback = function() {
	var event = arguments[0];
	if(this._familyString === event.target._familyString) {
		var rect = this.getBoundingClientRect();
		prompt("{'action':'" + event.type + "','time':'" + (new Date -0) + "','familyString':'" + event.target._familyString + "','left':'" + rect.left + "','top':'" + rect.top + "','width':'" + rect.width + "','height':'" + rect.height + "','value':'" + event.target.value + "','tag':'" +this.tagName + "'}");
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
			n._familyString = this.getElementFamilyString(n);
			n._cafe = new Object();
		} else {
			n._familyString = this.getElementFamilyString(n);
		}
	}
}
var _cafe = new Cafe();
setInterval("_cafe.hookWebDocument()", 200);
=======
if(typeof _cafe === "undefined") {
	function getElementFamilyString(element) {
	    var familyString = '';
	    for ( ; element && element.nodeType == 1; element = element.parentNode ) {
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
	function onClickCallback() {
		var event = arguments[0];
		prompt(event.type + ";,local.clickOnWebElementByFamilyString(\"" + getElementFamilyString(event.target) + "\");");
		finished();
	}
	function onChangeCallback() {
		var event = arguments[0];
		prompt(event.type + ";,local.enterTextInWebElementByFamilyString(\"" + getElementFamilyString(event.target) + "\"), \"" + event.target.value + "\");");
		finished();
	}
	function finished() {
		prompt('WebElementRecorder-finished');
	}
	
	document.removeEventListener('click', onClickCallback);
	document.removeEventListener('change', onChangeCallback);
	
	document.addEventListener('click', onClickCallback, false);
	document.addEventListener('change', onChangeCallback, false);
	var _cafe = new Object();
}
>>>>>>> 69518b12e6d8d78b2fd5cc8b4c83503ba97b2743
