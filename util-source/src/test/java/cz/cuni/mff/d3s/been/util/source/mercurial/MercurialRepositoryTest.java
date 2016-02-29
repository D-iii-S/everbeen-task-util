package cz.cuni.mff.d3s.been.util.source.mercurial;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.junit.BeforeClass;

import cz.cuni.mff.d3s.been.util.source.RepositoryTestBase;

public class MercurialRepositoryTest extends RepositoryTestBase {

	private static Path commitTemporaryFile(DefaultExecutor executor, Path repository) throws Exception {
		Path file = Files.createTempFile(repository,  "",  "");
		CommandLine commandAdd = new CommandLine("hg").addArgument("add").addArgument(file.getFileName().toString());
		int resultAdd = executor.execute(commandAdd);
		assertEquals(0, resultAdd);
		CommandLine commandCommit = new CommandLine("hg").addArgument("commit").addArgument("--message").addArgument("message");
		int resultCommit = executor.execute(commandCommit);
		assertEquals(0, resultCommit);
		return file;
	}

	@BeforeClass
	public static void prepareRepository() throws Exception {

		// Everything is done in a temporary repository.

		Path repositoryPath = Files.createTempDirectory("");
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(repositoryPath.toFile());
		
		// Initialize the repository.

		CommandLine command = new CommandLine("hg").addArgument("init", false);
		int result = executor.execute(command);
		assertEquals(0, result);

		// Create three consecutive commits with different time stamps.
		
		fileOnePath = commitTemporaryFile(executor, repositoryPath);
		Thread.sleep(1000);
		fileTwoPath = commitTemporaryFile(executor, repositoryPath);
		Thread.sleep(1000);
		fileTrePath = commitTemporaryFile(executor, repositoryPath);

		// Create the repository object.
		
		Path temporaryPath = Files.createTempDirectory("");
		repositoryObject = new MercurialRepository(repositoryPath.toAbsolutePath().toString(), temporaryPath);
		repositoryObject.pull();
	}
}
