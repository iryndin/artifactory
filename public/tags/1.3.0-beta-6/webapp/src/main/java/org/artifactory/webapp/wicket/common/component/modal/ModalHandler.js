var ModalHandler = {};

ModalHandler.onPopup = function() {
    var modal = Wicket.Window.current;

    // set dimentions
    var width = modal.settings.width;
    var height = modal.settings.height;

    if (width == 0) {
        width = modal.content.scrollWidth;
    }
    if (height == 0) {
        height = modal.content.scrollHeight + 1;
    }

    var maxWidth = Wicket.Window.getViewportWidth();
    var maxHeight = Wicket.Window.getViewportHeight() - 90;

    if (width > maxWidth) width = maxWidth;
    if (height > maxHeight) height = maxHeight;

    modal.window.style.width = width + modal.settings.widthUnit;
    modal.content.style.height = height + modal.settings.heightUnit;

    // centering the window
    var center = ( Wicket.Window.getViewportHeight() - height) / 2;
    center = Math.max(center, 5);
    modal.window.style.top = center + modal.settings.heightUnit;

    document.onkeyup = function (e) {
        var event = e ? e : window.event;

        var EscKey = 27;
        if (event.keyCode == EscKey) {
            Wicket.Window.close();
        }
    };
};

ModalHandler.onClose = function() {
    window.onkeypress = null;
}

ModalHandler.autoSize = function() {
    var modal = Wicket.Window.current;
    if (!modal) {
        return;
    }

    // set dimentions
    var height = modal.settings.height;
    if (height == 0) {
        height = modal.content.scrollHeight;
    }

    var maxHeight = Wicket.Window.getViewportHeight();
    if (height > maxHeight) height = maxHeight;

    modal.content.style.height = height + modal.settings.heightUnit;
};

/**
 * Returns the modal window markup with specified element identifiers.
 */
Wicket.Window.getMarkup =
function(idWindow, idClassElement, idCaption, idContent, idTop, idTopLeft, idTopRight, idLeft,
         idRight, idBottomLeft, idBottomRight, idBottom, idCaptionText, isFrame) {
    var s =
            "<div class=\"wicket-modal\" id=\"" + idWindow +
            "\" style=\"top: 10px; left: 10px; width: 100px;\">" +
            "<div id=\"" + idClassElement + "\">" +

            "<div class=\"w_top_1\">" +

            "<div class=\"w_topLeft\" id=\"" + idTopLeft + "\">" +
            "</div>" +

            "<div class=\"w_topRight\" id=\"" + idTopRight + "\">" +
            "</div>" +

            "<div class=\"w_top\" id='" + idTop + "'>" +
            "</div>" +

            "</div>" +

            "<div class=\"w_left\" id='" + idLeft + "'>" +
            "<div class=\"w_right_1\">" +
            "<div class=\"w_right\" id='" + idRight + "'>" +
            "<div class=\"w_content_1\" onmousedown=\"if (Wicket.Browser.isSafari()) { event.ignore = true; }  else { Wicket.stopEvent(event); } \">" +
            "<div class=\"w_caption\"  id=\"" + idCaption + "\">" +
            "<a class=\"w_close\" href=\"#\"></a>" +
            "<span id=\"" + idCaptionText + "\" class=\"w_captionText\"></span>" +
            "</div>" +

            "<div class=\"w_content_2\">" +
            "<div class=\"w_content_3\">" +
            "<div class=\"w_content\">";
    if (isFrame) {
        s +=
        "<iframe src='\/\/:' frameborder=\"0\" id='" + idContent +
        "' allowtransparency=\"false\" style=\"height: 200px\">" +
        "</iframe>";
    } else {
        s +=
        "<div id='" + idContent + "' class='w_content_scroll'></div>";
    }
    s +=
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "</div>" +


    "<div class=\"w_bottom_1\" id=\"" + idBottom + "\">" +

    "<div class=\"w_bottomRight\"  id=\"" + idBottomRight + "\">" +
    "</div>" +

    "<div class=\"w_bottomLeft\" id=\"" + idBottomLeft + "\">" +
    "</div>" +

    "<div class=\"w_bottom\" id=\"" + idBottom + "\">" +
    "</div>" +


    "</div>" +


    "</div>" +
    "</div>";

    return s;
}