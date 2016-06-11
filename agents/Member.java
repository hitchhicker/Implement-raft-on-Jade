package agents;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.ToApplyEntry;
import model.Entry;
import model.Log;
import rpc.AppendEntriesRPC;
import rpc.RequestVoteRPC;
import rpc.Result;

public class Member extends Agent{
	private int currentTerm = 1;
	private String votedFor = null; // every time the state turns to follower, it should be rested to null 
	private Log log;
	private int commitIndex = -1;
	private int lastApplied = -1;
	private int[] nextIndex;
	private int[] matchIndex;
	private int lastLogIndex = -1;
	private int lastLogTerm = 0;
	final static int SERVER__INIT_NUM = 5;
	public enum State {
		FOLLOWER, CANDIDATE, LEADER
	}
	private State state;
	
	private int electionTimeout;
	private int votes = 0;

	final static int TIMEOUT_MIN = 3000;
	final static int TIMEOUT_MAX = 6000;
	
	private Behaviour electionTimeoutBev; // all servers except leader have this behavior
	private Behaviour heartBeatBev = null; // only leader has this behavior
	protected void setup() {
		// register in the agent DF
		registerToDF();
		// initialize empty log
		log = new Log();
		
		Object[] args = getArguments();
		
		nextIndex = new int[(int)args[0]];
		matchIndex = new int[(int)args[0]];
		
		for(int i = 0; i < 5; i++) {
			nextIndex[i] = 0;
			matchIndex[i] = -1;
		}
		// initialize state as follower
		state = State.FOLLOWER;
		// set election timeout
		electionTimeout = getRandomTimeout();

		electionTimeoutBev = new OnElectionTimeout(this, electionTimeout);
		
		// launch election timeout behavior, the server will become candidate as soon timeout happens
		addBehaviour(electionTimeoutBev);
		
		// this behavior listens vote request from candidate 
		addBehaviour(new OnRequestVote());
		// this behavior listens append entry request from leader
		addBehaviour(new OnAppendEntries());
		
		addBehaviour(new OnResultVote());
		addBehaviour(new OnResultAppendEntries());
		addBehaviour(new OnRequestFromReception());
		addBehaviour(new ShowNode());
		addBehaviour(new ShowLog());
	}
	
	/**
	 * Server receives a ResultVote
	 * Performative : INFORM
	 * ConversationId : 'RV'
	 */
	private class OnResultVote extends CyclicBehaviour {
		public void action() {
			
		    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
		    		MessageTemplate.MatchConversationId("RV"));
			ACLMessage message = receive(mt);
			// follower ignores any such message, cause it has no sense
			if (message != null && state != State.FOLLOWER) {
				ObjectMapper mapper  = new ObjectMapper();
				try{
					Result voteResult = mapper.readValue(message.getContent(), Result.class);
					if (voteResult.getSuccess()) {
						// get a vote
						votes++;
						/* 
						 * when votes received from majority of servers
						 * become leader if he is not leader  
						 */
						if (votes > (getServerQuantity() - 1) / 2 && state != State.LEADER) {
							becomeLeader();
							// send heart beat to other servers regularly
							heartBeatBev = new RegularHeartBeat(myAgent, 2000);
							addBehaviour(heartBeatBev);
						}
					} else {
						needUpdateTerm(voteResult.getTerm());
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			} else
				block();
		}
	}
	
	/**
	 * Server receives a ResultAppendEntries or Response of Heart Beat
	 * Performative : INFORME
	 * ConversationId: 'AE' or 'HB'
	 */
	private class OnResultAppendEntries extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			MessageTemplate mt2 = MessageTemplate.or(MessageTemplate.MatchConversationId("AE"), 
					MessageTemplate.MatchConversationId("HB"));
			MessageTemplate mt = MessageTemplate.and(mt1, mt2);
			ACLMessage message = receive(mt1);
			// TODO : add something ?
			// follower ignores any such message
			if (message != null) {
				ObjectMapper mapper  = new ObjectMapper();
				try{
					Result appendEntriesResult = mapper.readValue(message.getContent(), Result.class);
					// if leader is still valid with his current term, continue
					if (!needUpdateTerm(appendEntriesResult.getTerm()) && state == State.LEADER) {
						if (message.getConversationId().equals("AE")) {
							int serverNumber = getServerNumberByAid(message.getSender());
							if (appendEntriesResult.getSuccess()) {
								// if successful, update nextIndex and matchIndex for this follower
								nextIndex[serverNumber]++;
								matchIndex[serverNumber]++;
								/*
								 * if there exists an N such that N > commitIndex, a majority of matchIndex[i] ≥ N
								 * and log[N].term == currentTerm: set commitIndex = N
								 */
								int n = commitIndex;
								int server_quantity = getServerQuantity();
								int count;
								while(true) {
									count = 0;
									for(int i = 0; i < server_quantity; i++) {
										if (matchIndex[i] >= n)
											count++;
									}	
									if (count > ((server_quantity - 1) / 2))
										n++;
									else
										break;
								}
								if ((n - 1) > commitIndex) {
									commitIndex = n - 1;
								}
								
								// apply entries commited
								while(lastApplied < commitIndex) {
									Entry entry = log.get(lastApplied + 1);
									applyByIndex(lastApplied + 1);
									addBehaviour(new InformReception(entry.getCommand(), entry.getKey(), entry.getCommand()));
									lastApplied++;
								}
							} else {
								// if not successful, decrement nextIndex and retry
								nextIndex[serverNumber]--;
								addBehaviour(new SendAppendEntries(
										log.getSubLog(nextIndex[serverNumber], log.length()),
										message.getSender()));
							}
						} else {
							 // if it's a heart beat, do nothing
						}
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			} else
				block();
		}
	}
	/**
	 * Server receives a RequestVote
	 * Performative : REQUEST
	 * ConversationId : 'RV'
	 */
	private class OnRequestVote extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
		    		MessageTemplate.MatchConversationId("RV"));
			ACLMessage message = receive(mt);
			if (message != null) {
				ObjectMapper mapper  = new ObjectMapper();
				boolean isConvertedToFollower = false;
				try{
					RequestVoteRPC msg = mapper.readValue(message.getContent(), RequestVoteRPC.class);
					boolean isSuccess;
					if (msg.getTerm() < currentTerm) {
						isSuccess = false;
					} else {
						isConvertedToFollower = needUpdateTerm(msg.getTerm());
						if ((votedFor == null || votedFor.equals(msg.getCandidateId()))
								&& msg.isMoreUpToDate(lastLogIndex, lastLogTerm)) {
								isSuccess = true;
								votedFor = msg.getCandidateId();
						} else 
								isSuccess = false;
					}
					addBehaviour(new SendResult(currentTerm, isSuccess, message.getSender(), "RV"));
				}catch(Exception ex){
					ex.printStackTrace();
				}
				if (state != State.LEADER && !isConvertedToFollower) {
					/*
					 * reset the timeout when he is not leader and he has
					 * not converted to Follower in the function needUpdateTerm
					 */
					nextTimeOut();
				}
			} else
				block();
		}
	}
	/**
	 * Server receives a AppendEntries
	 * Performative : REQUEST
	 * ConversationId : 'AE' or 'HB'
	 */
	private class OnAppendEntries extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
					MessageTemplate.or(MessageTemplate.MatchConversationId("AE"),
							MessageTemplate.MatchConversationId("HB")));
			ACLMessage message = receive(mt);
			if (message != null) {
				AID sender = message.getSender();
				ObjectMapper mapper  = new ObjectMapper();
				String convId = message.getConversationId();
				try{
					AppendEntriesRPC msg = mapper.readValue(message.getContent(), AppendEntriesRPC.class);
					if (!needUpdateTerm(msg.getTerm())) {
						// if AppendEntries RPC received from new leader: convert to follower
						if (state == State.CANDIDATE)
							becomeFollower();
						else if (state == State.FOLLOWER)
							nextTimeOut();
					}
					if (msg.getTerm() < currentTerm) {
						addBehaviour(new SendResult(currentTerm, false, sender, convId));
					} else {
						//check if leader is too far ahead in the log
						if (msg.getLeaderCommit() > commitIndex)
							// if so
							commitIndex = Math.min(msg.getLeaderCommit(), log.length() - 1);
							while (lastApplied < commitIndex) {
								applyByIndex(lastApplied + 1);
								lastApplied++;
							}
							
						// check if log is smaller than the preLogIndex
						if (log.length() - 1 < msg.getPrevLogIndex())
							// if so
							addBehaviour(new SendResult(currentTerm, false, sender, convId));
						
						// Reply false if log doesn’t contain an entry at prevLogIndex
						// whose term matches prevLogTerm
						else if (log.length() > 0 &&
								log.get(msg.getPrevLogIndex()).getTerm() != msg.getPrevLogTerm()){
							// delete the conflict entry and all that follow it
							log.sliceLog(0, msg.getPrevLogIndex() + 1);
							lastLogIndex = msg.getPrevLogIndex();
							lastLogTerm = msg.getPrevLogTerm();
							addBehaviour(new SendResult(currentTerm, false, sender, convId));
						}
						else if (log.length() > 0 &&
								msg.getLeaderCommit() > -1 &&
								log.get(msg.getLeaderCommit()).getTerm() != msg.getTerm()) {
							log.sliceLog(0, commitIndex + 1);
							if (msg.getEntries() != null) {
								for (Entry newEntry : msg.getEntries())
									log.append(newEntry);
							}
							lastLogIndex = log.length() - 1;
							lastLogTerm = log.get(lastLogIndex).getTerm();
							addBehaviour(new SendResult(currentTerm, true, sender, convId));
						}
						else {
							if (msg.getEntries() != null) {
								for (Entry newEntry : msg.getEntries())
									log.append(newEntry);
								lastLogIndex = log.length() - 1;
								lastLogTerm = log.get(lastLogIndex).getTerm();
							}
							addBehaviour(new SendResult(currentTerm, true, sender, convId));
						}
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			} else 
				block();
		}
	}
	// 
	/**
	 * Reply to RPCs with term and success
	 * Performative : INFORM
	 * ConversatinId : defined by parameter msgType
	 */
	private class SendResult extends OneShotBehaviour {
		private int term;
		private boolean isSuccess;
		private AID receiver;
		private String msgType; 
		
		public SendResult(int term, boolean isSuccess, AID receiver, String msgType) {
			this.term = term;
			this.isSuccess = isSuccess;
			this.receiver = receiver;
			this.msgType = msgType;
		}
		
		public void action() {
			// send RequestVote RPCs to all the other agents
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);
			message.addReceiver(receiver);
			message.setConversationId(msgType);
			
			Result res = new Result();
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();
			res.setTerm(term);
			res.setSuccess(isSuccess);
  			try {
  				mapper.writeValue(sw, res);
  				String s = sw.toString();
  				message.setContent(s);
  				send(message);
  			} catch(Exception ex) {
  				ex.printStackTrace();
  			}
		}
	}
	
	/**
	 * Send AppendEntries(Heart Beat if entry is null)
	 * Performative : REQUEST
	 * ConversationId : 'AE' or 'HB'
	 */
	// append entry or heart beat when entry is null
	private class SendAppendEntries extends OneShotBehaviour {
		private ArrayList<Entry> entries;
		private AID receiver;
		
		public SendAppendEntries(ArrayList<Entry> entries, AID receiver) {
			this.entries = entries;
			this.receiver = receiver;
		}
		
		public void action() {
			// send appendEntries RPCs to all the other agents
			ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
			if (entries != null)
				message.setConversationId("AE");
			else 
				message.setConversationId("HB");
			
			AppendEntriesRPC apdEntry = new AppendEntriesRPC();
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();

			int prevLogIndex = Math.max(-1, nextIndex[getServerNumberByAid(receiver)] - 1);
			int prevLogTerm;
			if (prevLogIndex == -1)
				prevLogTerm = 1;
			else {
				prevLogTerm = log.get(prevLogIndex).getTerm();
			}
			apdEntry.setTerm(currentTerm);
			apdEntry.setLeaderId(getName());
			apdEntry.setPrevLogIndex(prevLogIndex);
			apdEntry.setPrevLogTerm(prevLogTerm);
			apdEntry.setEntries(entries);
			apdEntry.setLeaderCommit(commitIndex);
			try {
  				mapper.writeValue(sw, apdEntry);
  				String s = sw.toString();
  				message.addReceiver(receiver);
  				message.setContent(s);
  				send(message);
  			}catch(Exception ex) {
  				ex.printStackTrace();
  			}
		}
	}
	/**
	 * Add Behaviour SendAppendEntries regularly
	 */
	private class RegularHeartBeat extends TickerBehaviour {
		
		public RegularHeartBeat(Agent a, long period) {
			super(a, period);
		}

		public void onTick() {
			for(int i = 0; i < getServerQuantity(); i++) {
				// send other server except itself
				if (i != getServerNumberByAid(getAID())) {
					if (lastLogIndex >= nextIndex[i]) {
						ArrayList<Entry> entries = log.getSubLog(nextIndex[i], lastLogIndex + 1);
						addBehaviour(new SendAppendEntries(entries, new AID(i+"", AID.ISLOCALNAME)));
					} else
						addBehaviour(new SendAppendEntries(null, new AID(i+"", AID.ISLOCALNAME)));
				}
			}
		}
	}
	
	/**
	 * Start a new election as a candidate, send RequestsVote RPC to other servers
	 * Performative : REQUEST
	 * ConversationId : 'RV'
	 */
	private class SendRequestVote extends OneShotBehaviour {
		
		public void action() {
			// become candidate
			becomeCandidate();
			// send RequestVote RPCs to all the other agents
			ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
			message.setConversationId("RV");
			// get other members using DF agent
			AID[] members = getOtherMembers();
			for (AID member : members) {
				message.addReceiver(member);	
			}
			RequestVoteRPC reqVote = new RequestVoteRPC();
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();
			
			reqVote.setTerm(currentTerm);
			reqVote.setCandidateId(getName());
			reqVote.setLastLogIndex(lastLogIndex);
			reqVote.setLastLogTerm(lastLogTerm);
  			try {
  				mapper.writeValue(sw, reqVote);
  				String s = sw.toString();
  				message.setContent(s);
  				send(message);
  			}catch(Exception ex) {
  				ex.printStackTrace();
  			}
		}
	}
	
	/**
	 * Event when timeout happens
	 */
	private class OnElectionTimeout extends WakerBehaviour {
		
		public OnElectionTimeout(Agent a, int timeout) {
			super(a, timeout);
		}
		public void onWake(){
			getAgent().addBehaviour(new SendRequestVote());
		}
	}
	
	/**
	 * Leader recuives a User Request from reception  
	 * Performative : REQUEST
	 * ConversationId : 'RU'
	 */
	private class OnRequestFromReception extends CyclicBehaviour {
		// behavior is only valid when it's a leader 
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
		    		MessageTemplate.MatchConversationId("RU")); // RU : Request User
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
				}catch(Exception ex){
					ex.printStackTrace();
				}
				// append entry to local log
				Entry e = new Entry(currentTerm, command, key, value);
				log.append(e);
				lastLogIndex++;
				lastLogTerm = currentTerm;
			} else
				block();
		}
	}
	
	/**
	 * Subscribe as leader so that the reception can know who is leader
	 * Performative : SUBSCRIBE
	 * ConversationId : None
	 */
	//  
	private class subscribeAsleader extends OneShotBehaviour {
		
		public void action() {
			ACLMessage message = new ACLMessage(ACLMessage.SUBSCRIBE);
			message.addReceiver(new AID("Reception", AID.ISLOCALNAME));
			message.setContent(myAgent.getName());
			send(message);
		}
	}
	
	/**
	 * Send message to State Machine to ask for applying the entry
	 * Performative : REQUEST
	 * ConversationId : None
	 */
	private class ApplyEntryBev extends OneShotBehaviour {
		private String command;
		private String key;
		private String value;
		
		public ApplyEntryBev(String command, String key, String value) {
			this.command = command;
			this.key = key;
			this.value = value;
		}
		
		public void action() {
			ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
			ToApplyEntry entry = new ToApplyEntry();
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();
			
			entry.setCommand(command);
			entry.setKey(key);
			entry.setValue(value);
  			try {
  				mapper.writeValue(sw, entry);
  				String s = sw.toString();
  				message.addReceiver(new AID("StateMachine" + getServerNumberByAid(getAID()), AID.ISLOCALNAME));
  				message.setContent(s);
  				send(message);
  			}catch(Exception ex) {
  				ex.printStackTrace();
  			}
		}
	}
	
	/**
	 * Inform reception that an entry is applied
	 * Performative : INFORM
	 * ConversationId : None
	 */
	private class InformReception extends OneShotBehaviour {
		private String command;
		private String key;
		private String value;
		
		public InformReception(String command, String key, String value) {
			this.command = command;
			this.key = key;
			this.value = value;
		}
		public void action() {
			ACLMessage message = new ACLMessage(ACLMessage.INFORM);

			ToApplyEntry entry = new ToApplyEntry();
			ObjectMapper mapper = new ObjectMapper();
			StringWriter sw = new StringWriter();
			
			entry.setCommand(command);
			entry.setKey(key);
			entry.setValue(value);
  			try {
  				mapper.writeValue(sw, entry);
  				String s = sw.toString();
  				message.addReceiver(new AID("Reception", AID.ISLOCALNAME));
  				message.setContent(s);
  				send(message);
  			}catch(Exception ex) {
  				ex.printStackTrace();
  			}
		}
	}
	
	/**
	 * Display all nodes infomation  
	 * Only for demonstration
	 * Performative : REQUEST
	 * ConversationId : 'shownode'
	 */
	private class ShowNode extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
		    		MessageTemplate.MatchConversationId("shownode")); // RU : Request User
			ACLMessage message = receive(mt);
			if (message != null) {
				ShowNodeInfos();
				if (state == State.LEADER) {
					// leader will propagate this message to other servers
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.setConversationId("shownode");
					for (AID aid : getOtherMembers()) 
						msg.addReceiver(aid);
					send(msg);	
				}
			} else
				block();
		}
	}
	
	private class ShowLog extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
		    		MessageTemplate.MatchConversationId("showlog")); // RU : Request User
			ACLMessage message = receive(mt);
			if (message != null) {
				
				if (state == State.LEADER) {
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.setConversationId("showlog");
					ShowLogInfos();
					for (AID aid : getOtherMembers()) 
						msg.addReceiver(aid);
					send(msg);	
				} else {
					int i = getServerNumberByAid(getAID());
					addBehaviour(new WakerBehaviour(getAgent(), (i+1)*100){
						public void onWake() {
							ShowLogInfos();
						}
					});
				}
			} else
				block();
		}
	}
	
	/**
	 * Register as a member on agent DF
	 */
	public void registerToDF() {
		DFAgentDescription dfad = new DFAgentDescription();
		dfad.setName(getAID());
	    ServiceDescription sd = new ServiceDescription();
	    sd.setType("raft");
	    sd.setName("member");
	    dfad.addServices(sd);
	    try {
	    	DFService.register(this, dfad);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	/**
	 * Get other servers's AID
	 * @return AID[]
	 */
	private AID[] getOtherMembers() {
		AID[] aids = null;
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("raft");
		sd.setName("member");
		template.addServices(sd);
		try {	
			DFAgentDescription[] result =DFService.search(this, template);
			if (result != null)
				aids = new AID[result.length];
			int i=0;
			for (DFAgentDescription res : result) {
				// add all except itself
				if (!res.getName().equals(getName()))
					aids[i] = res.getName();
				i++;
			}
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
		return aids; 
	}
	
	/**
	 * Generate random timeout number
	 * @return int
	 */
	public int getRandomTimeout() {
		Random rand = new Random();
		int randomNum = rand.nextInt((TIMEOUT_MAX - TIMEOUT_MIN) + 1) + TIMEOUT_MIN;
		return randomNum;
	}
	
	public void resetElectionTimeout() {
		setElectionTimeout(getRandomTimeout());
	}
	
	public void setElectionTimeout(int electionTimeout) {
		this.electionTimeout = electionTimeout;
	}
	
	/**
	 * Start next timeout behaviour
	 */
	public void nextTimeOut() {
		if (electionTimeoutBev != null)
			removeBehaviour(electionTimeoutBev);
		
		resetElectionTimeout();
		electionTimeoutBev = new OnElectionTimeout(Member.this, electionTimeout);
		addBehaviour(electionTimeoutBev);
	}
	
	/**
	 * A leader or a candidate can become a follower
	 */
	public void becomeFollower() {
		if (state != State.FOLLOWER)
			System.out.println("[DEBUG]Agent " + getName() + " becomes follower.");
		state = State.FOLLOWER;
		// reset votes to zero
		votes = 0;
		votedFor = null;
		if (heartBeatBev != null)
			removeBehaviour(heartBeatBev);
		nextTimeOut();
	}
	
	/**
	 * A candidate can become a leader
	 */
	public void becomeLeader() {
		System.out.println("[DEBUG]Agent " + getName() + " becomes leader.");
		// change state to leader
		state = State.LEADER;
		// remove election timeout
		removeBehaviour(electionTimeoutBev);
		
		// subscribe as new leader
		addBehaviour(new subscribeAsleader());
		
		electionTimeoutBev = null;
		// initialize nextIndex and matchIndex
		for(int i = 0; i < getServerQuantity(); i++) {
			nextIndex[i] = lastLogIndex + 1;
			matchIndex[i] = -1;
		}
	}
	
	/**
	 * A follower can become a candidate
	 */
	public void becomeCandidate() {
		// increment its current term
		currentTerm++;
		// state to candidate
		state = State.CANDIDATE;
		// vote for itself
		votedFor = getName();
		//increment votes (the vote by itself)
		votes++;
		// reset election timer
		nextTimeOut();
		// debug
		System.out.println("[DEBUG]Agent " + getName() + " becomes candidate.");
	}
	
	/**
	 * Use it when the majority problem is considered
	 * @return quantity of server
	 */
	public int getServerQuantity() {
		int res = 0;
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("raft");
		sd.setName("member");
		template.addServices(sd);
		try {	
			DFAgentDescription[] result = DFService.search(this, template);
			if (result != null)
				res = result.length;
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
		return res;
	}
	
	/**
	 * Retrieve server number from agent name, for example : 1@raft -> 1
	 * @param aid : agent Aid value
	 * @return int
	 */
	public int getServerNumberByAid(AID aid) {
		return Integer.parseInt(aid.getName().split("@")[0]);
	}
	
	/**
	 * If RPC request or response contains term T > currentTerm,
	 * set currentTerm = T, convert to follower, return true
	 * @param term : term in the message
	 * @return boolean
	 */
	public boolean needUpdateTerm(int term) {
		if (term > currentTerm) {
			currentTerm = term;
			becomeFollower();
			return true;
		} else 
			return false;
	}
	/**
	 * Apply the entry by the index of log
	 * @param index : index of log
	 */
	public void applyByIndex(int index) {
		Entry entry = log.get(index);
		addBehaviour(new ApplyEntryBev(entry.getCommand(), entry.getKey(), entry.getValue()));
	}

	/**
	 * Display nodes infos
	 * Only for demonstration
	 */
	// function for the demonstration
	public void ShowNodeInfos() {
		System.out.printf("%-20s%-20s%-20s%-20s%-20s%-20s%-20s\n", getAID().getName(), state, currentTerm, lastLogIndex, 
				lastLogTerm, commitIndex,lastApplied);
	}
	
	/**
	 * Display log infos
	 * Only for demontration
	 */
	public void ShowLogInfos() {
		System.out.println("-------" + getAID().getLocalName() + "-------" + "(" + state + ")");
		log.display();
	}
}
