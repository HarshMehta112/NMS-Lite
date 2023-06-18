var genericAjaxCall={

    ajaxpost: function (request){
        $.ajax({

            type:'POST',

            url:request.url,

            data : request.data,

            success : function (data)
            {
                let callbacks;

                if(request.callback!==undefined)
                {
                    callbacks = $.Callbacks();

                    callbacks.add(request.callback);

                    callbacks.fire(data);

                    callbacks.remove(request.callback);
                }

            },
            timeout: 15000
        });
    },
        ajaxget: function ajaxCall (result){
        $.ajax({
            type:result.type,

            url:result.url,

            data: result.data,

            dataType: result.dataType,

            success: function (data){
                if(result.hasOwnProperty('callback')){
                    result.callback(data);
                }
            },
            timeout: 15000
        });

    }
};