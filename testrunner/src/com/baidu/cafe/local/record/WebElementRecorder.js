var WebElementRecorder = function() {};
WebElementRecorder.prototype.findInArray = function (ar, el) {
	var len = ar.length;
	for (var i = 0; i < len; i++) {
		if (ar[i] == el) return i;
	}
	return -1;
};
WebElementRecorder.prototype.getElementsByTagName = function(tagName, doc){
    return doc.getElementsByTagName(tagName.toLowerCase());
};
WebElementRecorder.prototype.getXPath = function(element) {
	var n = element;
	var s = "";
	while (true){
	    var p = n.parentNode;
	    if (p == n || p == null) break;
	    var ix = this.findInArray(this.getElementsByTagName(n.tagName, p), n);
	    var sfx = ix > 0 ? "["+(ix+1)+"]" : "";
	    s = n.tagName.toLowerCase() + sfx + (s == "" ? "" : ("/" + s)); 
	    n = p; 
	}    
	return "/" + s; 
};
WebElementRecorder.prototype.onClickCallback = function() {
	var event = arguments[0];
	prompt(event.type + ";,local.clickOnWebElement(By.xpath(\"" + record.getXPath(event.target) + "\"));");
};
WebElementRecorder.prototype.onChangeCallback = function() {
	var event = arguments[0];
	prompt(event.type + ";,local.enterTextInWebElement(By.xpath(\"" + record.getXPath(event.target) + "\"), \"" + event.target.value + "\");");
};
WebElementRecorder.prototype.finished = function() {
	prompt('WebElementRecorder-finished');
}
document.removeEventListener('click', WebElementRecorder.prototype.onClickCallback);
document.removeEventListener('change', WebElementRecorder.prototype.onChangeCallback);

document.addEventListener('click', WebElementRecorder.prototype.onClickCallback, false);
document.addEventListener('change', WebElementRecorder.prototype.onChangeCallback, false);
