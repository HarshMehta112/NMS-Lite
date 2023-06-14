import Verticle.DatabaseVerticle;
import Verticle.DiscoveryEngine;
import Verticle.PollingEngine;
import Verticle.PublicAPIVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class BootStrap
{
    public static void main(String[] args)
    {
        Vertx vertx = Vertx.vertx();

        //use stander declaration
        vertx.deployVerticle(new DatabaseVerticle()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                System.out.println("Public API deployed Successfully");
            }
            else
            {
                System.out.println("Some error occurred "+deplyment.cause().getMessage());

            }
        });

        vertx.deployVerticle(PublicAPIVerticle.class.getName()).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                System.out.println("Public API deployed Successfully");
            }
            else
            {
                System.out.println("Some error occurred "+deplyment.cause().getMessage());
            }
        });

        vertx.deployVerticle(DiscoveryEngine.class.getName(),new DeploymentOptions().setWorkerPoolSize(4)).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                System.out.println("Public API deployed Successfully");
            }
            else
            {
                System.out.println("Some error occurred "+deplyment.cause().getMessage());
            }
        });

        // why setWorkerPoolSize
        vertx.deployVerticle(PollingEngine.class.getName(),new DeploymentOptions().setWorkerPoolSize(4)).onComplete(deplyment->
        {
            if(deplyment.succeeded())
            {
                System.out.println("Public API deployed Successfully");
            }
            else
            {
                System.out.println("Some error occurred "+deplyment.cause().getMessage());
            }
        });

    }
}
