package cz.cuni.mff.d3s.been.util.storage.vanilla;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import cz.cuni.mff.d3s.been.util.storage.Storage;
import cz.cuni.mff.d3s.been.util.storage.StorageException;

public class VanillaStorage implements Storage {

	private static final int FILE_BUFFER_SIZE = 1024;

	private static final String PATH_TEMPSPACE = ".temp";
	private static final String PATH_PACKSPACE = ".pack";
	private static final String PATH_KEEPSPACE = ".keep";
	private static final String PATH_STATUS = ".status";

	private Path storagePath;

	public VanillaStorage(Path argStoragePath) throws StorageException {
		storagePath = argStoragePath;

		if (!Files.isDirectory(storagePath)) {
			throw new StorageException("Vanilla storage path does not exist.");
		}
	}

	/*****************************************************************************/

	private Path getBasePath(Iterable<String> address) {
		// Build the base path by concatenating address elements
		Path basePath = storagePath;
		for (String element : address) {
			basePath = basePath.resolve(element);
		}
		return (basePath);
	}

	private Path getInternalPath(Iterable<String> address, String directory) throws StorageException {
		Path basePath = getBasePath(address);
		Path internalPath = basePath.resolve(directory);

		// Make sure the path exists
		try {
			Files.createDirectories(internalPath);
		} catch (IOException e) {
			throw new StorageException("Failed to create internal path.", e);
		}

		return (internalPath);
	}

	@Override
	public Path getWorkspacePath(Iterable<String> address, Workspace workspace) throws StorageException {
		String directory;
		switch (workspace) {
			case TEMP: directory = PATH_TEMPSPACE; break;
			case PACK: directory = PATH_PACKSPACE; break;
			case KEEP: directory = PATH_KEEPSPACE; break;
			default: throw new StorageException ("Unknown workspace type.");
		}
		
		return getInternalPath(address, directory);
	}

	private void copyPathToPath(Path source, Path destination) throws StorageException {

		// We do not use Commons IO because it does not preserve attributes.

		DefaultExecutor executor = new DefaultExecutor ();
		
		CommandLine command = new CommandLine ("cp")
			.addArgument("-a", false)
			.addArgument(source.toString(), false)
			.addArgument(destination.toString(), false);

		try {
			int result = executor.execute(command);
			if (result != 0) throw new StorageException ("Failed to copy storage, exit code " + result + ".");
		}
		catch (IOException e)
		{
			throw new StorageException("Failed to copy storage.", e); 
		}
	}
	
	@Override
	public void copyToWorkspace(Iterable<String> address, Workspace workspace, Path source, Path destination) throws StorageException {
		Path workspacePath = getWorkspacePath(address, workspace);
		
		if (source == null) source = Paths.get("");
		if (destination == null) destination = Paths.get("");
		Path workspaceDestination = workspacePath.resolve(destination);
		copyPathToPath(source,workspaceDestination);
	}
	
	@Override
	public void copyFromWorkspace(Iterable<String> address, Workspace workspace, Path source, Path destination) throws StorageException {
		Path workspacePath = getWorkspacePath(address, workspace);
		
		if (source == null) source = Paths.get("");
		if (destination == null) destination = Paths.get("");
		Path workspaceSource = workspacePath.resolve(source);
		copyPathToPath(workspaceSource,destination);
	}
	
	/*****************************************************************************/

	private FileChannel getLockedStatusChannel(Iterable<String> address) throws StorageException {
		Path basePath = getBasePath(address);
		Path statusPath = basePath.resolve(PATH_STATUS);
		
		FileChannel statusChannel;
		
		try {
			Files.createDirectories(basePath);
			
			// We want to return the channel which is why we cannot try with resources here.
			statusChannel = FileChannel.open(
					statusPath,
					StandardOpenOption.READ,
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE);
			
			try {
				statusChannel.lock();
			} catch (IOException e) {
				statusChannel.close();
				throw new StorageException("Failed to lock status file.", e);
			}
		} catch (IOException e) {
			throw new StorageException("Failed to access status file.", e);
		}
		
		return (statusChannel);
	}
	
	private Status readStatusFromChannel(FileChannel statusChannel) throws StorageException {

		ByteBuffer statusBuffer = ByteBuffer.allocate(FILE_BUFFER_SIZE);
		try {
			statusChannel.read(statusBuffer);
		} catch (IOException e) {
			throw new StorageException("Failed to read status file.", e);
		}
		
		byte [] statusArray = new byte [statusBuffer.position()];
		statusBuffer.position(0);
		statusBuffer.get (statusArray);
		String statusText = new String(statusArray, StandardCharsets.UTF_8);

		if (statusText.isEmpty()) return (Status.NONE);
		
		try {
			return (Status.valueOf(statusText));
		} catch (IllegalArgumentException e) {
			throw new StorageException("Invalid status file.", e);
		}
	}

	@Override
	public Status getStatus(Iterable<String> address) throws StorageException {
		try (FileChannel statusChannel = getLockedStatusChannel(address)) {
			Status status = readStatusFromChannel(statusChannel);
			
			return (status);
			
		} catch (IOException e) {
			throw new StorageException("Failed to close status file.", e);
		}
	}

	@Override
	public boolean compareAndSetStatus(Iterable<String> address, Status oldStatus, Status newStatus) throws StorageException {
		try (FileChannel statusChannel = getLockedStatusChannel(address)) {
			Status currentStatus = readStatusFromChannel(statusChannel);
				
			if (currentStatus != oldStatus) return (false);
	
			byte [] statusArray = newStatus.name().getBytes(StandardCharsets.UTF_8);
			ByteBuffer statusBuffer = ByteBuffer.wrap(statusArray);
	
			try {
				statusChannel.truncate(0);
				statusChannel.write(statusBuffer);
			} catch (IOException e) {
				throw new StorageException("Failed to update status file.", e);
			}

			return (true);
	
		} catch (IOException e) {
			throw new StorageException("Failed to close status file.", e);
		}
	}

	/*****************************************************************************/

	@Override
	public Iterable<String> list(Iterable<String> address) throws StorageException {
		Path basePath = getBasePath(address);
		List<String> list = new LinkedList<String> ();
		
		class ListFilter implements DirectoryStream.Filter<Path> {
			@Override
			public boolean accept(Path entry) throws IOException {
				return (!Files.isHidden(entry));
			}
		}
		
		try (DirectoryStream<Path> directory = Files.newDirectoryStream(basePath, new ListFilter ()))	{
			for (Path item : directory) {
				list.add(item.toString());
			}
		} catch (IOException e) {
			throw new StorageException ("Failed to list storage.", e);
		}
		
		return (list);
	}
}
