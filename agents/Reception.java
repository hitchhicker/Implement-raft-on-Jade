package agents;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.ToApplyEntry;

public class Reception extends Agent{
	AID currentLeader = null;
	
	protected void setup() {
		addBehaviour(new RequestFromUser());
		addBehaviour(new OnUpdateLeader());
		addBehaviour(new InformFromLearder());
	}
	
	/**
	 * Reception receives a request from Gateway Agent
	 * Perfomative : REQUEST
	 * ConversationId : None
	 */
	private class RequestFromUser extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage message = receive(mt);
			
			if (message != null) {
				String content = message.getContent();
				
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.addReceiver(currentLeader);
				// make message to json
				ToApplyEntry entry = new ToApplyEntry();
				ObjectMapper mapper = new ObjectMapper();
				StringWriter sw = new StringWriter();
				if (!content.isEmpty()) {
					if (content.equals("shownode")) {
						// Delay 200ms to display for better demonstration
						addBehaviour(new WakerBehaviour(getAgent(), 200) {
							protected void onWake() {
								System.out.printf("%-20s%-20s%-20s%-20s%-20s%-20s%-20s\n", "server", "role", "term", "last_log_index", 
										"last_log_term", "commit_index", "last_applied");
								msg.setContent(content);
								msg.setConversationId("shownode");
								send(msg);
							}
						});
					}
					else if (content.equals("showlog")) {
						// Delay 200ms to display for better demonstration
						addBehaviour(new WakerBehaviour(getAgent(), 200) {
							protected void onWake() {
								msg.setContent(content);
								msg.setConversationId("showlog");
								send(msg);
							}
						});
					}
					else if (paramsCheck(content.split(" "))) {
						String[] params = content.split(" ");
						entry.setCommand(params[0]);
						entry.setKey(params[1]);
						if (params.length == 3)
							entry.setValue(params[2]);
						else
							entry.setValue(null);
			  			try {
			  				mapper.writeValue(sw, entry);
			  				String s = sw.toString();
			  				msg.setContent(s);
			  				msg.setConversationId("RU"); // RU :  Request User
			  				send(msg);
			  			}catch(Exception ex) {
			  				ex.printStackTrace();
			  			}
					}
				}
	  			ACLMessage reply = message.createReply();
	  			reply.setPerformative(ACLMessage.INFORM);
	  			send(reply);
			} else 
				block();
		}
	}
	
	/**
	 * Reception receives a inform from leader server
	 * Performative : REQUEST
	 * ConversationId : None
	 */
	private class InformFromLearder extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage message = receive(mt);
			
			if (message != null) {
				String content = message.getContent();
				ObjectMapper mapper  = new ObjectMapper();
				String key = null;
				String value = null;
				try{
					ToApplyEntry entry = mapper.readValue(content, ToApplyEntry.class);
					key = entry.getKey();
					value = entry.getValue();
					System.out.println("[Reception] Commit " + entry.displayEntry());
				}catch(Exception ex){
					ex.printStackTrace();
				}
			} else
				block();
		}
	}
	
	/**
	 * Reception receives a subscribtion from leader
	 * Performative : SUBSCRIBE
	 * ConversationId : None
	 */
	private class OnUpdateLeader extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
			ACLMessage message = receive(mt);
			
			if (message != null) {
				currentLeader = new AID(message.getContent(), AID.ISGUID);
			} else 
				block();
		}
	}
	
	/**
	 * Check if the parameter is good before sending to leader
	 * @param params : a String array with command, key, value(optional)
	 * @return true if it's OK
	 */
	public boolean paramsCheck(String[] params) {
		switch (params[0].toLowerCase()) {
		case "put":
			if (params.length != 3) {
				System.out.println("parameters error");
				return false;
			}
			break;
		case "get":
			if (params.length != 2) {
				System.out.println("parameters error");
				return false;
			}
			break;
		case "delete":
			if (params.length != 2) {
				System.out.println("parameters error");
				return false;
			}
			break;
		case "update":
			if (params.length != 3) {
				System.out.println("parameters error");
				return false;
			}
			break;
		default:
			System.out.println("unknown command");
			return false;
		}
		return true;
	}
}
