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

        console.log('update device pae'+data)

        //TODO use jquery

        $("#disk").html(data['disk.used.percentage'])

        $("#uptime").html(data['uptime'])

        $("#hostname").html(data['system.name']);

        $("#cpu").html(data['cpu.user.percentage']);

        $("#memory").html(data['memory.used.percentage']);


    }
}


$(document).ready(function() {
    // Sample JSON array of timestamps and values
    var jsonData = [
        { timestamp: "2023-06-15 06:47:56", value: 8.03 },
        { timestamp: "2023-06-15 06:47:26", value: 8.04 },
        { timestamp: "2023-06-15 06:46:56", value: 8.05 },
        { timestamp: "2023-06-15 06:46:26", value: 8.06 },
        { timestamp: "2023-06-15 06:45:33", value: 8.07 },
        { timestamp: "2023-06-15 06:45:03", value: 8.08 },
        { timestamp: "2023-06-15 06:44:33", value: 8.09 },
        { timestamp: "2023-06-15 06:44:03", value: 8.11 },
        { timestamp: "2023-06-15 06:43:33", value: 8.12 }
    ];

    // Extract values and timestamps from jsonData
    var values = jsonData.map(function(entry) {
        return entry.value;
    });
    var timestamps = jsonData.map(function(entry) {
        return entry.timestamp;
    });

    // Create the bar chart
    var ctx = document.getElementById("myChart").getContext("2d");
    new Chart(ctx, {
        type: "bar",
        data: {
            labels: timestamps,
            datasets: [
                {
                    label: "cpu.user.percentage",
                    data: values,
                    backgroundColor: "rgba(54, 162, 235, 0.5)",
                    borderColor: "rgba(54, 162, 235, 1)",
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            scales: {
                x: {
                    display: false
                },
                y: {
                    beginAtZero: true
                }
            }
        }
    });
});