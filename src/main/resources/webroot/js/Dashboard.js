$(document).ready(function() {

    dashboardmain.onload();
});


$(function() {

    var eventBus = new EventBus('/login/eventbus');

    eventBus.onopen = function () {

        eventBus.registerHandler('updates.data', function (err, msg) {

            console.log(JSON.parse(msg.body))

            let array = JSON.parse(msg.body)

            array.forEach(function (jsonarray){
              if(jsonarray[0].hasOwnProperty("MEMORY"))
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
              else if(jsonarray[0].hasOwnProperty("UP"))
              {
                  console.log(jsonarray[0]["UP"]);

                  $("#up").html(jsonarray[0]["UP"]);

                  $("#down").html(jsonarray[0]["DOWN"]);

                  $("#total").html(jsonarray[0]["UP"] + jsonarray[0]["DOWN"]);
              }

            })

        });
    }
})

var dashboardmain = {

    updateTableData : function (data,metricType,table)
    {
        $("#"+table).dataTable().fnClearTable();

        console.log(metricType)

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
        genericAjaxCall.ajaxpost(request);
    }

};

