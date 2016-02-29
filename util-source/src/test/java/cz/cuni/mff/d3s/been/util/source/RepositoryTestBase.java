package cz.cuni.mff.d3s.been.util.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class RepositoryTestBase {

	protected static Repository repositoryObject;
	
	protected static Path fileOnePath;
	protected static Path fileTwoPath;
	protected static Path fileTrePath;

	@Test
	public void testVersionCount() throws Exception {
		// The repository must hold three different versions.
		assertEquals(3, repositoryObject.list(Instant.EPOCH).size());
	}

	private Path cloneSequentialRevision(int sequence) throws Exception {
		Path clonePath = Files.createTempDirectory("");
		List<Revision> revisions = repositoryObject.list(Instant.EPOCH);
		Collections.sort(revisions);
		repositoryObject.checkout(revisions.get(sequence).getDate(), clonePath);
		return clonePath;
	}

	@Test
	public void testVersionOne() throws Exception {
		Path clonePath = cloneSequentialRevision(0);
		assertTrue(Files.isReadable(clonePath.resolve(fileOnePath.getFileName())));
		assertFalse(Files.isReadable(clonePath.resolve(fileTwoPath.getFileName())));
		assertFalse(Files.isReadable(clonePath.resolve(fileTrePath.getFileName())));
	}

	@Test
	public void testVersionTwo() throws Exception {
		Path clonePath = cloneSequentialRevision(1);
		assertTrue(Files.isReadable(clonePath.resolve(fileOnePath.getFileName())));
		assertTrue(Files.isReadable(clonePath.resolve(fileTwoPath.getFileName())));
		assertFalse(Files.isReadable(clonePath.resolve(fileTrePath.getFileName())));
	}

	@Test
	public void testVersionTre() throws Exception {
		Path clonePath = cloneSequentialRevision(2);
		assertTrue(Files.isReadable(clonePath.resolve(fileOnePath.getFileName())));
		assertTrue(Files.isReadable(clonePath.resolve(fileTwoPath.getFileName())));
		assertTrue(Files.isReadable(clonePath.resolve(fileTrePath.getFileName())));
	}

}