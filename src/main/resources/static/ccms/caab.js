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