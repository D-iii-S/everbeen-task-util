package cz.cuni.mff.d3s.been.util.source;

import cz.cuni.mff.d3s.been.taskapi.TaskException;

@SuppressWarnings("serial")
public class SourceControlException extends TaskException {

	public SourceControlException(String message) {
		super(message);
	}

	public SourceControlException(String message, Throwable cause) {
		super(message, cause);
	}
}
