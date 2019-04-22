package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandProcessor {
    private FileSystemManager fileSystemManager;
    private Map<String, List<String[]>> validFields = new HashMap<String, List<String[]>>();

    public CommandProcessor(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public Document handleRequest(Document msg) {
        Document replyMsg = new Document();
        switch(msg.getString("command")) {
            case Commands.FILE_CREATE_REQUEST:
                String command, message;
                Boolean success = false;
                command = Commands.INVALID_PROTOCOL;
                if (!msg.containsKey("command")) {
                    message = missingField("command");
                } else if (!msg.containsKey("fileDescriptor")) {
                    message = missingField("fileDescriptor");
                } else if (!msg.containsKey("pathName")) {
                    message = missingField("pathName");
                } else {
                    command = Commands.FILE_CREATE_RESPONSE;
                    if (!fileSystemManager.isSafePathName(msg.getString("pathName"))) {
                        message = "unsafe pathname";
                    } else if (!fileSystemManager.fileNameExists(msg.getString("pathName"))) {
                        message = "pathname already exists";
                    } else {
                        message = "file loader ready";
                        success = true;
                    }
                }
                replyMsg.append("command", command);
                if (command.equals(Commands.FILE_CREATE_RESPONSE)) {
                    replyMsg.append("fileDescriptor",
                            Document.parse(msg.getString("fileDescriptor")));
                    replyMsg.append("pathName", msg.getString("pathName"));
                    replyMsg.append("status", success.toString());
                }
                replyMsg.append("message", message);
        }
        return replyMsg;
    }

    private String missingField(String field) {
        return "message must contain a " + field + " field as string";
    }

    public void handleResponse(Document message) {
        switch(message.getString("command")) {
            //Do stuff
        }
    }
}
