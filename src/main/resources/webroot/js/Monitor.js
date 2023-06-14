$(function (){monitormain.onload()})

var bar;

var pie;

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

    close: function ()
    {
        $("#monitorModal").close();
    },

    info : function (event) {


        var a = $(event.target);

        var row = a.closest("tr")

        var id = row.find("td:nth-child(1)").text();

        console.log(id);

        devicemain.onload(id);

    },


    deletemonitor: function (event)
    {
        var a = $(event.target);

        var row = a.closest("tr")

        var id = row.find("td:nth-child(1)").text();

        console.log(id)

        let request = {

            url: "DeleteMonitorDevice",

            data: {id},

            success:toastr.success("discovery device deleted successfully")
        };
        genericAjaxCall.ajaxpost(request);
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

