var devicemain = {

    onload: function (id){

        // $('#monitorbody').load("./Device.html");

        $('#monitorbody').html('<div class="box-container"><div class="box box1"><div class="text"><h2 id="hostname" class="topic-heading">--</h2><h3 class="topic">Hostname</h3></div></div><div class="box box1"><div class="text"><h2 id="disk" class="topic-heading">--</h2><h3 class="topic">Disk Used(%)</h3></div></div><div class="box box1"><div class="text"><h2 id="memory" class="topic-heading">--</h2><h3 class="topic">Memory Used(%)</h3></div></div><div class="box box1"><div class="text"><h2 id="cpu" class="topic-heading">--</h2><h3 class="topic">CPU Used(%)</h3></div></div><div class="box box1"><div class="text"><h2 id="uptime" class="topic-heading">--</h2><h3 class="topic">Uptime</h3></div></div></div>')

        console.log(id)

        var ids = {"id":id};


        let request = {

            url: "deviceInfo",

            data : JSON.stringify(ids),

            callback: devicehelper.updateDevicePage
        }
        genericAjaxCall.ajaxpost(request);
    },

}

devicehelper =
{
    updateDevicePage : function (data)
    {
        data = JSON.parse(data)

        console.log('update device page'+data)

        $("#disk").html(data['disk.used.percentage'])

        $("#uptime").html(data['uptime'])

        $("#hostname").html(data['system.name']);

        $("#cpu").html(data['cpu.user.percentage']);

        $("#memory").html(data['memory.used.percentage']);


    }
}

