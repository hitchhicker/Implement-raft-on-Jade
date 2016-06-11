package rpc;

public class RequestVoteRPC {
	private int term;
	private String candidateId;
	private int lastLogIndex;
	private int lastLogTerm;
	public int getTerm() {
		return term;
	}
	public void setTerm(int term) {
		this.term = term;
	}
	public String getCandidateId() {
		return candidateId;
	}
	public void setCandidateId(String candidateId) {
		this.candidateId = candidateId;
	}
	public int getLastLogIndex() {
		return lastLogIndex;
	}
	public void setLastLogIndex(int lastLogIndex) {
		this.lastLogIndex = lastLogIndex;
	}
	public int getLastLogTerm() {
		return lastLogTerm;
	}
	public void setLastLogTerm(int lastLogTerm) {
		this.lastLogTerm = lastLogTerm;
	}
	
	/**
	 * Compare which log is more up-to-date
	 * @param anotherIndex : lastLogIndex of another log
	 * @param anotherTerm : anotherTerm of another log
	 * @return true if self's log is more up-to-date
	 */
	public boolean isMoreUpToDate(int anotherIndex, int anotherTerm) {
		// the log with the later term is more up-to-date
		if (anotherTerm > lastLogTerm) 
			return false;
		else if (anotherTerm < lastLogTerm)
			return true;
		else { // when their terms are equal
			// whichever log is longer is more up-to-date
			if (anotherIndex > lastLogIndex)
				return false;
			else
				return true;
		}
	}
}
