var SubMenuPanel = {
    OPENED_CSS: 'sub-menu-opened',
    CLOSED_CSS: 'sub-menu-closed',

    toogleMenu: function(cookieName, link) {
        var menuItem = DomUtils.findParent(link, 'li')
        var menuGroup = DomUtils.nextSibling(menuItem);
        var isOpened = menuGroup.className == SubMenuPanel.OPENED_CSS;

        if (isOpened) {
            // close menu
            CookieUtils.clearCookie(cookieName);
            menuItem.className = menuItem.className.replace(/menu-group-opened/, 'menu-group-enabled');
            menuGroup.className = SubMenuPanel.CLOSED_CSS;
        } else {
            // open menu
            CookieUtils.setCookie(cookieName, 'true');
            menuItem.className = menuItem.className.replace(/menu-group-enabled/, 'menu-group-opened');
            menuGroup.className = SubMenuPanel.OPENED_CSS;
        }
        return false;
    }
};


var CookieUtils = {
    setCookie: function (name, value, days) {
        var expires;
        if (days) {
            var date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = '; expires=' + date.toGMTString();
        } else {
            expires = '';
        }
        document.cookie = name + '=' + escape(value) + expires + '; path=/';
    },

    clearCookie: function (name) {
        CookieUtils.setCookie(name, '', -1);
    }
};