function updateHiddenField(selectElementId, hiddenFieldId) {
    var selectElement = document.getElementById(selectElementId);
    var selectedOption = selectElement.options[selectElement.selectedIndex];
    document.getElementById(hiddenFieldId).value = selectedOption.text;
}