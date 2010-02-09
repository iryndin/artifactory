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
        StyledCheckbox.updateStyle(button, checkbox.checked);
    },

    update: function(checkbox) {
        var button = checkbox.parentNode.getElementsByTagName('button')[0];
        StyledCheckbox.updateStyle(button, checkbox.checked);
    },

    updateStyle: function(button, checked) {
        if (checked) {
            button.className = 'styled-checkbox styled-checkbox-checked';
        } else {
            button.className = 'styled-checkbox styled-checkbox-unchecked';
        }
    }
};
