package rpc;

import java.util.ArrayList;

import model.Entry;

public class AppendEntriesRPC {
	private int term;
	private String leaderId;
	private int prevLogIndex;
	private int prevLogTerm;
	private ArrayList<Entry> entries;
	private int leaderCommit;
	
	public int getTerm() {
		return term;
	}
	public void setTerm(int term) {
		this.term = term;
	}
	public String getLeaderId() {
		return leaderId;
	}
	public void setLeaderId(String leaderId) {
		this.leaderId = leaderId;
	}
	public int getPrevLogIndex() {
		return prevLogIndex;
	}
	public void setPrevLogIndex(int prevLogIndex) {
		this.prevLogIndex = prevLogIndex;
	}
	public int getPrevLogTerm() {
		return prevLogTerm;
	}
	public void setPrevLogTerm(int prevLogTerm) {
		this.prevLogTerm = prevLogTerm;
	}
	public ArrayList<Entry> getEntries() {
		return entries;
	}
	public void setEntries(ArrayList<Entry> entries) {
		this.entries = entries;
	}
	public int getLeaderCommit() {
		return leaderCommit;
	}
	public void setLeaderCommit(int leaderCommit) {
		this.leaderCommit = leaderCommit;
	}
}
