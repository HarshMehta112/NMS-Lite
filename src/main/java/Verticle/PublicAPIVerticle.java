package Verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import Utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PublicAPIVerticle extends AbstractVerticle
{

    private static final Logger logger = LoggerFactory.getLogger(PublicAPIVerticle.class);

    EventBus eventBus;

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        eventBus = vertx.eventBus();

        Router router = Router.router(vertx);

        try
        {
            router.route().handler(BodyHandler.create());

            router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

            PropertyFileAuthentication authentication = PropertyFileAuthentication.create(vertx, Constants.PROPERTY_FILE_PATH);

            router.route().handler(StaticHandler.create().setCachingEnabled(false).setIndexPage("login.html"));

            router.route("/login/*").handler(RedirectAuthHandler.create(authentication,"login.html"));

            router.post("/loginHandler").handler(FormLoginHandler.create(authentication)).failureHandler(context->
            {
                context.reroute("/login/");
            });

            router.route("/login/*").handler(StaticHandler.create().setCachingEnabled(false).setIndexPage("Dashboard.html"));

            router.route("/login/Add").handler(routingContext ->
            {
                eventBus.request(Constants.ADD_DISCOVERY_DEVICE,routingContext.body().asJsonObject(),response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("added");

                        logger.info("Discovery Device Added");
                    }
                    else
                    {
                        routingContext.response().end("Not Added");

                        logger.info("Discovery Device Not Added");
                    }
                });

            });

            router.route("/login/LoadMonitorTable*").handler(routingContext ->
            {
                eventBus.<JsonArray>request(Constants.LOAD_MONITOR_DEVICE,"",response->
                {
                    if(response.succeeded())
                    {
                        logger.info("response from database verticle "+response.result().body());

                        routingContext.response().setStatusCode(200).end(response.result().body().encodePrettily());
                    }
                    //migrate into one method
                    else
                    {
                        logger.info("Some Problem in loading Monitor Devices");

                        routingContext.response().end("Some Problem in loading Monitor Devices");
                    }
                });
            });


            router.route("/login/Load").handler(routingContext ->
            {
                eventBus.<JsonArray>request(Constants.GET_ALL_DISCOVERY_DEVICE,"", response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end(response.result().body().encodePrettily());
                    }
                    else
                    {
                        logger.info("Some Problem in loading Discovery Devices");

                        routingContext.response().end("Some Problem in loading Discovery Devices");
                    }
                });
            });


            router.route("/login/Delete").handler(routingContext ->
            {
                eventBus.request(Constants.DELETE_DISCOVERY_DEVICE,routingContext.body().asJsonObject(),response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("Discovery Device Deleted");
                    }
                    else
                    {
                        routingContext.response().end("Some problem in deleting the discovery device");
                    }
                });
            });

            router.route("/login/DeleteMonitorDevice").handler(routingContext ->
            {
                logger.info("Delete Monitor Id "+routingContext.body().asJsonObject());

                eventBus.request(Constants.DELETE_MONITOR_DEVICE,routingContext.body().asJsonObject(),response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("Monitor Device Deleted");
                    }
                    else
                    {
                        routingContext.response().end("Some problem in deleting the monitor device");
                    }
                });
            });


            router.route("/login/RunDiscovery").handler(routingContext ->
            {
                eventBus.request(Constants.RUN_DISCOVERY,routingContext.body().asJsonObject(),response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("Device discovered successfully");
                    }
                    else
                    {
                        routingContext.response().end("Device not discovered");
                    }
                });
            });

            router.route("/login/provision").handler(routingContext ->
            {
                eventBus.request(Constants.RUN_PROVISION,routingContext.body().asJsonObject(),response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("Device provisioned successfully");
                    }
                    else
                    {
                        routingContext.response().end("Some error occurred device not provisioned");
                    }
                });
            });

            router.route("/login/dashboard").handler(routingContext ->
            {
                eventBus.request(Constants.DASHBOARD_LOAD,"",response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("Dashboard Loaded successfully");
                    }
                    else
                    {
                        routingContext.response().end("Some error occurred dashboard data loading");
                    }
                });
            });


            router.route("/login/deviceInfo").handler(routingContext ->
            {
                eventBus.<JsonObject>request(Constants.MONITOR_DEVICE_INFO,routingContext.request().getParam("deviceId"), response->
                {
                    if(response.succeeded())
                    {
                        logger.info("Response "+response.result().body().encodePrettily());

                        routingContext.response().end(response.result().body().toString());
                    }
                    else
                    {
                        routingContext.response().end("Some error occurred on loadinf device information");
                    }
                });
            });



            router.route("/login/Edit").handler(routingContext ->
            {
                eventBus.request(Constants.EDIT_DISCOVERY_DEVICE,routingContext.body().asJsonObject(),response->
                {
                    if(response.succeeded())
                    {
                        routingContext.response().setStatusCode(200).end("Edited");

                        logger.info("Discovery Device Edited");
                    }
                    else
                    {
                        routingContext.response().end("Not Edited");

                        logger.info("Discovery Device Not Edited");
                    }
                });
            });


            SockJSHandler jsHandler = SockJSHandler.create(vertx);

            SockJSBridgeOptions bridgeOptions = new SockJSBridgeOptions()
                    .addInboundPermitted(new PermittedOptions().setAddressRegex("updates.*"))
                    .addOutboundPermitted(new PermittedOptions().setAddressRegex("updates.*"));

            router.mountSubRouter("/login/eventbus",jsHandler.bridge(bridgeOptions));


            router.route("/login/logout").handler(context -> {

                context.clearUser();

                context.response().putHeader("location", "/login").setStatusCode(302).end();
            });
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        try
        {
            vertx.createHttpServer(new HttpServerOptions().setSsl(true).setKeyStoreOptions(new JksOptions().setPath(
                            Constants.SSL_KEYSTORE_PATH).setPassword(Constants.SSL_PASSWORD)))
                    .requestHandler(router).listen(8080).onComplete(ready ->
                    {
                        if(ready.succeeded())
                        {
                            logger.info("server started listening on port no 8080");
                        }
                        else
                        {
                            logger.info("some error occurred with server" + ready.cause().getMessage());
                        }
                    });
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        startPromise.complete();

    }
}