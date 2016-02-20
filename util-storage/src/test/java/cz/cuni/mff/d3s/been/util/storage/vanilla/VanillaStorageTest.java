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
}
