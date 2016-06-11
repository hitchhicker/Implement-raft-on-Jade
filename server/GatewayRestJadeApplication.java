package server;

import util.Constants;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import server.resource.NativeJadeGatewayResource;
import server.resource.SimplePostResource;

public class GatewayRestJadeApplication extends Application {
	@Override
    public Restlet createInboundRoot() {
    	Router router = new Router(getContext());
    	router.attach(
         		Constants.SIMPLE_POST_ADDRESS,
         		SimplePostResource.class);
    	router.attach(
         		Constants.AGENT_NATIVE_ADDRESS,
         		NativeJadeGatewayResource.class);
         return router;
    }
}
