

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApplicationException extends Exception
{
	final Log log = LogFactory.getLog(ApplicationException.class);
	private static final long serialVersionUID = 9171857755318125858L;
	private String message;
	public ApplicationException(String message)
	{
		setMessage(message);
		log.error(message);
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}