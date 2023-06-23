import Utils.UserConfig;
import Verticle.DatabaseVerticle;
import Verticle.DiscoveryEngine;
import Verticle.PollingEngine;
import Verticle.PublicAPIVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootStrap
{
    private static final Logger logger = LoggerFactory.getLogger(BootStrap.class);

    public static Vertx vertx = Vertx.vertx();

    public static void closeVertxInstance()
    {
        logger.debug("Stopping all verticles....");

        vertx.close().onComplete(voidAsyncResult ->
        {
            if(voidAsyncResult.succeeded())
            {
                logger.debug("Vert.x instance closed successfully");
            }
            else
            {
                logger.debug(voidAsyncResult.cause().getMessage());
            }
        });
    }


    public static void main(String[] args)
    {
        CompositeFuture.all(vertx.deployVerticle(DatabaseVerticle.class.getName(), new DeploymentOptions().setInstances(2).setWorkerPoolName("Database Verticle").setWorkerPoolSize(UserConfig.DATABASE_VERTICLE_THREAD_COUNT)),

                vertx.deployVerticle(PublicAPIVerticle.class.getName(),new DeploymentOptions().setWorkerPoolName("PublicAPI Verticle").setWorkerPoolSize(UserConfig.PUBLIC_API_VERTICLE_THREAD_COUNT)),

                vertx.deployVerticle(DiscoveryEngine.class.getName(),new DeploymentOptions().setWorkerPoolName("DiscoveryEngine Verticle").setWorkerPoolSize(UserConfig.DISCOVERY_ENGINE_VERTICLE_THREAD_COUNT)),

                vertx.deployVerticle(PollingEngine.class.getName(),new DeploymentOptions().setWorkerPoolName("PollingEngine Verticle").setWorkerPoolSize(UserConfig.POLLING_ENGINE_VERTICLE_THREAD_COUNT)))

                .onComplete(compositeFutureAsyncResult ->
                {
                    if(compositeFutureAsyncResult.succeeded())
                    {
                        logger.debug("All verticles are deployed successfully");
                    }
                    else
                    {
                        closeVertxInstance();
                    }
                });




    }
}