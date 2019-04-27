package unimelb.bitbox.util;

import unimelb.bitbox.BadMessageException;
import unimelb.bitbox.Commands;

/**
 * Simple class to manage a host string and port number. Provides conversion to and from a {@link Document}
 * which further provides conversion to a JSON string.
 * @author aaron
 *
 */
public class HostPort {
	public String host;
	public int port;
	public HostPort(String host, int port) {
		this.host=host;
		this.port=port;
	}
	public HostPort(String hostPort) {
		this.host=hostPort.split(":")[0];
		this.port=Integer.parseInt(hostPort.split(":")[1]);
	}
	public HostPort(Document hostPort) throws BadMessageException {
		this.host=hostPort.getString(Commands.HOST);
		this.port=(int) hostPort.getLong(Commands.PORT);
	}
	public Document toDoc() {
		Document hp = new Document();
		hp.append(Commands.HOST, host);
		hp.append(Commands.PORT, port);
		return hp;
	}
	public String toString() {
		return host+":"+port;
	}
	
	@Override
    public boolean equals(Object o) { 
        if (o == this) { 
            return true; 
        } 
        if (!(o instanceof HostPort)) { 
            return false; 
        } 
        HostPort c = (HostPort) o;   
        return host.equals(c.host) && port==c.port; 
    } 
}
