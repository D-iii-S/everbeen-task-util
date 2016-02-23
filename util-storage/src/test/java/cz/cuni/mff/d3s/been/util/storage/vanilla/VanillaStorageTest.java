package cz.cuni.mff.d3s.been.util.storage.vanilla;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import cz.cuni.mff.d3s.been.util.storage.Storage;

public class VanillaStorageTest {

	private static Storage storage;

	@BeforeClass
	public static void prepareStorage() throws Exception {
		Path temporary = Files.createTempDirectory("");
		storage = new VanillaStorage(temporary);
	}
	
	@Test
	public void testEmptyAddressStatus() throws Exception {
		Iterable<String> address = Arrays.asList();
		assertTrue (storage.getStatus(address) == Storage.Status.NONE);
	}
	
	@Test
	public void testShallowAddressStatus() throws Exception {
		Iterable<String> address = Arrays.asList("shallow");
		assertTrue (storage.getStatus(address) == Storage.Status.NONE);
		assertTrue (storage.compareAndSetStatus(address, Storage.Status.NONE, Storage.Status.BUSY));
		assertTrue (storage.getStatus(address) == Storage.Status.BUSY);
		assertTrue (storage.compareAndSetStatus(address, Storage.Status.BUSY, Storage.Status.FAIL));
		assertTrue (storage.getStatus(address) == Storage.Status.FAIL);
	}

	@Test
	public void testDeepAddressStatus() throws Exception {
		Iterable<String> address = Arrays.asList("deep", "deep", "deep");
		assertTrue (storage.getStatus(address) == Storage.Status.NONE);
		assertTrue (storage.compareAndSetStatus(address, Storage.Status.NONE, Storage.Status.BUSY));
		assertTrue (storage.getStatus(address) == Storage.Status.BUSY);
		assertTrue (storage.compareAndSetStatus(address, Storage.Status.BUSY, Storage.Status.FAIL));
		assertTrue (storage.getStatus(address) == Storage.Status.FAIL);
	}
	
	@Test
	public void testFileCopyToWorkspace() throws Exception {
		Iterable<String> address = Arrays.asList("copy", "file");
		Path source = Files.createTempFile("", "");
		storage.copyToWorkspace(address, Storage.Workspace.KEEP, source, null);
		Path workspace = storage.getWorkspacePath(address, Storage.Workspace.KEEP);
		Path destination = workspace.resolve(source.getFileName());
		assertTrue(Files.isRegularFile(destination));
	}
	
	@Test
	public void testDirectoryCopyToWorkspace() throws Exception {
		Iterable<String> address = Arrays.asList("copy", "directory");
		Path source = Files.createTempDirectory("");
		storage.copyToWorkspace(address, Storage.Workspace.KEEP, source, source.getFileName());
		Path workspace = storage.getWorkspacePath(address, Storage.Workspace.KEEP);
		Path destination = workspace.resolve(source.getFileName());
		assertTrue(Files.isDirectory(destination));
	}

	@Test
	public void testRecursiveCopyToWorkspace() throws Exception {
		Iterable<String> address = Arrays.asList("copy", "recursive");
		Path source = Files.createTempDirectory("");
		Path innerFile = Files.createTempFile(source,  "",  "");
		Path innerDirectory = Files.createTempDirectory(source,  "");
		storage.copyToWorkspace(address, Storage.Workspace.KEEP, source, null);
		Path destination = storage.getWorkspacePath(address, Storage.Workspace.KEEP);
		Path destinationFile = destination.resolve(innerFile.getFileName());
		Path destinationDirectory = destination.resolve(innerDirectory.getFileName());
		assertTrue(Files.isRegularFile(destinationFile));
		assertTrue(Files.isDirectory(destinationDirectory));
	}
}
