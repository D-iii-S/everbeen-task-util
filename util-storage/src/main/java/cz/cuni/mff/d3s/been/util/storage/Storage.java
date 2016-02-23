package cz.cuni.mff.d3s.been.util.storage;

import java.nio.file.Path;

public interface Storage {

	enum Workspace { 
		/** Temporary workspace. Can be pruned when not used for some time. */
		TEMP, 
		/** Compressible workspace. Can be compressed when not used for some time. */
		PACK, 
		/** Permanent workspace. Will not be touched in any automated manner. Honest :-) */
		KEEP 	
	};
		
	enum Status { 
		/** The storage address does not exist yet. */
		NONE, 
		/** The storage address is used by an active task. */
		BUSY, 
		/** The storage address was used by a task that has failed. */
		FAIL, 
		/** The storage address was used by a task that has finished successfully. */
		DONE, 
		/** The storage address was used by a task that has finished successfully and was pruned and compressed afterwards. */
		PACK 
	};

	Status getStatus(Iterable<String> address) throws StorageException;
	boolean compareAndSetStatus(Iterable<String> address, Status oldStatus, Status newStatus) throws StorageException;

	Path getWorkspacePath(Iterable<String> address, Workspace workspace) throws StorageException;
	
	/** Copies files from elsewhere to storage workspace.
	 * <p>
	 * Source can be either a file or a directory.
	 * Destination must be a directory.
	 * 
	 * @param address storage address
	 * @param workspace storage workspace
	 * @param source source path, null for current directory
	 * @param destination workspace relative destination path, null for workspace directory
	 * @throws StorageException
	 */
	void copyToWorkspace(Iterable<String> address, Workspace workspace, Path source, Path destination) throws StorageException;

	/** Copies files from storage workspace to elsewhere.
	 * <p>
	 * Source can be either a file or a directory.
	 * Destination must be a directory.
	 * 
	 * @param address storage address
	 * @param workspace storage workspace
	 * @param source workspace relative source path, null for workspace directory
	 * @param destination destination path, null for current directory
	 * @throws StorageException
	 */
	void copyFromWorkspace(Iterable<String> address, Workspace workspace, Path source, Path destination) throws StorageException;

	Iterable<String> list(Iterable<String> address) throws StorageException;
}
