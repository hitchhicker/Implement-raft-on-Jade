package server;

import util.Constants;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class GatewayRestJadeMainServer {

	public static void startGatewaytRestJadeServer() {
		try {
			// Create a new Component.
		    Component component = new Component();
		    // Add a new HTTP server listening on port 8182.
		    component.getServers().add(Protocol.HTTP, Constants.REST_SERVER_PORT);
		    component.getDefaultHost().attach(new GatewayRestJadeApplication());
		   // Start the component.
		    component.start();
	   }
	  catch(Exception ex) {
		ex.printStackTrace();
	  }
	}
}
