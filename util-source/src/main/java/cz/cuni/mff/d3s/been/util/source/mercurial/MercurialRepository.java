package cz.cuni.mff.d3s.been.util.source.mercurial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
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
public class MercurialRepository implements Repository {

	protected String remoteAddress;
	protected Path localAbsolutePath;

	private static final Logger log = LoggerFactory.getLogger(MercurialRepository.class);
	
	private DateTimeFormatter stampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx");
	
	public MercurialRepository(String argRemoteAddress, Path argLocalPath) throws SourceControlException {
		
		remoteAddress = argRemoteAddress;
		
		// Local path is kept as absolute because current directory may change throughout execution.
		localAbsolutePath = argLocalPath.toAbsolutePath();
		
		// If the local path does not exist initialize an empty repository inside.
		if (!hgExists()) hgInitialize();
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
		MercurialRepository other = (MercurialRepository) obj;
		return remoteAddress.equals(other.remoteAddress);
	}
	
	/*****************************************************************************/
	
	/**
	 * Checks whether a local repository mirror exists.
	 * 
	 * @return True when the repository exists.
	 */
	private boolean hgExists () {
		Path repo = localAbsolutePath.resolve(".hg");
		boolean exists = Files.isDirectory(repo);
		return exists;
	}
	
	/**
	 * Initializes a local repository mirror.
	 * 
	 * @throws SourceControlException
	 */
	private void hgInitialize () throws SourceControlException {
		DefaultExecutor executor = new DefaultExecutor ();
		executor.setWorkingDirectory(localAbsolutePath.toFile());
		
		CommandLine command = new CommandLine ("hg")
			.addArgument("init", false)
			.addArgument(localAbsolutePath.toString(), false);

		try {
			int result = executor.execute(command);
			if (result != 0) throw new SourceControlException ("Failed to initialize the repository, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to initialize the repository.", e); 
		}
	}

	/**
	 * Lists revisions from repository.
	 * 
	 * @param date Date filter specification or null for no filter.
	 * @param limit Revision count limit or null for no limit.
	 * @return List of revisions.
	 * @throws SourceControlException
	 */
	public List<Revision> hgLog(String date, String limit) throws SourceControlException {
		final List<Revision> list = new LinkedList<Revision> ();
		
		class LogParser extends LogOutputStream {
			@Override
			protected void processLine(String line, int level) {
				String [] record = line.split ("\\t");
				try {
					String hash = record [0];
					Instant date = Instant.from(stampFormatter.parse(record [1]));
					list.add(new MercurialRevision(MercurialRepository.this, hash, date));
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
		
		CommandLine command = new CommandLine ("hg")
			.addArgument("log", false)
			.addArgument("--template", false) 
			.addArgument("{node}\\t{date|isodatesec}\\n", false);
		
		if (date != null) command.addArgument("--date", false).addArgument(date, false);
		if (limit != null) command.addArgument("--limit", false).addArgument(limit, false);
		
		try {
			int result = executor.execute(command);			
			if (result != 0) throw new SourceControlException ("Failed to query the repository log, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to query the repository log.", e); 
		}
		
		return list;
	}
	
	/**
	 * Exports revision from repository.
	 * 
	 * @param revision Revision to clone.
	 * @param exportPath Directory to clone to.
	 * @throws SourceControlException
	 */
	private void hgClone(String revision, Path exportPath) throws SourceControlException {
		DefaultExecutor executor = new DefaultExecutor ();
		executor.setWorkingDirectory(localAbsolutePath.toFile());
		
		CommandLine command = new CommandLine ("hg")
			.addArgument("clone", false)
			.addArgument("--rev", false).addArgument(revision, false)
			.addArgument(localAbsolutePath.toString(), false)
			.addArgument(exportPath.toAbsolutePath().toString(), false);
		
		try {
			int result = executor.execute(command);
			if (result != 0) throw new SourceControlException ("Failed to clone the repository, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to clone the repository.", e); 
		}
	}
	
	/*****************************************************************************/
	
	@Override
	public void pull() throws SourceControlException {
		DefaultExecutor executor = new DefaultExecutor ();
		executor.setWorkingDirectory(localAbsolutePath.toFile());
		
		CommandLine command = new CommandLine ("hg")
			.addArgument("pull", false)
			.addArgument(remoteAddress, false);
		
		try {
			int result = executor.execute(command);			
			if (result != 0) throw new SourceControlException ("Failed to pull the repository, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new SourceControlException("Failed to pull the repository.", e); 
		}
	}

	@Override
	public List<Revision> list(Instant from) throws SourceControlException {
		return hgLog(">" + stampFormatter.format(from.atOffset(ZoneOffset.UTC)), null);
	}

	@Override
	public void checkout(Instant instant, Path directory) throws SourceControlException {
		List<Revision> candidateRevisions = hgLog("<" + stampFormatter.format(instant.atOffset(ZoneOffset.UTC)), "1");
		if (candidateRevisions.isEmpty()) throw new SourceControlException("Attempting to checkout a revision that does not exist.");
		MercurialRevision nativeRevision = (MercurialRevision) candidateRevisions.get(0);
		String hashRevision = nativeRevision.hash;
		hgClone(hashRevision, directory);
	}
		
	@Override
	public void checkout(Revision revision, Path directory) throws SourceControlException {
		MercurialRevision nativeRevision = (MercurialRevision) revision;
		if (nativeRevision == null) throw new SourceControlException ("Attempting to checkout revision from a different version control system.");
		if (!nativeRevision.repository.equals(this)) throw new SourceControlException ("Attempting to checkout revision from a different repository.");
		String hashRevision = nativeRevision.hash;
		hgClone(hashRevision, directory);
	}
}
