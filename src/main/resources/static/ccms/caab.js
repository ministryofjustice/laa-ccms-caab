document.addEventListener('DOMContentLoaded', function () {
    toggleDiv();
});

function toggleDiv() {
    var documentDescription = document.querySelector('.documentDescriptionMore');
    var docDescElementWidth = documentDescription ? documentDescription.getBoundingClientRect().width : 0;

    document.querySelectorAll('.moreLess').forEach(function (moreLess) {
        var previousDescription = moreLess.parentElement ? moreLess.parentElement.previousElementSibling : null;
        var span = previousDescription ? previousDescription.querySelector('span') : null;
        var docDescTextWidth = span ? span.getBoundingClientRect().width : 0;
        if (!previousDescription || previousDescription.textContent.length === 0 || docDescTextWidth < docDescElementWidth) {
            moreLess.removeAttribute('class');
            moreLess.textContent = '';
        }

    });

    document.querySelectorAll('.moreLess').forEach(function (moreLess) {
        moreLess.addEventListener('click', function () {
            var previousDescription = moreLess.parentElement ? moreLess.parentElement.previousElementSibling : null;
            if (!previousDescription) {
                return;
            }

            if (moreLess.textContent === 'more') {
            // Move to the previous div and swap the class so that the full text is displayed
                previousDescription.removeAttribute('class');
                previousDescription.classList.add('documentDescriptionLess');
                moreLess.textContent = 'less';
            } else {
                previousDescription.removeAttribute('class');
                previousDescription.classList.add('documentDescriptionMore');
                moreLess.textContent = 'more';
            }
        });
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
