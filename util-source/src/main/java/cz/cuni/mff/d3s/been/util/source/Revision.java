package cz.cuni.mff.d3s.been.util.source;

import java.time.Instant;

public class Revision implements Comparable<Revision> {

	private Instant date;

	protected Revision(Instant argDate) {
		date = argDate;
	}

	public Instant getDate() {
		return (date);
	}

	@Override
	public int compareTo(Revision o) {
		return date.compareTo(o.getDate());
	};
}
