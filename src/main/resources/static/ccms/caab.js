$(document).ready($(function () {
    new toggleDiv();
}));
function toggleDiv() {
    var docDescElementWidth = $('.documentDescriptionMore').width();

    $('.moreLess').each(function () {
        var docDescTextWidth = parseInt(
            $(this).parent().prev().find('span').width());
        if ($(this).parent().prev().text().length === 0 || docDescTextWidth
            < docDescElementWidth) {
            $(this).removeClass().text('');
        }

    });

    $('.moreLess').click(function () {
        if ($(this).text() === 'more') {
            // Move to the previous div and swap the class so that the full text is displayed
            $(this).parent().prev().removeClass().addClass('documentDescriptionLess');
            $(this).text('less');
        } else {
            $(this).parent().prev().removeClass().addClass('documentDescriptionMore');
            $(this).text('more');
        }
    });
}
(function() {
    // Dropdown with display value handler
    const dropdownsWithDisplayValue = document.querySelectorAll('select[data-display-value-id]');
    dropdownsWithDisplayValue.forEach(select => {
        select.addEventListener('change', function() {
            const displayValueId = this.getAttribute('data-display-value-id');
            const displayValueInput = document.getElementById(displayValueId);
            if (displayValueInput) {
                displayValueInput.value = this.options[this.selectedIndex].text;
            }

            // Specific handler for proceeding type dropdown
            const orderTypeId = this.getAttribute('data-order-type-id');
            const descriptionId = this.getAttribute('data-description-id');
            const larScopeId = this.getAttribute('data-lar-scope-id');

            if (orderTypeId) {
                document.getElementById(orderTypeId).value = this.options[this.selectedIndex].getAttribute('order-type-required') || '';
            }
            if (descriptionId) {
                document.getElementById(descriptionId).value = this.options[this.selectedIndex].getAttribute('proceeding-description') || '';
            }
            if (larScopeId) {
                document.getElementById(larScopeId).value = this.options[this.selectedIndex].getAttribute('lar-scope') || '';
            }
        });
    });

    // Delete button handler
    const deleteButtons = document.querySelectorAll('button[data-delete-id]');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const deleteId = this.getAttribute('data-delete-id');
            const inputId = this.getAttribute('data-delete-input-id');
            const input = document.getElementById(inputId);
            if (input) {
                input.value = deleteId;
            }
            // Form submission is handled by the button type="submit"
        });
    });
})();
