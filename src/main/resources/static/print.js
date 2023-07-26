function printSections(className) {
    var printContents = document.getElementsByClassName(className);
    var originalContents = document.body.innerHTML;

    var printContentHtml = '';
    for (var i = 0; i < printContents.length; i++) {
        printContentHtml += printContents[i].outerHTML;
    }

    document.body.innerHTML = printContentHtml;
    window.print();

    document.body.innerHTML = originalContents;
}