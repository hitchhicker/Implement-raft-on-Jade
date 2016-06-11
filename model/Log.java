package model;

import java.util.ArrayList;

// Different with the raft paper, the index starts from 0
public class Log {
	private ArrayList<Entry> entries;
	public Log() {
		entries = new ArrayList<Entry>();
	}
	
	public int length() {
		return entries.size();
	}
	
	public Entry get(int index) {
		return entries.get(index);
	}
	
	public void append(Entry e) {
		entries.add(e);
	}
	
	public void sliceLog(int start, int end) {
		entries = new ArrayList<Entry>(entries.subList(start, end));
	}
	
	public ArrayList<Entry> getSubLog(int start, int end) {
		ArrayList<Entry> subLog = new ArrayList<Entry>(entries.subList(start, end));
		return subLog;
	}
	
	// display log one by one, for debug
	public void display() {
		if (entries.size() > 0)
			System.out.printf("%-10s%-10s\n", "term", "command");
		for (Entry e : entries) {
			if (e.getValue() != null)
				System.out.printf("%-10s%-10s\n", e.getTerm(), e.getCommand() + " " + e.getKey() + " " + e.getValue());
			else 
				System.out.printf("%-10s%-10s\n", e.getTerm(), e.getCommand() + " " + e.getKey());
//			System.out.println("Term : " + e.getTerm());
//			System.out.println("Key : " + e.getKey());
//			System.out.println("Value : " + e.getValue());
		}
	}
}
