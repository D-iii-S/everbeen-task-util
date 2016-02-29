package cz.cuni.mff.d3s.been.util.source.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.cuni.mff.d3s.been.util.source.Repository;
import cz.cuni.mff.d3s.been.util.source.Revision;
import cz.cuni.mff.d3s.been.util.source.SourceControlException;

/**
 * @author ceres
 *
 */
public class GitRepository implements Repository {

	protected String remoteAddress;
	protected Path localAbsolutePath;

	private static final Logger log = LoggerFactory.getLogger(GitRepository.class);
	
	public GitRepository(String argRemoteAddress, Path argLocalPath) throws SourceControlException {
		
		remoteAddress = argRemoteAddress;
		
		// Local path is kept as absolute because current directory may change throughout execution.
		localAbsolutePath = argLocalPath.toAbsolutePath();
		
		// If the local path does not exist initialize a repository mirror inside.
		if (!gitExists()) gitInitialize();
	}

	/*****************************************************************************/
	
	@Override
	public int hashCode() {
		// Assuming the remote path is reasonably unique,
		// we can use it as a decent hash code here too.
		return remoteAddress.hashCode ();
	}

	@Override
	public boolean equals(Object obj) {
		// Some easy decisions.
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		// Really comparing two repositories.
		// Equality means same remote path.
		GitRepository other = (GitRepository) obj;
		return remoteAddress.equals(other.remoteAddress);
	}
	
	/*****************************************************************************/
	
	/**
	 * Checks whether a local repository mirror exists.
	 * 
	 * @return True when the repository exists.
	 */
	private boolean gitExists () {
		Path repoHead = localAbsolutePath.resolve("HEAD");
		Path repoRefs = localAbsolutePath.resolve("refs");
		Path repoObjects = localAbsolutePath.resolve("objects");
		boolean exists = Files.isReadable(repoHead) && Files.isDirectory(repoRefs) && Files.isDirectory(repoObjects);
		return exists;
	}
	
	/**
	 * Initializes a local repository mirror.
	 * 
	 * @throws SourceControlException
	 */
	private void gitInitialize () throws SourceControlException {
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(localAbsolutePath.toFile());
		
		CommandLine command = new CommandLine("git")
			.addArgument("clone", false)
			.addArgument("--mirror", false)
			.addArgument(remoteAddress, false)
			.addArgument(localAbsolutePath.toString(), false);

		try {
			int result = executor.execute(command);
			if (result != 0) throw new SourceControlException("Failed to initialize the repository, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to initialize the repository.", e); 
		}
	}

	/**
	 * Lists revisions from repository.
	 * 
	 * @param since Oldest date or null for no limit.
	 * @param until Newest date or null for no limit.
	 * @param limit Revision count limit or null for no limit.
	 * @return List of revisions.
	 * @throws SourceControlException
	 */
	public List<Revision> gitLog(String since, String until, String limit) throws SourceControlException {
		final List<Revision> list = new LinkedList<Revision>();
		
		class LogParser extends LogOutputStream {
			@Override
			protected void processLine(String line, int level) {
				String [] record = line.split ("\\t");
				try {
					String hash = record [0];
					Instant date = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(record [1]));
					list.add(new GitRevision(GitRepository.this, hash, date));
				}
				catch (ArrayIndexOutOfBoundsException | DateTimeParseException e) {
					log.info("Failed to parse log entry: {}.", line);
				}
			}
		}
		
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(localAbsolutePath.toFile());
		
		PumpStreamHandler pumper = new PumpStreamHandler(new LogParser());
		executor.setStreamHandler(pumper);
		
		CommandLine command = new CommandLine("git")
			.addArgument("log", false)
			.addArgument("HEAD", false)
			.addArgument("--format=%H\t%cI", false); 
		
		if (since != null) command.addArgument("--since" + "=" + since, false);
		if (until != null) command.addArgument("--until" + "=" + until, false);
		if (limit != null) command.addArgument("--max-count" + "=" + limit, false);
		
		try {
			int result = executor.execute(command);			
			if (result != 0) throw new SourceControlException("Failed to query the repository log, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to query the repository log.", e); 
		}
		
		return list;
	}
	
	/**
	 * Checks out revision from repository.
	 * <p>
	 * There seems to be no simple way of getting a working directory for a commit without getting a repository history too.
	 * We therefore create a shared repository clone and check out the particular commit. 
	 * 
	 * @param revision Revision to export.
	 * @param exportPath Directory to export to.
	 * @throws SourceControlException
	 */
	private void gitClone(String revision, Path exportPath) throws SourceControlException {
		
		DefaultExecutor executor = new DefaultExecutor();
		
		CommandLine cloneCommand = new CommandLine("git")
			.addArgument("clone", false)
			.addArgument("--shared", false)
			.addArgument("--no-checkout", false)
			.addArgument(localAbsolutePath.toString(), false)
			.addArgument(exportPath.toAbsolutePath().toString(), false);
		
		CommandLine checkoutCommand = new CommandLine("git")
				.addArgument("checkout", false)
				.addArgument(revision, false);
			
		try {
			executor.setWorkingDirectory(localAbsolutePath.toFile());
			int cloneResult = executor.execute(cloneCommand);
			if (cloneResult != 0) throw new SourceControlException("Failed to clone the repository, exit code " + cloneResult + ".");
			
			executor.setWorkingDirectory(exportPath.toFile());
			int checkoutResult = executor.execute(checkoutCommand);
			if (checkoutResult != 0) throw new SourceControlException("Failed to checkout the repository, exit code " + checkoutResult + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to clone or checkout the repository.", e); 
		}
	}
	
	/*****************************************************************************/
	
	@Override
	public void pull() throws SourceControlException {
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(localAbsolutePath.toFile());
		
		CommandLine command = new CommandLine("git")
			.addArgument("fetch", false);
		
		try {
			int result = executor.execute(command);			
			if (result != 0) throw new SourceControlException("Failed to pull the repository, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to pull the repository.", e); 
		}
	}

	@Override
	public List<Revision> list(Instant from) throws SourceControlException {
		return gitLog(from.toString(), null, null);
	}

	@Override
	public void checkout(Instant instant, Path directory) throws SourceControlException {
		List<Revision> candidateRevisions = gitLog(null, instant.toString(), "1");
		if (candidateRevisions.isEmpty()) throw new SourceControlException("Attempting to checkout a revision that does not exist.");
		GitRevision nativeRevision = (GitRevision) candidateRevisions.get(0);
		String hashRevision = nativeRevision.hash;
		gitClone(hashRevision, directory);
	}
		
	@Override
	public void checkout(Revision revision, Path directory) throws SourceControlException {
		GitRevision nativeRevision = (GitRevision) revision;
		if (nativeRevision == null) throw new SourceControlException ("Attempting to checkout revision from a different version control system.");
		if (!nativeRevision.repository.equals(this)) throw new SourceControlException ("Attempting to checkout revision from a different repository.");
		String hashRevision = nativeRevision.hash;
		gitClone(hashRevision, directory);
	}
}
