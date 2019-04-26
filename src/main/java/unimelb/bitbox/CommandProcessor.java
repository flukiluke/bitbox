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

        // no further handling needed
        if (msgInCommand.equals(Commands.INVALID_PROTOCOL)) return msgOut;

        // field names
        String pathName, md5, content;
        Document fileDescriptor;
        long fileSize, lastModified, position, length;
        boolean status;

        // used for byte requests
        byte[] contentBytes;
        ByteBuffer contentBB;

        boolean success;

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
                    System.out.println("000");

                    md5 = fileDescriptor.getString("md5");
                    fileSize = safeGetLong(fileDescriptor, "fileSize");
                    lastModified = safeGetLong(fileDescriptor, "lastModified");

                    try {
                        loaderCreated = fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);

                        if (loaderCreated) {
                            success = true;
                            message = "file loader ready";

                            // create a FILE_BYTES_REQUEST
                            newMsg = newFileBytesRequest(fileDescriptor, pathName, 0, fileSize);
                            msgOut.add(newMsg);
                        } else {
                            success = false;
                            message = "file loader creation unsuccessful";
                        }
                    } catch (NoSuchAlgorithmException e) {
                        success = false;
                        message = "file loader creation unsuccessful: MD5 algorithm not available";
                    } catch (IOException e) {
                        success = false;
                        message = "file loader creation unsuccessful: file system exception";
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


            case Commands.FILE_BYTES_RESPONSE:

                status = msgIn.getBoolean("status");
                if (status == false) break;

                fileDescriptor = (Document) msgIn.get("fileDescriptor");
                pathName = msgIn.getString("pathName");
                content = msgIn.getString("content");
                position = safeGetLong(msgIn, "position");
                length = safeGetLong(msgIn, "length");
                fileSize = safeGetLong(fileDescriptor, "fileSize");

                contentBytes = Base64.getDecoder().decode(content);
                // TODO handle exception if we the peer gives us the wrong length! or calculate our own length?
                contentBB = ByteBuffer.allocate((int) length);
                contentBB.put(contentBytes);

                try {
                    if (fileSystemManager.writeFile(pathName, contentBB, position)) {

                        try {
                            if (position < fileSize || !fileSystemManager.checkWriteComplete(pathName)) {
                                long newPosition = position + length;
                                newMsg = newFileBytesRequest(fileDescriptor, pathName, newPosition, fileSize);
                                msgOut.add(newMsg);
                            }
                        } catch (NoSuchAlgorithmException e) {
                            //TODO need to handle
                            e.printStackTrace();
                        } catch (IOException e) {
                            //TODO need to handle
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                    //TODO need to handle
                    e.printStackTrace();
                }



            case Commands.FILE_BYTES_REQUEST:

                fileDescriptor = (Document) msgIn.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                position = safeGetLong(msgIn, "position");
                length = safeGetLong(msgIn, "length");

                pathName = msgIn.getString("pathName");

                //TODO do we need to reject requests if the length < blockSize? doesn't say so in the spec.

                try {
                    contentBB = fileSystemManager.readFile(md5, position, length);

                    if (contentBB != null) {
                        contentBytes = contentBB.array();
                        content = Base64.getEncoder().encodeToString(contentBytes);
                        message = "successful read";
                        status = true;
                    } else {
                        content = "";
                        message = "unsuccessful read";
                        status = false;
                    }
                } catch (IOException e) {
                    content = "";
                    status = false;
                    message = "file loader creation unsuccessful: file system exception";
                } catch (NoSuchAlgorithmException e) {
                    content = "";
                    status = false;
                    message = "file loader creation unsuccessful: MD5 algorithm not available";
                }

                //TODO update length to reflect actual size of returned content
                newMsg = newFileBytesResponse(fileDescriptor, pathName, position, length, content, message, status);
                msgOut.add(newMsg);
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
        System.out.println(command);
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
        return "message is missing required field: " + field;
    }



    private Document newFileBytesRequest (Document fileDescriptor, String pathName, long position, long fileSize) {
        Document msg = new Document();

        // calculate length to request
        long length = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
        long remaining = fileSize - position;
        if (remaining < length) length = remaining;

        msg.append(Commands.COMMAND, Commands.FILE_BYTES_REQUEST);
        msg.append(Commands.FILE_DESCRIPTOR, fileDescriptor);
        msg.append(Commands.PATH_NAME, pathName);
        msg.append(Commands.POSITION, position);
        msg.append(Commands.LENGTH, length);

        return msg;
    }

    private Document newFileBytesResponse (Document fileDescriptor, String pathName, long position, long length,
                                           String content, String message, boolean status) {
        Document msg = new Document();
        msg.append(Commands.COMMAND, Commands.FILE_BYTES_RESPONSE);
        msg.append(Commands.FILE_DESCRIPTOR, fileDescriptor);
        msg.append(Commands.PATH_NAME, pathName);
        msg.append(Commands.POSITION, position);
        msg.append(Commands.LENGTH, length);
        msg.append(Commands.CONTENT, content);
        msg.append(Commands.MESSAGE, message);
        msg.append(Commands.STATUS, status);
        return msg;
    }


    //TODO can remove if we implement a check in Commands.java that message fields are in correct type
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