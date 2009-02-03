var StyledRadio = {
    onmouseover: function(button) {
        DomUtils.removeHoverStyle(button);
    },

    onmouseout: function(button) {
        DomUtils.removeHoverStyle(button);
    },

    onclick: function(button) {
        var checkbox = button.parentNode.getElementsByTagName('input')[0];
        checkbox.checked = true;

        dojo.forEach(document.getElementsByName(checkbox.name), function(checkbox) {
            var button = checkbox.parentNode.getElementsByTagName('button')[0];
            if (checkbox.checked) {
                button.className = 'styled-checkbox styled-checkbox-checked';
                button.blur();
            } else {
                button.className = 'styled-checkbox styled-checkbox-unchecked';
            }
            button['cssBefore-hover'] = button.className;
        });
    }
};
