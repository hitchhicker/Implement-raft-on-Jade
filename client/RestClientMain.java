package client;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import util.Constants;

public class RestClientMain {
	public static void userLoop() {
		GatewayRestJadeClient client = new GatewayRestJadeClient("cmd");
		String cmd = readInput();
		while (cmd != null) {
			Map<String,Object> params = new HashMap<String,Object>();
			params.put(Constants.RECEIVER_NAME, "reception");
			params.put(Constants.CONTENT, cmd);
			params.put(Constants.PERFORMATIVE, Constants.REQUEST);
			  
			String answer = client.postGatewayAgentMessage(params);
//			System.out.println(answer);
			cmd = readInput();
		}
	}
	
	public static String readInput() {
		BufferedReader br=null;
		String cmd="";
		try {
			try {
			br = new BufferedReader(new InputStreamReader(System.in));
//			System.out.print("raft>");
			cmd=br.readLine();
			cmd = cmd.toLowerCase();
		} catch(EOFException e) {
				br.close();
			}
		} catch(IOException e) {
			System.out.println("IO Exception");
		}
		return cmd;
	}
//	public static void main(String[] args) {
////		test1();
////		test2();
////		test3();
//		test4();
//	}
  public static void test1() {
	  GatewayRestJadeClient client = new GatewayRestJadeClient("rest");
	  /**
	   * Envoie un message post simple au service rest
	   */
	  Map<String,Object> params1 = new HashMap<String,Object>();
	  params1.put(Constants.CONTENT,"hello Jade and Rest");
	  String answer1 = client.simpleMessage(params1);
	  System.out.println(answer1);
	  
	  /**
	   * Envoie un message post avec plusieurs paramètres au service rest
	   */
	  Map<String,Object> params2 = new HashMap<String,Object>();
	  params2.put(Constants.CONTENT, "hello Rest");
	  params2.put("name", "cm");
	  params2.put("total", 10000);
	  String answer2 = client.simpleMessage(params2);
	  System.out.println(answer2);
  }
  public static void test2() {
	  /**
	   * Envoie un message destiné à l'agent admin
	   * Les paramètres sont corrects
	   * Le récepteur est la classe JadeGateway native
	   */
	  GatewayRestJadeClient client = new GatewayRestJadeClient("cm");
	  Map<String,Object> params = new HashMap<String,Object>();
	  String content = "put a 4";
	  params.put(Constants.RECEIVER_NAME, "reception");
	  params.put(Constants.CONTENT, content);
	  params.put(Constants.PERFORMATIVE, Constants.REQUEST);
	  
	  String answer = client.postGatewayAgentMessage(params);
	  System.out.println(answer);
  }
  public static void test3() {
	  /**
	   * Envoie un message destiné à l'agent admin
	   * Les paramètres sont corrects
	   * Le récepteur est la classe JadeGateway native
	   */
	  GatewayRestJadeClient client = new GatewayRestJadeClient("cm3");
	  Map<String,Object> params = new HashMap<String,Object>();
	  String content = "get a";
	  params.put(Constants.RECEIVER_NAME, "reception");
	  params.put(Constants.CONTENT, content);
	  params.put(Constants.PERFORMATIVE, Constants.REQUEST);
	  
	  String answer = client.postGatewayAgentMessage(params);
	  System.out.println(answer);
  }
  
  public static void test4() {
	  /**
	   * Envoie un message destiné à l'agent admin
	   * Les paramètres sont corrects
	   * Le récepteur est la classe JadeGateway native
	   */
	  GatewayRestJadeClient client = new GatewayRestJadeClient("cm4");
	  Map<String,Object> params = new HashMap<String,Object>();
	  String content = "delete a";
	  params.put(Constants.RECEIVER_NAME, "reception");
	  params.put(Constants.CONTENT, content);
	  params.put(Constants.PERFORMATIVE, Constants.REQUEST);
	  
	  String answer = client.postGatewayAgentMessage(params);
	  System.out.println(answer);
  }
  public static void test5() {
	  /**
	   * Envoie un message destiné à l'agent admin
	   *Manque le paramètre performatif
	   */
	  GatewayRestJadeClient client = new GatewayRestJadeClient("rest");
	  Map<String,Object> params = new HashMap<String,Object>();
	  params.put(Constants.CONTENT, "Error Rest :(");
	  params.put(Constants.RECEIVER_NAME, "admin");
	  //params.put(Constants.PERFORMATIVE, Constants.REQUEST);
	  String answer = client.postGatewayAgentMessage(params);
	  System.out.println(answer);
  }
}
