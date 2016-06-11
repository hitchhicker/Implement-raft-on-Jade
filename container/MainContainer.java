package container;

import agents.Member;
import agents.Reception;
import agents.StateMachine;
import client.RestClientMain;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import server.GatewayRestJadeMainServer;

public class MainContainer {
	final static int NUM_SITE = 5;
	
	public static void main (String[] args) {
		final String MAIN_PROPERTIES_FILE = "config/mainContainerConfig";
		
		Runtime rt = Runtime.instance();
		Profile p = null;
		GatewayRestJadeMainServer.startGatewaytRestJadeServer();
		try{
			p = new ProfileImpl(MAIN_PROPERTIES_FILE);
			AgentContainer mc = rt.createMainContainer(p);
			for (int i = 0; i< NUM_SITE; i++) {
				// create server member
				AgentController member = mc.createNewAgent(""+i, Member.class.getName(), new Object[]{NUM_SITE});
				member.start();
				// create statemachine for each server
				AgentController stateMachine = mc.createNewAgent("StateMachine" + i, StateMachine.class.getName(), null);
				stateMachine.start();
			}
			// create reception agent
			AgentController reception = mc.createNewAgent("Reception", Reception.class.getName(), null);
			reception.start();
		} catch(Exception ex) {
			System.out.println(ex);
		}
		try {
			Thread.sleep(4000);
			RestClientMain.userLoop();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
