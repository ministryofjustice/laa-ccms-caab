function updateHiddenField(selectElementId, hiddenFieldId) {
    var selectElement = document.getElementById(selectElementId);
    var selectedOption = selectElement.options[selectElement.selectedIndex];
    var categoryDescription = selectedOption.text;
    document.getElementById(hiddenFieldId).value = categoryDescription;
}