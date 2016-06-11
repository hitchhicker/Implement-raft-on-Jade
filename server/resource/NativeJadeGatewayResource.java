package server.resource;

import util.Constants;
import util.ContentBroker;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.wrapper.gateway.JadeGateway;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

public class NativeJadeGatewayResource extends SimplePostResource {

	@Post
	public void update(Representation entity) {
		/**
		 * On suppose que les paramètres destinés à créer un message pour un agent jade
		 * ont été envoyés sous la forme d'un objet json de la façon suivante
		 * {"receiver":"admin", "performatif":"request","content":{"action" : "look", "login":"admin","pwd":"admin"}}
		 * Il pourraient y avoir d'autre paramètres
		 * Le content broker parse cette chaîne json 
		 */
		String sd = "";
		try {
			sd = URLDecoder.decode(entity.getText(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println("Rest server entity: " + sd);
		ContentBroker cb = new ContentBroker(sd);
		/**
		 * utilise l'agent local JadeGateway, qui va envoyer un message à l'agent destinataire
		 * en utilisant les paramètres receiver, performatif, content et retourne la réponse au client.
		 * 
		 */
		
		activeAgent(cb.getMap());
	}

	private void activeAgent(Map<String, Object> params) {
		/**
		 * Initialise le gateway
		 * Le main container est sur la même station que le server rest
		 * On utilise l'agent gateway par défaut
		 */
		JadeGateway.init(null, null);
		try {
			/**
			 * On crée le behaviour que l'agent Gateway doit exécuter
			 */
			ProcessBehaviour behaviour = new ProcessBehaviour(params);
			/**
			 * On demande à l'agent Gateway d'exécuter ce behaviour
			 * Il doit s'arrêter rapidement
			 * Il met la réponse dans des variables locales du behaviour
			 */
			JadeGateway.execute(behaviour);
			/**
			 * On formate la réponse qui sera retournée au client rest qui a fait la requête
			 * La réponse utilise un status (ok ou non) et un contenu.
			 */
			formatAnswer(behaviour.getAnswer(),behaviour.getStatus());
		} catch (StaleProxyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ControllerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class ProcessBehaviour extends Behaviour {
		Map<String, Object> params;
		private boolean stop = false;
		int step = 0;
		String convId;
		Status status;
		String answer;

		public ProcessBehaviour(Map<String, Object> params) {
			super();
			this.params = params;
			convId = String.valueOf(System.currentTimeMillis());
		}

		public Status getStatus() {
			return status;
		}

		public String getAnswer() {
			return answer;
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				String perfs = (String) params.get(Constants.PERFORMATIVE);
				String agent_id = (String) params.get(Constants.RECEIVER_NAME);
				if (perfs != null && agent_id != null) {
					int perf = Constants.PERFORMATIVES.get(perfs);
					AID aid = new AID(agent_id,AID.ISLOCALNAME);
					ACLMessage message = new ACLMessage(perf);
					message.addReceiver(aid);
					message.setContent((String) params.get(Constants.CONTENT));
					message.setConversationId(convId);
					myAgent.send(message);
//					System.out.println("ProcessBehaviour --> Message sent to: " + agent_id);
					step = 1;
				} else {
					stopProcess(Status.CLIENT_ERROR_BAD_REQUEST, "No performative or no aid");
				}
				break;
			case 1:
				MessageTemplate mt = MessageTemplate.and(
						MessageTemplate.MatchPerformative(ACLMessage.INFORM),
						MessageTemplate.MatchConversationId(convId));
				ACLMessage answer = myAgent.receive(mt);
				if (answer != null) {
					stopProcess(Status.SUCCESS_OK,
							convId + " - " + answer.getContent());
				} else
					block();
				break;
			}
		}

		private void stopProcess(Status st, String ans) {
			status = st;
			answer = ans;
			stop = true;
		}

		@Override
		public boolean done() {
			return stop;
		}
	}
}
