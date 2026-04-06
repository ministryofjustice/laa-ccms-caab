document.addEventListener("DOMContentLoaded", function () {
  initCostAllocationDefaultSubmit();
});

function initCostAllocationDefaultSubmit() {
  var form = document.getElementById("cost-allocation-form");
  var nextButton = document.getElementById("cost-allocation-next");
  if (!form || !nextButton) {
    return;
  }

  form.addEventListener("keydown", function (event) {
    var isEnter = event.key === "Enter" || event.keyCode === 13;
    if (!isEnter) {
      return;
    }

    var target = event.target;
    if (!target) {
      return;
    }

    var tagName = target.tagName;
    if (tagName === "TEXTAREA" || tagName === "BUTTON") {
      return;
    }

    if (tagName === "INPUT") {
      var type = (target.getAttribute("type") || "").toLowerCase();
      if (type === "submit" || type === "button") {
        return;
      }
    }

    event.preventDefault();

    if (typeof form.requestSubmit === "function") {
      form.requestSubmit(nextButton);
      return;
    }

    var fallbackField = form.querySelector(
      "input[name='action'][data-default-submit='true']"
    );
    if (!fallbackField) {
      fallbackField = document.createElement("input");
      fallbackField.type = "hidden";
      fallbackField.name = "action";
      fallbackField.setAttribute("data-default-submit", "true");
      form.appendChild(fallbackField);
    }
    fallbackField.value = nextButton.value;
    form.submit();
  });
}
