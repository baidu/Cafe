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
    var xpath = ''; 
    for ( ; element && element.nodeType == 1; element = element.parentNode ) { 
        var children = element.parentNode.childNodes;
        var id = ''; 
        var idx = 1;
        for(var i = 0; i < children.length; i += 1) {
            if (children[i] === element) {
                break;
            } else if (children[i].tagName == element.tagName) {
                idx += 1;
            }
        }
        idx > 1 ? (id = '[' + idx + ']') : (id = '');
        xpath = '/' + element.tagName.toLowerCase() + id + xpath;
    }   
    return xpath;
}
function onClickCallback() {
	var event = arguments[0];
	prompt(event.type + ";,local.clickOnWebElement(By.xpath(\"" + getXPath(event.target) + "\"));");
	finished();
}
function onChangeCallback() {
	var event = arguments[0];
	prompt(event.type + ";,local.enterTextInWebElement(By.xpath(\"" + getXPath(event.target) + "\"), \"" + event.target.value + "\");");
	finished();
}
function finished() {
	prompt('WebElementRecorder-finished');
}

document.removeEventListener('click', onClickCallback);
document.removeEventListener('change', onChangeCallback);

document.addEventListener('click', onClickCallback, false);
document.addEventListener('change', onChangeCallback, false);
