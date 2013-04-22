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
