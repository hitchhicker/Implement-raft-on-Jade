package model;

public class ToApplyEntry {
	private String command;
	private String key;
	private String value;
	
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
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String displayEntry() {
		if (value != null)
			return "<" + command +" " + key + " " + value + ">";
		else
			return "<" + command + " " + key + ">";
	}
}
