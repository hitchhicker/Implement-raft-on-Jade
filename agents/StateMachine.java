package agents;

import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.ToApplyEntry;

public class StateMachine extends Agent{
	HashMap<String, Object> map;
	protected void setup() {
		map = new HashMap<String, Object>();
		addBehaviour(new RequestFromMember());
	}
	
	/**
	 * State Machine receives a request from server
	 * Performative : REQUEST 
	 * ConversationId : None
	 */
	private class RequestFromMember extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage message = receive(mt);
			if (message != null) {
				String content = message.getContent();
				ObjectMapper mapper  = new ObjectMapper();
				String key = null;
				String value = null;
				String command = null;
				try{
					ToApplyEntry entry = mapper.readValue(content, ToApplyEntry.class);
					command = entry.getCommand();
					key = entry.getKey();
					value = entry.getValue();
					System.out.println("[" + getName().split("@")[0] + "] Apply "+ entry.displayEntry()); 
					applyCommand(command,key, value);
					displayMap();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			} else
				block();
		}
	}
	
	/**
	 * Apply command by his parameters
	 * @param command
	 * @param key
	 * @param value
	 */
	public void applyCommand(String command, String key, String value) {
		switch (command) {
		case "put":
			map.put(key, value);
			System.out.println("Put " + key + " with value " + value);
			break;
		case "get":
			if (map.get(key) != null)
				System.out.println("Value of " + key + " is " + map.get(key));
			else
				System.out.println("[ERR] Key " + key + " doesn't exist");
			break;
		case "delete":
			try {
				if (map.remove(key) != null)
					System.out.println(key + " is removed from DB.");	
				else 
					System.out.println("[ERR] Key " + key + " doesn't exist");
			} catch (Exception e) {
				System.out.println("[ERR] Key not found");
			}
			break;
		case "update":
			map.put(key, value);
			System.out.println("Update " + key + " with value " + value);
			break;
		default:
			System.out.println("[ERR] Command unknwon.");
			break;
		}
	}
	
	/**
	 * Display map
	 * Only for demonstration
	 */
	public void displayMap() {
		String output;
		output = "{";
		for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
			output += "'" + entry.getKey()+"': " + entry.getValue() + ", ";
		}
		if (output.length() > 1)
			output = output.substring(0, output.length() - 2);
		output += "}\n";
		System.out.print(output);
	}
}
