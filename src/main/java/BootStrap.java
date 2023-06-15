import Verticle.DatabaseVerticle;
import Verticle.DiscoveryEngine;
import Verticle.PollingEngine;
import Verticle.PublicAPIVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootStrap
{
    private static final Logger logger = LoggerFactory.getLogger(BootStrap.class);

    public static void main(String[] args)
    {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(DatabaseVerticle.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                logger.info("Database verticle deployed Successfully");
            }
            else
            {
                logger.error("Some error occurred "+deplyment.cause().getMessage());

            }
        }).compose(result->vertx.deployVerticle(PublicAPIVerticle.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                logger.info("Public API deployed Successfully");
            }
            else
            {
                logger.error("Some error occurred "+deplyment.cause().getMessage());
            }
        })).compose(result->vertx.deployVerticle(DiscoveryEngine.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                logger.info("DiscoveryEngine deployed Successfully");
            }
            else
            {
                logger.error("Some error occurred "+deplyment.cause().getMessage());
            }
        })).compose(result-> vertx.deployVerticle(PollingEngine.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                logger.info("PollingEngine deployed Successfully");
            }
            else
            {
                logger.error("Some error occurred "+deplyment.cause().getMessage());
            }
        }));
        // why setWorkerPoolSize


    }
}
