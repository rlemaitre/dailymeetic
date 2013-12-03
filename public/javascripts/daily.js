// namespace
window.daily = {
    handler: {}
};

// Allow for console.log to not break IE
if (typeof window.console == "undefined" || typeof window.console.log == "undefined") {
    window.console = {
        log: function () {
        },
        info: function () {
        },
        warn: function () {
        }
    };
}
if (typeof window.console.group == 'undefined' || typeof window.console.groupEnd == 'undefined' || typeof window.console.groupCollapsed == 'undefined') {
    window.console.group = function () {
    };
    window.console.groupEnd = function () {
    };
    window.console.groupCollapsed = function () {
    };
}
if (typeof window.console.markTimeline == 'undefined') {
    window.console.markTimeline = function () {
    };
}
window.console.clear = function () {
};

// ready event
daily.ready = function () {

    // selector cache
    var
        $ui = $('.ui').not('.hover, .down'),
        $menu = $('#menu'),
        $hideMenu = $menu.find('.hide.item'),
        $sidebarButton = $('.attached.launch.button'),
        // alias
        handler
        ;

    // event handlers
    handler = {
        menu: {
            mouseenter: function () {
                $(this)
                    .stop()
                    .animate({
                        width: '180px'
                    }, 300, function () {
                        $(this).find('.text').text('Menu').show();
                    })
                ;
            },
            mouseleave: function (event) {
                $(this).find('.text').hide();
                $(this)
                    .stop()
                    .animate({
                        width: '70px'
                    }, 300, function () {
                        $(this).find('.text').hide();
                    })
                ;
            }

        }
    };
    // attach events
    $sidebarButton
        .on('mouseenter', handler.menu.mouseenter)
        .on('mouseleave', handler.menu.mouseleave)
    ;
    $menu
        .sidebar('attach events', '.launch.button, .launch.item')
        .sidebar('attach events', $hideMenu, 'hide')
    ;
};


// attach ready event
$(document)
    .ready(daily.ready)
;