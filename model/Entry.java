package model;

public class Entry {
	private int term;
	private String command;
	private String key;
	private String value;
	
	public Entry() {
	}
	
	public Entry(int t, String c, String k, String v) {
		term = t;
		command = c;
		key = k;
		value = v;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	
}
