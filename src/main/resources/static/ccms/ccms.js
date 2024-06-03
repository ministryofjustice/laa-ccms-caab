function doBlurEvent(theElement) {
  var theParent = theElement.parentNode;
  if (theElement.value == "" && document.getElementById(
      theElement.name + "_error") == null) {
    var oDiv = document.createElement("DIV");
    oDiv.setAttribute("id", theElement.name + "_error");
    oDiv.setAttribute("class", "errorText");
    oDiv.innerHTML = "Please complete this box";
    theParent.appendChild(oDiv);
  } else if (theElement.value != "" && document.getElementById(
      theElement.name + "_error") != null) {
    try {
      var err = document.getElementById(theElement.name + "_error");
      theParent.removeChild(err);
    } catch (e) {
    }
  }
}

function doChangeEvent(theElement) {
}

function doClickEvent(theElement) {
}

function doFocusEvent(theElement) {
}

function popupHelp(url) {
  url =url.replace(".jsp","")
  var helpWindow = window.open(url, "help",
      "height=300,width=420,location=no,toolbar=no,titlebar=no,directories=no,status=no,menubar=0,scrollbars=yes,resizable=yes");
  if (window.focus) {
    helpWindow.focus()
  }
  return false;
}

/*
 * Limits the text entry to size specified Parameters: field : The field. event :
 * the event size : The size limit
 */
function stopTyping(field, event, size) {
  var txt = field.value;
  var re = /\r\n/g
  txt = txt.replace(re, "\n");
  if (txt.length >= size) {
    /*
 * left arrow 37 up arrow 38 right arrow 39 down arrow 40 delete 46
 * backspace 8
 */
    // check for the shift key as well
    // This is required as in IE SHIFT+% will return a code of 37...
    if (event.shiftKey) {
      return false;
    } else {
      if (event.keyCode != 8 && event.keyCode != 46
          && event.keyCode != 38 && event.keyCode != 39
          && event.keyCode != 40 && event.keyCode != 37) {
        return false;
      }
    }
  }
}

/*
 * Limits the text entry to size specified Parameters: field : The field. event :
 * the event size : The size limit
 */
function updateCount(field, event, size) {
  var txt = field.value;
  var re = /\r\n/g
  txt = txt.replace(re, "\n");

  var counter = document.getElementById(field.id + '_count');
  counter.innerHTML = txt.length + ' / ' + size;
  if (field.value.length > size) {
    counter.style = 'color:red;font-weight:bold';
  } else {
    counter.style = '';
  }
}

function createPrintLink(parentElementId, linkText) {
  var parentElement = document.getElementById(parentElementId);
  var printLink = document.createElement("a");
  // IE bug, it ignores the setAttribute onclick
  if (printLink.attachEvent) {
    printLink.attachEvent("onclick", printWindow);
  }
  printLink.setAttribute("href", "javascript: void(null)");
  printLink.setAttribute("onclick", "javascript: window.print()");
  printLink.appendChild(document.createTextNode(linkText));
  parentElement.appendChild(printLink);

}

function printWindow() {
  window.print();
}

/**
 * Duplicate post prevention.
 */
(function () {

  // Flag indicating whether a submission has been made yet.
  var submitted = false;

  /**
   * Function that handles a submission attempt.
   */
  function submitHandler(event) {
    event = event || window.event;
    if (submitted) {
      if (event.preventDefault) {
        event.preventDefault();
      }
      event.cancelBubble = true;
      return false;
    }
    submitted = true;
  }

  /**
   * Adds submission handlers to all forms on the page.
   */
  function attachSubmissionHandlers() {
    var forms = document.getElementsByTagName("form");
    for (var index = 0; index < forms.length; index++) {
      var form = forms[index];
      addEventListener(form, "submit", submitHandler);
    }
  }

  /**
   * Adds a listener for a type of event on the supplied object.
   *
   * @param object
   *            The object that fires the event.
   * @param event
   *            The name of the event type (e.g. "load")
   * @param callback
   *            The function to be called when the event is fired.
   */
  function addEventListener(object, event, callback) {
    if (object.addEventListener) {
      object.addEventListener(event, callback, false);
    } else if (object.attachEvent) {
      object.attachEvent("on" + event, callback);
    }
  }

  /**
   * Adds character counter to text areas.
   */
  function addTextAreaMaxLength() {
    var textareas = document.getElementsByTagName("textarea");
    for (var index = 0; index < textareas.length; index++) {
      var textarea = textareas[index];
      var counter = document.createElement("div");
      counter.setAttribute("id", textarea.id + '_count');

      var tmpFn = window.stopTyping;
      window.stopTyping = function (a, b, size) {
        return size;
      };
      var maxlength = textarea.onkeydown();
      window.stopTyping = tmpFn;

      if (maxlength > 4000) {
        counter.appendChild(
            document.createTextNode(textarea.value.length + ' / ' + maxlength));
        insertAfter(textarea, counter);
      }
    }
  }

  function insertAfter(referenceNode, newNode) {
    referenceNode.parentNode.insertBefore(newNode, referenceNode.nextSibling);
  }

  // Add the form submission handlers when the page has finished loading
  addEventListener(window, "load", attachSubmissionHandlers);
  addEventListener(window, "load", addTextAreaMaxLength);
}());

document.addEventListener("DOMContentLoaded", function (event) {
  console.log('Removing max length');
  var addressLine1 = document.getElementById('client_mainAddress_addressLine1');
  var addressLine2 = document.getElementById('client_mainAddress_addressLine2');
  if (addressLine1 != null && addressLine2 != null) {
    addressLine1.removeAttribute('maxlength');
    addressLine2.removeAttribute('maxlength');
  }
});

/**
* To disable OPA Interview after one click
*/
function clickAndDisableHref(ctl) {
  $(ctl).prop("disabled", true);
  $(".interview-loading").removeClass("hidden");
  $(ctl).text('Loading ...');
  $("#interviewProgress").show("slow")
  return true;
}

function preventMultipleHrefClicks(e) {
  $(e).addClass('click-once');
  clickAndDisableHref(e);
}
