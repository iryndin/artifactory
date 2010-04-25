SyntaxHighlighter.hasFlash = function() {
    var stdPlugin = (navigator.mimeTypes
            && navigator.mimeTypes["application/x-shockwave-flash"]
            && navigator.mimeTypes["application/x-shockwave-flash"].enabledPlugin)
            || navigator.plugins["Shockwave Flash"]
            || navigator.plugins["Shockwave Flash 2.0"];
    if (stdPlugin) {
        return true;
    }
    if (window.ActiveXObject) {
        try {
            return new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
        } catch (e) {
        }
    }
    return false;
};

SyntaxHighlighter.byId = function(id, clipboardSwf, brush, gutter, toolbar, autoLinks, wrapLines) {
    // rebuild brushes if needed (more brushes might be added with ajax
    var brushes = SyntaxHighlighter.vars.discoveredBrushes;
    if (brushes && !brushes[brush]) {
        SyntaxHighlighter.vars.discoveredBrushes = null;
    }

    // config copy swf
    if (SyntaxHighlighter.hasFlash()) {
        SyntaxHighlighter.config.clipboardSwf = clipboardSwf;
    }

    SyntaxHighlighter.highlight({
        brush: brush,
        gutter:gutter,
        toolbar:toolbar,
        'auto-links':autoLinks ,
        'wrap-lines':wrapLines
    }, document.getElementById('code-' + id));
};