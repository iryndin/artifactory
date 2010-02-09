var StyledTabbedPanel = {
    init: function(tabsContainerId, moveLeftId, moveRightId) {
        var tabsContainer = dojo.byId(tabsContainerId);
        var moveLeft = dojo.byId(moveLeftId);
        var moveRight = dojo.byId(moveRightId);
        var tabs = tabsContainer.getElementsByTagName('li');
        var height = 100;

        tabsContainer.scrollTop = document._tabScrollTop;

        function move(dir) {
            var scrollTop = tabsContainer.scrollTop + height * dir;
            if (scrollTop >= 0 && scrollTop <= tabsContainer.scrollHeight - height) {
                tabsContainer.scrollTop = scrollTop;
                document._tabScrollTop = scrollTop;
            }
            initLinks();
            return false;
        }

        function initLinks() {
            // move buttons
            moveLeft.style.visibility = (tabsContainer.scrollTop - height >= 0) ? 'visible' : 'hidden';
            moveRight.style.visibility = (tabsContainer.scrollTop + height <= tabsContainer.scrollHeight - height) ? 'visible' : 'hidden';

            if (tabsContainer.scrollTop > tabsContainer.scrollHeight - height) {
                tabsContainer.scrollTop -= height;
            }
        }

        function initTabs() {
            var prevTab;
            dojo.forEach(tabs, function(tab) {
                DomUtils.removeStyle(tab, 'first-tab');
                DomUtils.removeStyle(tab, 'last-tab');

                if (prevTab && tab.offsetTop != prevTab.offsetTop) {
                    DomUtils.addStyle(tab, 'first-tab');
                    DomUtils.addStyle(prevTab, 'last-tab');
                }
                prevTab = tab;
            });

            DomUtils.addStyle(tabs[0], 'first-tab');
            DomUtils.addStyle(tabs[tabs.length - 1], 'last-tab');
        }

        moveLeft.onclick = function() {
            return move(-1);
        }
        moveRight.onclick = function() {
            return move(1);
        }


        dojo.disconnect(StyledTabbedPanel.onresize);
        StyledTabbedPanel.onresize = dojo.connect(window, 'onresize', function() {
            initLinks();
            initTabs();
        });

        // init
        initLinks();
        initTabs();

        dojo.addOnLoad(function() {
            setTimeout(function() {
                initLinks();
                initTabs();
            }, 100);
        });
    }
};