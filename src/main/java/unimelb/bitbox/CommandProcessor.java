package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;

public class CommandProcessor {
    private FileSystemManager fileSystemManager;

    public CommandProcessor(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public Document handleRequest(Document msg) throws IOException {
        Document replyMsg = new Document();
        String request = msg.getString(Commands.COMMAND); // request received
        String response = Commands.INVALID_PROTOCOL; // default response
        String message = checkFieldsComplete(request, msg); // message field

        // other common field names
        String pathName, md5;
        Document fileDescriptor = new Document();
        Long lastModified;
        Boolean success;

        // check fields are complete
        if (!message.equals("")) {
            replyMsg.append(Commands.COMMAND, response);
            replyMsg.append(Commands.MESSAGE, message);
            return replyMsg;
        }

        response = msg.getString(Commands.COMMAND);
        switch(request) {
            case Commands.FILE_CREATE_REQUEST:
                fileDescriptor = (Document) msg.get(Commands.FILE_DESCRIPTOR);
                pathName = msg.getString(Commands.PATH_NAME);
                success = false;

                // check that the file can be created
                if (!fileSystemManager.isSafePathName(msg.getString("pathName"))) {
                    message = "unsafe pathname given";
                } else if (fileSystemManager.fileNameExists(msg.getString("pathName"))) {
                    message = "pathname already exists";
                } else {
                    message = "file loader ready";
                    success = true;
                }
                replyMsg = file_related_reply(response, fileDescriptor, pathName, success, message);
                break;

            case Commands.FILE_DELETE_REQUEST:
                fileDescriptor = (Document) msg.get(Commands.FILE_DESCRIPTOR);
                pathName = msg.getString(Commands.PATH_NAME);
                success = false;

                // check the file can be deleted
                if (!fileSystemManager.isSafePathName(msg.getString("pathName"))) {
                    message = "unsafe pathname given";
                } else if (!fileSystemManager.fileNameExists(msg.getString("pathName"))) {
                    message = "pathname does not exist";
                } else {
                    lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);
                    md5 = fileDescriptor.getString(Commands.MD5);
                    success = fileSystemManager.deleteFile(pathName, lastModified, md5);
                    message = "file deleted";
                    if (!success) {
                        message = "there was a problem deleting the file";
                    }
                }
                replyMsg = file_related_reply(response, fileDescriptor, pathName, success, message);
                break;

            case Commands.FILE_MODIFY_REQUEST:
                fileDescriptor = (Document) msg.get(Commands.FILE_DESCRIPTOR);
                pathName = msg.getString(Commands.PATH_NAME);
                success = false;

                // check that the file can be modified
                if (!fileSystemManager.isSafePathName(msg.getString("pathName"))) {
                    message = "unsafe pathname given";
                } else if (!fileSystemManager.fileNameExists(msg.getString("pathName"))) {
                    message = "pathname does not exist";
                } else {
                    lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);
                    md5 = fileDescriptor.getString(Commands.MD5);
                    success = fileSystemManager.modifyFileLoader(pathName, md5, lastModified);
                    message = "file loader ready";
                    if (!success) {
                        message = "there was a problem modifying the file";
                    }
                }
                replyMsg = file_related_reply(response, fileDescriptor, pathName, success, message);
                break;

            case Commands.DIRECTORY_CREATE_REQUEST:
                pathName = msg.getString(Commands.PATH_NAME);
                success = false;

                if (fileSystemManager.isSafePathName(pathName)) {
                    message = "unsafe pathname given";
                } else if (fileSystemManager.dirNameExists(pathName)) {
                    message = "pathname already exists";
                } else {
                    success = fileSystemManager.makeDirectory(pathName);
                    message = "directory created";
                    if (!success) {
                        message = "there was a problem creating the directory";
                    }
                }
                replyMsg = dir_related_reply(response, pathName, message, success);
                break;

            case Commands.DIRECTORY_DELETE_REQUEST:
                pathName = msg.getString(Commands.PATH_NAME);
                success = false;

                if (fileSystemManager.isSafePathName(pathName)) {
                    message = "unsafe pathname given";
                } else if (!fileSystemManager.dirNameExists(pathName)) {
                    message = "pathname does not exist";
                } else {
                    success = fileSystemManager.deleteDirectory(pathName);
                    message = "directory deleted";
                    if (!success) {
                        message = "there was a problem deleting the directory";
                    }
                }
                replyMsg = dir_related_reply(response, pathName, message, success);
                break;

        }
        return replyMsg;
    }

    /**
     * Writes the reply message for all file related requests e.g. FILE_CREATE, FILE_DELETE,
     * FILE_MODIFY
     * @param response the protocol request
     * @param fileDescriptor the description of the file as a Document object
     * @param pathName the path of the file
     * @param success whether the request was successfully fulfilled
     * @param message details of why the request succeeded/failed
     * @return the reply message
     */
    private Document file_related_reply(String response, Document fileDescriptor, String pathName,
                                        Boolean success, String message) {
        Document replyMsg = new Document();
        replyMsg.append(Commands.COMMAND, response);
        replyMsg.append(Commands.FILE_DESCRIPTOR, fileDescriptor);
        replyMsg.append(Commands.PATH_NAME, pathName);
        replyMsg.append(Commands.STATUS, success.toString());
        replyMsg.append(Commands.MESSAGE, message);
        return replyMsg;
    }

    /**
     * Writes the reply message for all directory related requests e.g. DIR_CREATE, DIR_DELETE
     * @param response the protocol request
     * @param pathName the path of the file
     * @param success whether the request was successfully fulfilled
     * @param message details of why the request succeeded/failed
     * @return the reply message
     */
    private Document dir_related_reply(String response, String pathName, String message,
                                       Boolean success) {
        Document replyMsg = new Document();
        replyMsg.append(Commands.COMMAND, response);
        replyMsg.append(Commands.PATH_NAME, pathName);
        replyMsg.append(Commands.MESSAGE, message);
        replyMsg.append(Commands.STATUS, success.toString());
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

    /**
     * Returns the missing field error message
     * @param field the field that is missing
     * @return the error message
     */
    private String missingField(String field) {
        return "message must contain a " + field + " field as string";
    }

    public void handleResponse(Document message) {
        switch(message.getString(Commands.COMMAND)) {
            //Do stuff
        }
    }
}
