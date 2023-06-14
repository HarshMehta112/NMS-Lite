$(document).ready(function() {

    setTimeout(dashboardmain.onload(),1000)
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
                  console.log(jsonarray[0]["UP"])

                  document.getElementById("up").innerHTML = jsonarray[0]["UP"]

                  document.getElementById("down").innerHTML = jsonarray[0]["DOWN"]

                  document.getElementById("total").innerHTML = jsonarray[0]["UP"] + jsonarray[0]["DOWN"]


              }

            })



        });
    }
})

var dashboardmain = {

    logout : function () {
        let request = {

            url: "logout",

        };
        genericAjaxCall.ajaxpost(request);
    },

    updateTableData : function (data,metricType,table)
    {
        $("#"+table).dataTable().fnClearTable();

        console.log(metricType)

        let dataTable = $("#"+table).DataTable({
            searching: false, paging: false, info: false,destroy:true,"bDestroy": true,

            data: data,
            columns: [
                {data: 'IPADDRESS'},
                {targets:1 , data: metricType}
            ],
            order: [[1, 'Desc']],
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

