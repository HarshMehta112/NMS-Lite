$(document).ready(function() {

    dashboardmain.onload();
});


$(function() {

    let eventBus = new EventBus('/login/eventbus');

    eventBus.onopen = function () {

        eventBus.registerHandler('updates.data', function (err, msg) {

            console.log(JSON.parse(msg.body))

            let array = JSON.parse(msg.body)

            array.forEach(function (jsonarray)
            {
                if(jsonarray && jsonarray.length>0)
                {
                    if(jsonarray[0].hasOwnProperty("UP"))
                    {
                        $("#up").html(jsonarray[0]["UP"]);

                        $("#down").html(jsonarray[0]["DOWN"]);

                        $("#total").html(jsonarray[0]["TOTAL"]);

                        $("#unknown").html(jsonarray[0]["UNKNOWNS"])
                    }

                    else if(jsonarray[0].hasOwnProperty("MEMORY"))
                    {
                        dashboardmain.updateTableData(jsonarray,"MEMORY","memory")
                    }
                    else if(jsonarray[0].hasOwnProperty("DISK"))
                    {
                        dashboardmain.updateTableData(jsonarray,"DISK","disk")
                    }
                    else if(jsonarray[0].hasOwnProperty("CPU"))
                    {
                        dashboardmain.updateTableData(jsonarray,"CPU","cpu")
                    }
                }

            })

        });
    }
})

var dashboardmain = {

    updateTableData : function (data,metricType,table)
    {
        $("#"+table).dataTable().fnClearTable();

        let dataTable = $("#"+table).DataTable({

            searching: false, paging: false, info: false,destroy:true,

            data: data,

            columns: [
                {data: 'IPADDRESS'},
                {targets:1 , data: metricType}
            ],
            order: [[1, 'desc']],
        });
},

    onload : function ()
    {
        let request = {

            url: "dashboard"

        }

        setTimeout(function ()
        {
           genericAjaxCall.ajaxpost(request)

        },1000);

    }

};

