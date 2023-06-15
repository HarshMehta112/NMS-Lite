import Utils.UserConfig;
import Verticle.DatabaseVerticle;
import Verticle.DiscoveryEngine;
import Verticle.PollingEngine;
import Verticle.PublicAPIVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class BootStrap
{
    private static final Logger logger = LoggerFactory.getLogger(BootStrap.class);


    static ArrayList<String> deploymentIds = new ArrayList<>();

    //first server then DB
    public static void main(String[] args)
    {
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(UserConfig.APPLICATION_THREAD_COUNT));

        vertx.deployVerticle(PublicAPIVerticle.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                deploymentIds.add(deplyment.result());

                logger.info("Public API deployed Successfully");
            }
            else
            {
                logger.error("Some error occurred "+deplyment.cause().getMessage());
            }

        }).compose(result->vertx.deployVerticle(DatabaseVerticle.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                deploymentIds.add(deplyment.result());

                logger.info("Database verticle deployed Successfully");
            }
            else
            {
                logger.error("Some error occurred "+deplyment.cause().getMessage());

            }

        })).compose(result->vertx.deployVerticle(DiscoveryEngine.class.getName(),new DeploymentOptions().setWorkerPoolSize(1)).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                deploymentIds.add(deplyment.result());

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
                deploymentIds.add(deplyment.result());

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
