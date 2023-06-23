var devicemain = {

    onload: function (id){

        // $('#monitorbody').load("./Device.html");

        $('#monitorbody').html('<div class=row><div class=box-container><div class="box box1"><div class=text><h2 class=topic-heading id=hostname>--</h2><h3 class=topic>Hostname</h3></div></div><div class="box box1"><div class=text><h2 class=topic-heading id=disk>--</h2><h3 class=topic>Disk Used(%)</h3></div></div><div class="box box1"><div class=text><h2 class=topic-heading id=memory>--</h2><h3 class=topic>Memory Used(%)</h3></div></div><div class="box box1"><div class=text><h2 class=topic-heading id=cpu>--</h2><h3 class=topic>CPU Used(%)</h3></div></div><div class="box box1"><div class=text><h2 class=topic-heading id=uptime>--</h2><h3 class=topic>Uptime</h3></div></div></div></div><div class=container><div class=row><div class=col-mod-12><table class=table><thead><tr class=table-header><th>Interface Name<th>Interface OUT Traffic (bps)<th>Interface IN Traffic (bps)<th>Interface Traffic (bps)<tbody><tr><td id=lo-name><td id=lo-TX-rate><td id=lo-RX-rate><td id=lo-total-rate><tr><td id=wl-name><td id=wl-TX-rate><td id=wl-RX-rate><td id=wl-total-rate><tr><td id=en-name><td id=en-TX-rate><td id=en-RX-rate><td id=en-total-rate></table></div></div></div>')

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

        $("#lo-name").html(data['lo.name'])

        $("#lo-RX-rate").html(data['loRxRate'])

        $("#lo-TX-rate").html(data['loTxRate'])

        $("#lo-total-rate").html(data['loTotalRate'])

        $("#wl-name").html(data['wl.name'])

        $("#wl-RX-rate").html(data['wlRxRate'])

        $("#wl-TX-rate").html(data['wlTxRate'])

        $("#wl-total-rate").html(data['wlTotalRate'])

        $("#en-name").html(data['en.name'])

        $("#en-RX-rate").html(data['enRxRate'])

        $("#en-TX-rate").html(data['enTxRate'])

        $("#en-total-rate").html(data['enTotalRate'])

    }
}

