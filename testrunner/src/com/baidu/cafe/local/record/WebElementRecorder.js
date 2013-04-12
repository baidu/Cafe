function findInArray(ar, el) {
	var len = ar.length;
	for (var i = 0; i < len; i++) {
		if (ar[i] == el) return i;
	}
	return -1;
}
function getElementsByTagName(tagName, doc){
    return doc.getElementsByTagName(tagName.toLowerCase());
}
function getXPath(element) {
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
}
function onClickCallback() {
	var event = arguments[0];
	prompt(event.type + ";,local.clickOnWebElement(By.xpath(\"" + getXPath(event.target) + "\"));");
	finished();
};
function onChangeCallback() {
	var event = arguments[0];
	prompt(event.type + ";,local.enterTextInWebElement(By.xpath(\"" + getXPath(event.target) + "\"), \"" + event.target.value + "\");");
	finished();
};
function finished() {
	prompt('WebElementRecorder-finished');
};

document.removeEventListener('click', onClickCallback);
document.removeEventListener('change', onChangeCallback);

document.addEventListener('click', onClickCallback, false);
document.addEventListener('change', onChangeCallback, false);
