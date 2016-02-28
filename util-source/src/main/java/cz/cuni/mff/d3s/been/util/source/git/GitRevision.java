package cz.cuni.mff.d3s.been.util.source.git;

import java.time.Instant;

import cz.cuni.mff.d3s.been.util.source.Revision;

class GitRevision extends Revision {

	protected GitRepository repository;
	protected String hash;
	
	GitRevision (GitRepository argRepository, String argHash, Instant argDate) {
		super (argDate);
		repository = argRepository;
		hash = argHash;
	}
	
	@Override
	public int hashCode() {
		// Assuming the revision hash is reasonably unique,
		// we can use it as a decent hash code here too.
		return hash.hashCode ();
	}

	@Override
	public boolean equals(Object obj) {
		// Some easy decisions.
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		// Really comparing two revisions.
		// Equality means same repository and same hash,
		// even though same hash alone would probably suffice.
		GitRevision other = (GitRevision) obj;
		return hash.equals(other.hash) && repository.equals (other.repository);
	}
}
