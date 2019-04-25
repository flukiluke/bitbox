package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandProcessor {
    private FileSystemManager fileSystemManager;

    public CommandProcessor(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public Document handleRequest(Document msg) {
        Document replyMsg = new Document();
        String request = msg.getString(Commands.COMMAND); // request received
        String response = Commands.INVALID_PROTOCOL; // default response
        String message = checkFieldsComplete(request, msg); // message field

        //check fields are complete
        if (!message.equals("")) {
            replyMsg.append(Commands.COMMAND, response);
            replyMsg.append(Commands.MESSAGE, message);
            return replyMsg;
        }

        switch(request) {
            case Commands.FILE_CREATE_REQUEST:
                System.out.println("yes");
                response = Commands.FILE_CREATE_RESPONSE;
                Boolean success = false;

                if (!fileSystemManager.isSafePathName(msg.getString("pathName"))) {
                    message = "unsafe pathname";
                } else if (!fileSystemManager.fileNameExists(msg.getString("pathName"))) {
                    message = "pathname already exists";
                } else {
                    message = "file loader ready";
                    success = true;
                }

                replyMsg.append(Commands.COMMAND, response);
                replyMsg.append(Commands.FILE_DESCRIPTOR,
                        (Document) msg.get(Commands.FILE_DESCRIPTOR));
                replyMsg.append(Commands.PATH_NAME, msg.getString(Commands.PATH_NAME));
                replyMsg.append(Commands.STATUS, success.toString());
                replyMsg.append(Commands.MESSAGE, message);
                break;
        }
        return replyMsg;
    }

    /**
     * Checks if any fields are missing
     * @param command the protocol of the message
     * @param msg the JSON message being received
     * @return
     */
    private String checkFieldsComplete(String command, Document msg) {
        for (String field: (String[]) Commands.validFields.get(command)) {
            if (!msg.containsKey(field)) {
                return missingField(field);
            }
        }
        return "";
    }

    private String missingField(String field) {
        return "message must contain a " + field + " field as string";
    }

    public void handleResponse(Document message) {
        switch(message.getString(Commands.COMMAND)) {
            //Do stuff
        }
    }
}
