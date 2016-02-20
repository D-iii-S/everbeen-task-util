package cz.cuni.mff.d3s.been.util.source;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public interface Repository {

	void pull() throws SourceControlException;

	List<Revision> list(Instant from) throws SourceControlException;

	void checkout(Instant instant, Path directory) throws SourceControlException;
	void checkout(Revision revision, Path directory) throws SourceControlException;
}
