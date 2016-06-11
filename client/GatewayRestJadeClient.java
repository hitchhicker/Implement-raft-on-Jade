package client;

import util.Constants;
import util.ContentBroker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class GatewayRestJadeClient {
	final String postAddress = Constants.REST_URL
			+ Constants.SIMPLE_POST_ADDRESS;
	final String agentTempAddress = Constants.REST_URL
			+ Constants.AGENT_NATIVE_ADDRESS;
	String login;
	HttpClient client;

	public GatewayRestJadeClient(String login) {
		client = HttpClients.createDefault();
		this.login = login;
	}

	/**
	 * Envoie un simple message post au server adresse = /restjade/simple Toutes
	 * les requêtes HTTP sont formatées avec un seul paramètre main=objet json
	 * construits à partir d'une map de paramètres un paramètre login est ajouté
	 * ex: {"content" : "hello Jade and Rest"}
	 */
	public String simpleMessage(Map<String, Object> params) {
		return formatPostMessage(postAddress, params);
	}

	/**
	 * Envoie un message post au server destiné à un agent avec plusieurs
	 * paramètres Utilise la classe JaeGateway adresse =
	 * .../restjadegateway/agent/
	 * 
	 */

	public String postGatewayAgentMessage(Map<String, Object> params) {
		return formatPostMessage(agentTempAddress, params);
	}

	/**
	 * Formate l'appel au serveur Il n'y a qu'un paramètre dans la requête main
	 * = <objet json construit sur params>
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	private String formatPostMessage(String url, Map<String, Object> params) {
		HttpPost httppost = new HttpPost(url);
		String answer = "";
		ContentBroker cb = new ContentBroker(params);
		StringEntity entity = new StringEntity(cb.toJson(),
				Charset.forName("utf-8"));
		httppost.setEntity(entity);
		HttpResponse res;
		try {
			res = client.execute(httppost);
//			System.out.println(res.getStatusLine().getStatusCode());
			HttpEntity answerEntity = res.getEntity();
			answer = EntityUtils.toString(answerEntity);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return answer;
	}
}
