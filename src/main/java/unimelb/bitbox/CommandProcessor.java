package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

public class CommandProcessor {
    private FileSystemManager fileSystemManager;

    public CommandProcessor(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public ArrayList<Document> handleMessage(Document msgIn) {

        ArrayList<Document> msgOut = new ArrayList<Document>();
        Document newMsg = new Document();


        String msgInCommand = msgIn.getString(Commands.COMMAND); // request received
        String msgOutCommand = Commands.INVALID_PROTOCOL; // default response

        // other common field names
        String pathName, md5;
        Document fileDescriptor = new Document();
        Long fileSize;
        Long lastModified;
        Boolean success;

        String message = checkFieldsComplete(msgInCommand, msgIn); // message field

        // check if there were any missing fields identified
        // (if there were, the message will contain a missing fields message)
        if (!message.equals("")) {
            newMsg.append(Commands.COMMAND, msgOutCommand);
            newMsg.append(Commands.MESSAGE, message);
            msgOut.add(newMsg);
            return msgOut;
        }

        msgOutCommand = msgIn.getString(Commands.COMMAND);
        switch (msgInCommand) {
            case Commands.FILE_CREATE_REQUEST:
                msgOutCommand = Commands.FILE_CREATE_RESPONSE;
                success = false;
                fileDescriptor = (Document) msgIn.get(Commands.FILE_DESCRIPTOR);
                pathName = msgIn.getString(Commands.PATH_NAME);

                // check that the file can be created
                if (!fileSystemManager.isSafePathName(pathName)) {
                    message = "unsafe pathname given";
                } else if (fileSystemManager.fileNameExists(pathName)) {
                    message = "pathname already exists";
                } else {

                    // try to create file loader
                    boolean loaderCreated;

                    md5 = fileDescriptor.getString("md5");
                    fileSize = safeGetLong(fileDescriptor, "fileSize");
                    lastModified = safeGetLong(fileDescriptor, "lastModified");

                    try {
                        loaderCreated = fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
                    } catch (NoSuchAlgorithmException e) {
                        loaderCreated = false;
                    } catch (IOException e) {
                        loaderCreated = false;
                    }

                    if (loaderCreated) {
                        message = "file loader ready";
                        success = true;

                        // create a FILE_BYTES_REQUEST
                        newMsg = newFileBytesRequest(fileDescriptor, pathName, 0);
                        msgOut.add(newMsg);
                    } else {
                        message = "file loader not ready";
                    }

                    // create FILE_CREATE_RESPONSE
                    newMsg = file_related_reply(msgOutCommand, fileDescriptor, pathName, success, message);
                    msgOut.add(0, newMsg);
                }
                break;

            case Commands.FILE_DELETE_REQUEST:
                msgOutCommand = Commands.FILE_DELETE_RESPONSE;
                fileDescriptor = (Document) msgIn.get(Commands.FILE_DESCRIPTOR);
                pathName = msgIn.getString(Commands.PATH_NAME);
                success = false;

                // check the file can be deleted
                if (!fileSystemManager.isSafePathName(msgIn.getString("pathName"))) {
                    message = "unsafe pathname given";
                } else if (!fileSystemManager.fileNameExists(msgIn.getString("pathName"))) {
                    message = "pathname does not exist";
                } else {
                    lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);
                    md5 = fileDescriptor.getString(Commands.MD5);

                    success = fileSystemManager.deleteFile(pathName, lastModified, md5);
                    if (success) {
                        message = "file deleted";
                        msgOutCommand = Commands.FILE_DELETE_RESPONSE;
                    } else {
                        message = "there was a problem deleting the file";
                    }
                }
                newMsg = file_related_reply(msgOutCommand, fileDescriptor, pathName, success, message);
                msgOut.add(newMsg);
                break;

            //TODO implement FILE_BYTES_REQUEST for modifying files
            case Commands.FILE_MODIFY_REQUEST:
                msgOutCommand = Commands.FILE_MODIFY_RESPONSE;
                fileDescriptor = (Document) msgIn.get(Commands.FILE_DESCRIPTOR);
                pathName = msgIn.getString(Commands.PATH_NAME);
                success = false;

                // check that the file can be modified
                if (!fileSystemManager.isSafePathName(msgIn.getString("pathName"))) {
                    message = "unsafe pathname given";
                } else if (!fileSystemManager.fileNameExists(msgIn.getString("pathName"))) {
                    message = "pathname does not exist";
                } else {
                    lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);
                    md5 = fileDescriptor.getString(Commands.MD5);
                    message = "file loader ready";
                    try {
                        success = fileSystemManager.modifyFileLoader(pathName, md5, lastModified);
                    } catch (IOException e) {
                        message = "trouble accessing file IOException thrown";
                    }
                    if (!success) {
                        message = "there was a problem modifying the file";
                    }
                }
                newMsg = file_related_reply(msgOutCommand, fileDescriptor, pathName, success, message);
                msgOut.add(newMsg);
                break;

            case Commands.DIRECTORY_CREATE_REQUEST:
                pathName = msgIn.getString(Commands.PATH_NAME);
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
                newMsg = dir_related_reply(msgOutCommand, pathName, message, success);
                msgOut.add(newMsg);
                break;

            case Commands.DIRECTORY_DELETE_REQUEST:
                pathName = msgIn.getString(Commands.PATH_NAME);
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
                newMsg = dir_related_reply(msgOutCommand, pathName, message, success);
                msgOut.add(newMsg);
                break;


            //RESPONSES

            case Commands.FILE_BYTES_RESPONSE:

                String content;
                long position, length;

                pathName = msgIn.getString("pathName");
                content = msgIn.getString("content");
                position = safeGetLong(msgIn, "position");
                fileDescriptor = (Document) msgIn.get("fileDescriptor");
                fileSize = safeGetLong(fileDescriptor, "fileSize");
                length = safeGetLong(msgIn, "length");


                //TODO make sure that length < blockSize

                byte[] contentBytes = Base64.getDecoder().decode(content);
                ByteBuffer contentBB = null;
                contentBB.put(contentBytes);

                try {
                    if (fileSystemManager.writeFile(pathName, contentBB, position)) {

                        try {
                            if (position < fileSize || !fileSystemManager.checkWriteComplete(pathName)) {

                            } else {
                                long newPosition = position + length;
                                newMsg = newFileBytesRequest(fileDescriptor, pathName, newPosition);
                                msgOut.add(newMsg);
                            }
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                    //TODO output message for this error
                    e.printStackTrace();
                }

                break;

        }
        return msgOut;
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



    private Document newFileBytesRequest (Document fileDescriptor, String pathName, long position) {
        Document msg = new Document();
        msg.append("command", Commands.FILE_BYTES_REQUEST);
        msg.append("fileDescriptor", fileDescriptor);
        msg.append("pathName", pathName);
        msg.append("position", position);
        msg.append("length", Configuration.getConfigurationValue("blockSize"));
        return msg;
    }

    private long safeGetLong (Document doc, String key) {

        long val;
        try {
            val = doc.getLong(key);
        } catch (ClassCastException | NullPointerException e) {
            try {
                val = Long.parseLong(doc.getString(key));
            } catch (NumberFormatException f) {
                val = -1;
            }
        }
        return val;

    }

}