$(function (){discoverymain.onload()})
var discoverymain = {

    onload: function ()
    {
        let request = {

            url: "Load",

            callback: discoverycallback.onload,
        };
        genericAjaxCall.ajaxget(request);
    },

    add: function ()
    {

        let param = $('#monitor').serializeArray().reduce(function (finalParam, currentValue)
        {
            finalParam[currentValue.name] = currentValue.value;
            return finalParam;
        }, {})

        if (discoveryhelper.validate(param.name, param.ip, param.type, param.username, param.password))
        {
            let request = {

                url: "Add",

                data: JSON.stringify({name:param.name,ip:param.ip,type:param.type,username:param.username,password:param.password}),

                callback: discoverycallback.add,

                success:toastr.success(param.ip,"Discovery Device Added Successfully")
            };
            genericAjaxCall.ajaxpost(request);

            $("#myModal").hide();

            discoverymain.onload();

        }
    },


    update: function ()
    {
        let id = $("#rawid").val();

        let param = $('form').serializeArray().reduce(function (finalParam, currentValue)
        {
            finalParam[currentValue.name] = currentValue.value;
            return finalParam;
        }, {});

        param.id = id;

        if (discoveryhelper.validate(param.name, param.ip, param.type, param.username, param.password))
        {
            let request = {

                url: "Edit",

                data: JSON.stringify(param),

                callback: discoverycallback.update,

                success:toastr.success(param.ip,"Discovery Device updated Successfully")
            };
            genericAjaxCall.ajaxpost(request);

            $("#myModalUpdate").hide();

            discoverymain.onload();
        }
    },


    deletemonitor: function (event)
    {
        var a = $(event.target);

        var row = a.closest("tr")

        var id = row.find("td:nth-child(1)").text();

        let request = {

            url: "Delete",

            data: {id},

            callback: discoverycallback.deletemonitor,

            success:toastr.success("discovery device deleted successfully")
        };
        genericAjaxCall.ajaxpost(request);
        discoverymain.onload();
    },

    discover: function (event){
        var a = $(event.target);

        var row = a.closest("tr")

        var id = row.find("td:nth-child(1)").text();

        var ip = row.find("td:nth-child(3)").text();

        let request = {

            url: "RunDiscovery",

            data: {id},

            success:toastr.success(ip,"discovery started successfully"),

            callback: location.reload,

        };
        genericAjaxCall.ajaxpost(request);

    },


    provision: function (event){
        var a = $(event.target);

        var row = a.closest("tr")

        var id = row.find("td:nth-child(1)").text();

        var ip = row.find("td:nth-child(3)").text();

        let request = {

            url: "provision",

            data: {id},

            success:toastr.success(ip,"provision process started successfully"),

            callback: location.reload,

        };
        genericAjaxCall.ajaxpost(request);

    }



};

var discoveryhelper = {

    adddata: function (data)
    {
        console.log(data)

        $.each(JSON.parse(data), function (key,value)
        {
            if(value.PROVISION === false)
            {
                table.row.add([value.DEVICEID,value.NAME, value.IPADDRESS, value.TYPE,
                    "<button onclick='discoverymain.discover(event)' class='btn' style='margin-left: 5px'>Run</button>" +
                    "<button onclick='discoverycallback.editdata(event)'  class='btn' style='margin-left: 5px'>Edit</button>" +
                    "<button onclick='discoverymain.deletemonitor(event)' id='DeleteBtn'  class='btn' style='margin-left: 5px'>Delete</button>"]).draw();
            }
            else {

                table.row.add([value.DEVICEID, value.NAME, value.IPADDRESS, value.TYPE,
                    "<button onclick='discoverymain.discover(event)'  class='btn' style='margin-left: 5px'>Run</button>" +
                    "<button onclick='discoverycallback.editdata(event)' class='btn' style='margin-left: 5px'>Edit</button>" +
                    "<button onclick='discoverymain.deletemonitor(event)' id='DeleteBtn'  class='btn' style='margin-left: 5px'>Delete</button>" +
                    "<button onclick='discoverymain.provision(event)' class='btn' style='margin-left: 5px'>Provision</button>"]).draw();
            }
        });
    },

    validate: function (name, ip, type, username, password)
    {
        if (name == "")
        {
            discoveryhelper.customalert(".failure", "Enter Valid Name");
            return false;
        }
        if (ip == "")
        {
            discoveryhelper.customalert(".failure", "Enter IP");
            return false;
        }


        if (type != "ping" && (username == "" || password == ""))
        {
            discoveryhelper.customalert(".failure", "Enter Username & Password");
            return false;
        }
        return true;
    },

    showssh: function ()
    {
        if ($("#updatetype").val() == "ssh")
        {
            $("#updatesshdivision").show();

        } else if ($("#type").val() == "ssh")
        {
            $("#sshdivision").show();

        } else
        {
            $("#sshdivision").hide();

            $("#updatesshdivision").hide();
        }
    },

    customalert: function (id, message)
    {
        $(id).text(message);

        $(id).show();

        setTimeout(function ()
        {
            $(id).hide();

        }, 2000);
    },

    floatbtn: function ()
    {
        $("#myModal").show();
    },

    closeadd: function ()
    {
        $("#myModal").hide();
    },

    closeupdate: function ()
    {
        $("#myModalUpdate").hide();
    },

};
var discoverycallback = {

    onload: function (data)
    {
        $("#monitors").dataTable().fnClearTable();
        table = $('#monitors').DataTable({lengthMenu: [3, 5, 10, 20, 50, 100, 200, 500],destroy:true,"bDestroy": true});

        discoveryhelper.adddata(data, table);
    },

    editdata: function (event)
    {
        $("#myModalUpdate").show();

        var a = $(event.target);

        console.log(a)

        var row = a.closest("tr")

        var beforeEditid = row.find("td:nth-child(1)").text();

        var beforeEditname = row.find("td:nth-child(2)").text();

        var beforeEditip = row.find("td:nth-child(3)").text();

        var beforeEdittype = row.find("td:nth-child(4)").text();

        let id = beforeEditid;

        let name = beforeEditname;

        let ip = beforeEditip;

        let type = beforeEdittype;

        $("#rawid").val(id);

        $("#updateip").val(ip);

        $("#updatename").val(name);

        $("#updatetype").val(type);

        if (type == "ssh")
        {
            $("#updatesshdivision").show();

        } else
        {
            $("#updatesshdivision").hide();
        }
    },

}
