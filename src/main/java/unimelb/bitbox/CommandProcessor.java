package unimelb.bitbox;

import unimelb.bitbox.util.Document;

public class CommandProcessor {
    private final Connection connection;

    public CommandProcessor(Connection connection) {
        this.connection = connection;
    }

    public void handleRequest(Document message) {
        switch(message.getString("command")) {
            //Do stuff
        }
    }

    public void handleResponse(Document message) {
        switch(message.getString("command")) {
            //Do stuff
        }
    }
}
