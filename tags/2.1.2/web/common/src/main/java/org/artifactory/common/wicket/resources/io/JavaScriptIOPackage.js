var IOUtils = {
    post: function(url, params) {
        // create form
        var form = document.createElement('form');
        form.method = 'post';
        form.action = url;
        form.style.display = 'none';

        // add fields
        for (var name in params) {
            var value = params[name];
            var field = document.createElement('input');
            field.type = 'hidden';
            field.name = name;
            field.value = value;
            form.appendChild(field);
        }

        // submit form
        document.body.appendChild(form);
        form.submit();
    }
};