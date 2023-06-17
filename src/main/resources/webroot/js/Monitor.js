$(function (){monitormain.onload()})

var monitormain = {

    onload: function ()
    {
        let request = {

            url: "LoadMonitorTable",

            data: "",

            callback: monitorcallback.onload,
        }
        genericAjaxCall.ajaxpost(request);
    },

    info : function (event) {


        var events = $(event.target);

        var row = events.closest("tr")

        var id = row.find("td:nth-child(1)").text();

        console.log(id);

        devicemain.onload(id);

    },


    deletemonitor: function (event)
    {
        var events = $(event.target);

        var row = events.closest("tr")

        var id = {"id":row.find("td:nth-child(1)").text()};

        console.log(id)

        let request = {

            url: "DeleteMonitorDevice",

            data: JSON.stringify(id),

        };

        if(confirm("Are you confirm to delete Monitor device"))
        {
            genericAjaxCall.ajaxpost(request);

            toastr.success("Monitor device deleted successfully")

        }
        monitormain.onload();
    },

};

var monitorhelper = {

    adddata: function (data)
    {
        $.each(JSON.parse(data), function (key,value)
        {
            table.row.add([value.DEVICEID,value.NAME, value.IPADDRESS, value.TYPE,value.STATUS,
                "<button onclick='monitormain.deletemonitor(event)' class='btn' style='margin-left: 5px'>Delete</button>" +
                "<button onclick='monitormain.info(event)' class='btn' style='margin-left: 5px'>View</button>"]).draw();

        });
    },



};

var monitorcallback = {

    onload: function (data)
    {
        $("#monitors").dataTable().fnClearTable();

        table = $('#monitors').DataTable({lengthMenu: [10, 20, 50, 100, 200, 500],destroy:true,"bDestroy": true});

        monitorhelper.adddata(data);
    },
}

