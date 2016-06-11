package util;

import jade.lang.acl.ACLMessage;

import java.util.Map;
import java.util.HashMap;

public class Constants {
	/**
	 * Exemples de performatifs
	 */
	public static String REQUEST = "request";
	public static String INFORM = "inform";
	public static Map<String,Integer> PERFORMATIVES = new HashMap<String,Integer>();
	static {
		
		PERFORMATIVES.put(REQUEST,ACLMessage.REQUEST);
		PERFORMATIVES.put(INFORM,ACLMessage.INFORM);
	}
	/**
	 * Nom des Agents
	 */
	public static String REST_ADMIN_AGENT = "reception";

	/**
	 * Adressage REST
	 */
	public static int REST_SERVER_PORT = 8182;
	public static String REST_URL = "http://localhost:" + REST_SERVER_PORT;
	public static String SIMPLE_POST_ADDRESS = "/restjade/simple";
	public static String AGENT_NATIVE_ADDRESS = "/restjadegateway/agent/";
	
	/**
	 * Exemples de paramètres de la requête REST pour un agent
	 */
	public static String CONTENT = "content";
	public static String PERFORMATIVE = "performative";
	public static String RECEIVER_NAME = "receiver";
	
	/**
	 * Propriétés de réponse de l'agent local 
	 */
	// Réponse de l'agent
	public static String ANSWER = "answer";
	// L'agent a décelé une erreur de paramètres
	public static String ERROR = "error";
	// L'agent vient d'être créé
	public static String INSTALLED = "installed";
}
