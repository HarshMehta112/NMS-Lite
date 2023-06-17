import Utils.UserConfig;
import Verticle.DatabaseVerticle;
import Verticle.DiscoveryEngine;
import Verticle.PollingEngine;
import Verticle.PublicAPIVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootStrap
{
    private static final Logger logger = LoggerFactory.getLogger(BootStrap.class);

    public static Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(UserConfig.APPLICATION_THREAD_COUNT));

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
        CompositeFuture.all(vertx.deployVerticle(DatabaseVerticle.class.getName()),
                vertx.deployVerticle(PublicAPIVerticle.class.getName()),
                vertx.deployVerticle(DiscoveryEngine.class.getName()),
                vertx.deployVerticle(PollingEngine.class.getName())).onComplete(compositeFutureAsyncResult ->
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