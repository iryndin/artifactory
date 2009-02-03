var StyledCheckbox = {
    onmouseover: function(button) {
        DomUtils.addHoverStyle(button);
    },

    onmouseout: function(button) {
        DomUtils.removeHoverStyle(button);
    },

    onclick: function(button) {
        var checkbox = button.parentNode.getElementsByTagName('input')[0];
        checkbox.checked = !checkbox.checked;

        if (checkbox.checked) {
            button.className = 'styled-checkbox styled-checkbox-checked';
            button.blur();
        } else {
            button.className = 'styled-checkbox styled-checkbox-unchecked';
        }
        button['cssBefore-hover'] = button.className;
    }
};
