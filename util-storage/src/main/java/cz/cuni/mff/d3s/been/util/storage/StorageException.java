package cz.cuni.mff.d3s.been.util.storage;

import cz.cuni.mff.d3s.been.taskapi.TaskException;

@SuppressWarnings("serial")
public class StorageException extends TaskException {

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
