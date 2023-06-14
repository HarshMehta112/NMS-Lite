var devicemain = {
    onload: function (deviceId){

        $('#monitorbody').html('<div class="box-container"><div class="box box1"><div class="text"><h2 id="hostname" class="topic-heading">--</h2><h3 class="topic">Hostname</h3></div></div><div class="box box2"><div class="text"><h2 id="disk" class="topic-heading">--</h2><h3 class="topic">Disk Used(%)</h3></div></div><div class="box box3"><div class="text"><h2 id="memory" class="topic-heading">--</h2><h3 class="topic">Memory Used(%)</h3></div></div><div class="box box4"><div class="text"><h2 id="cpu" class="topic-heading">--</h2><h3 class="topic">CPU Used(%)</h3></div></div><div class="box box5"><div class="text"><h2 id="uptime" class="topic-heading">--</h2><h3 class="topic">Uptime</h3></div></div></div>')

        console.log(deviceId)
        let request = {

            url: "deviceInfo",

            data : {deviceId},

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

        document.getElementById("disk").innerHTML =data['disk.used.percentage'];

        document.getElementById("uptime").innerHTML =data['uptime'];

        document.getElementById("hostname").innerHTML =data['system.name'];

        document.getElementById("cpu").innerHTML =data['cpu.user.percentage'];

        document.getElementById("memory").innerHTML =data['memory.used.percentage'];


    }
}

function getData(data)
{
    console.log("getData : " + data);



}