package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Logic for acting on commands received from a peer.
 *
 * Handles generating responses and driving the file system manager to read/write data.
 *
 * @author TransfictionRailways
 */
public class CommandProcessor {
    private FileSystemManager fileSystemManager;
    private ArrayList<Document> responses;
    private static Logger log = Logger.getLogger(ServerMain.class.getName());


    public CommandProcessor(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    /**
     * Main entry point. Parses a message from the client and acts upon it.
     * @param msgIn The message from the client
     * @return A (possibly empty) list of replies to be sent to the client
     * @throws BadMessageException If the client's message is malformed
     */
    public ArrayList<Document> handleMessage(Document msgIn) throws BadMessageException {
        this.responses = new ArrayList<>();
        String msgInCommand = msgIn.getString(Commands.COMMAND); // request received
        switch (msgInCommand) {
            case Commands.FILE_CREATE_REQUEST:
                fileCreateRequest(msgIn);
                break;

            case Commands.FILE_MODIFY_REQUEST:
                fileModifyRequest(msgIn);
                break;

            case Commands.FILE_DELETE_REQUEST:
                fileDeleteRequest(msgIn);
                break;

            case Commands.DIRECTORY_CREATE_REQUEST:
                dirCreateRequest(msgIn);
                break;

            case Commands.DIRECTORY_DELETE_REQUEST:
                dirDeleteRequest(msgIn);
                break;

            case Commands.FILE_BYTES_REQUEST:
                fileBytesRequest(msgIn);
                break;

            case Commands.FILE_BYTES_RESPONSE:
                fileBytesResponse(msgIn);
                break;

            /*
             * What about other responses from the peer? We don't bother handling them because we
             * don't care about their contents. Furthermore, it is not a problem for us if a peer send a response
             * without us having issued a request - we will be polite and not terminate the connection with
             * INVALID_PROTOCOL in an attempt to ensure good support for peers written by other people.
             */
        }
        return this.responses;
    }

    /**
     * Handle request to create a file and start requesting bytes for that file if successful.
     * @param msgIn The peer's request for file creation
     * @throws BadMessageException If the request is malformed
     */
    private void fileCreateRequest(Document msgIn) throws BadMessageException {
        Document fileDescriptor = msgIn.getDocument(Commands.FILE_DESCRIPTOR);
        String pathName = msgIn.getString(Commands.PATH_NAME);
        String message = "";

        // check that the file can be created
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
        } else if (fileSystemManager.fileNameExists(pathName)) {
            message = "pathname already exists";
        } else {
            // try to create file loader
            String md5 = fileDescriptor.getString(Commands.MD5);
            long fileSize = fileDescriptor.getLong(Commands.FILE_SIZE);
            long lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);

            try {
                boolean status = fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
                if (status) {
                    if(fileSystemManager.checkShortcut(pathName)) {
                        fileRelatedReply(Commands.FILE_CREATE_RESPONSE, fileDescriptor, pathName,
                                true, "transfer complete using local data");
                        return;
                    }
                    fileRelatedReply(Commands.FILE_CREATE_RESPONSE, fileDescriptor, pathName,
                            true, "file loader ready");
                    requestBytes(fileDescriptor, pathName, 0, fileSize);
                    return;
                }
                message = "file loader creation unsuccessful";
            } catch (NoSuchAlgorithmException e) {
                log.severe("Missing hashing algorithm: " + e.getLocalizedMessage());
                System.exit(1);
            } catch (IOException e) {
                message = "file loader creation unsuccessful: file system exception";
            }
        }
        fileRelatedReply(Commands.FILE_CREATE_RESPONSE, fileDescriptor, pathName,
                false, message);
    }

    /**
     * Handle request to modify a file and start requesting bytes for that file if successful.
     * @param msgIn The peer's request for file modification
     * @throws BadMessageException If the request is malformed
     */
    private void fileModifyRequest(Document msgIn) throws BadMessageException {
        Document fileDescriptor = msgIn.getDocument(Commands.FILE_DESCRIPTOR);
        String pathName = msgIn.getString(Commands.PATH_NAME);
        String message = "";

        // check that the file can be created
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
        } else if (!fileSystemManager.fileNameExists(pathName)) {
            message = "pathname does not exist";
        } else {
            // try to create file loader
            String md5 = fileDescriptor.getString(Commands.MD5);
            long fileSize = fileDescriptor.getLong(Commands.FILE_SIZE);
            long lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);

            try {
                boolean status = fileSystemManager.modifyFileLoader(pathName, md5, lastModified);
                if (status) {
                    if(fileSystemManager.checkShortcut(pathName)) {
                        fileRelatedReply(Commands.FILE_MODIFY_RESPONSE, fileDescriptor, pathName,
                                true, "transfer complete using local data");
                        return;
                    }
                    fileRelatedReply(Commands.FILE_MODIFY_RESPONSE, fileDescriptor, pathName,
                            true, "file loader ready");
                    requestBytes(fileDescriptor, pathName, 0, fileSize);
                    return;
                }
                message = "file loader creation unsuccessful";
            } catch (IOException e) {
                message = "file loader creation unsuccessful: file system exception";
            } catch (NoSuchAlgorithmException e) {
                log.severe("Missing hashing algorithm: " + e.getLocalizedMessage());
                System.exit(1);
            }
        }
        fileRelatedReply(Commands.FILE_MODIFY_RESPONSE, fileDescriptor, pathName,
                false, message);
    }

    /**
     * Handle request to delete a file and delete it if possible.
     * @param msgIn The peer's request for file deletion
     * @throws BadMessageException If the request is malformed
     */
    private void fileDeleteRequest(Document msgIn) throws BadMessageException {
        Document fileDescriptor = msgIn.getDocument(Commands.FILE_DESCRIPTOR);
        String pathName = msgIn.getString(Commands.PATH_NAME);
        String message;
        boolean status = false;

        // check the file can be deleted
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
        } else if (!fileSystemManager.fileNameExists(pathName)) {
            message = "pathname does not exist";
        } else {
            String md5 = fileDescriptor.getString(Commands.MD5);
            long lastModified = fileDescriptor.getLong(Commands.LAST_MODIFIED);
            status = fileSystemManager.deleteFile(pathName, lastModified, md5);
            if (status) {
                message = "file deleted";
            } else {
                message = "there was a problem deleting the file";
            }
        }
        fileRelatedReply(Commands.FILE_DELETE_RESPONSE, fileDescriptor, pathName,
                status, message);
    }

    /**
     * Handle request to create a directory and create it if possible.
     * @param msgIn The peer's request for directory creation
     * @throws BadMessageException If the request is malformed
     */
    private void dirCreateRequest(Document msgIn) throws BadMessageException {
        String pathName = msgIn.getString(Commands.PATH_NAME);
        String message;
        boolean status = false;

        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
        } else if (fileSystemManager.dirNameExists(pathName)) {
            message = "pathname already exists";
        } else {
            status = fileSystemManager.makeDirectory(pathName);
            message = "directory created";
            if (!status) {
                message = "there was a problem creating the directory";
            }
        }
        dirRelatedReply(Commands.DIRECTORY_CREATE_RESPONSE, pathName, message, status);
    }

    /**
     * Handle request to delete a directory and delete it if possible.
     * @param msgIn The peer's request for directory deletion
     * @throws BadMessageException If the request is malformed
     */
    private void dirDeleteRequest(Document msgIn) throws BadMessageException {
        String pathName = msgIn.getString(Commands.PATH_NAME);
        String message;
        boolean status = false;

        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
        } else if (!fileSystemManager.dirNameExists(pathName)) {
            message = "pathname does not exist";
        } else {
            status = fileSystemManager.deleteDirectory(pathName);
            message = "directory deleted";
            if (!status) {
                message = "there was a problem deleting the directory";
            }
        }
        dirRelatedReply(Commands.DIRECTORY_DELETE_RESPONSE, pathName, message, status);
    }

    /**
     * Handle request for bytes from a file. Replies with the data if possible.
     * @param msgIn The peer's request for file data
     * @throws BadMessageException If the request is malformed
     */
    private void fileBytesRequest(Document msgIn) throws BadMessageException {
        Document fileDescriptor = msgIn.getDocument(Commands.FILE_DESCRIPTOR);
        String pathName = msgIn.getString(Commands.PATH_NAME);
        long position = msgIn.getLong(Commands.POSITION);
        long length = msgIn.getLong(Commands.LENGTH);
        String md5 = fileDescriptor.getString(Commands.MD5);

        String content = "";
        String message = "";
        boolean status = false;

        try {
            // If peer requests bytes that don't exist we will return an unsuccessful read
            ByteBuffer contentBB = fileSystemManager.readFile(md5, position, length);
            if (contentBB != null) {
                content = Base64.getEncoder().encodeToString(contentBB.array());
                message = "successful read";
                status = true;
            } else {
                message = "unsuccessful read";
            }
        } catch (IOException e) {
            message = "file loader creation unsuccessful: file system exception";
        } catch (NoSuchAlgorithmException e) {
            log.severe("Missing hashing algorithm: " + e.getLocalizedMessage());
            System.exit(1);
        }

        returnBytes(fileDescriptor, pathName, position, length,
                content, message, status);
    }

    /**
     * Accepts receipt of data from the peer as an answer to one of our requests.
     * @param msgIn The peer's response with file data
     * @throws BadMessageException If the response is malformed
     */
    private void fileBytesResponse(Document msgIn) throws BadMessageException {
        boolean status = msgIn.getBoolean(Commands.STATUS);
        if (!status) {
            log.warning("Peer read for file " + msgIn.getString(Commands.PATH_NAME) + " failed: "
                    + msgIn.getString(Commands.MESSAGE));
            return;
            // A more robust error handling system would retry the request here
        }

        Document fileDescriptor = msgIn.getDocument(Commands.FILE_DESCRIPTOR);
        String pathName = msgIn.getString(Commands.PATH_NAME);
        String content = msgIn.getString(Commands.CONTENT);
        long position = msgIn.getLong(Commands.POSITION);
        long length = msgIn.getLong(Commands.LENGTH);
        long fileSize = fileDescriptor.getLong(Commands.FILE_SIZE);

        ByteBuffer contentBB;
        try {
            byte[] contentBytes = Base64.getDecoder().decode(content);
            if (contentBytes.length != length) {
                // The peer has misreported how much data it returned. Handle gracefully.
                length = contentBytes.length;
                log.warning("Peer misreported content length, correcting.");
            }
            contentBB = ByteBuffer.allocate((int)length);
            contentBB.put(contentBytes);
            contentBB.position(0);
        } catch (IllegalArgumentException e) {
            throw new BadMessageException("Malformed bytes response");
        }

        try {
            if (fileSystemManager.writeFile(pathName, contentBB, position)) {
                long newPosition = position + length;
                if (newPosition < fileSize) {
                    requestBytes(fileDescriptor, pathName, newPosition, fileSize);
                } else {
                    // this must be run as final step to write file
                    fileSystemManager.checkWriteComplete(pathName);
                }
            }
            else {
                log.severe("Failed to write bytes to " + pathName);
                fileSystemManager.cancelFileLoader(pathName);
            }
        } catch (NoSuchAlgorithmException e) {
            log.severe("Missing hashing algorithm: " + e.getLocalizedMessage());
            System.exit(1);
        } catch (IOException e) {
            log.severe("I/O error while writing bytes for " + pathName);
            try {
                fileSystemManager.cancelFileLoader(pathName);
            } catch (IOException f) {
                // We don't really care
            }
        }
    }

    /**
     * Writes the reply message for all file related requests e.g. FILE_CREATE, FILE_DELETE,
     * FILE_MODIFY
     * @param response the response command to send
     * @param fileDescriptor the description of the file as a Document object
     * @param pathName the path of the file
     * @param status whether the request was successfully fulfilled
     * @param message details of why the request succeeded/failed
     */
    private void fileRelatedReply(String response, Document fileDescriptor, String pathName,
                                        Boolean status, String message) {
        Document replyMsg = new Document();
        replyMsg.append(Commands.COMMAND, response);
        replyMsg.append(Commands.FILE_DESCRIPTOR, fileDescriptor);
        replyMsg.append(Commands.PATH_NAME, pathName);
        replyMsg.append(Commands.STATUS, status);
        replyMsg.append(Commands.MESSAGE, message);
        this.responses.add(replyMsg);
    }

    /**
     * Writes the reply message for all directory related requests e.g. DIR_CREATE, DIR_DELETE
     * @param response the response command to send
     * @param pathName the path of the file
     * @param status whether the request was successfully fulfilled
     * @param message details of why the request succeeded/failed
     */
    private void dirRelatedReply(String response, String pathName, String message,
                                       Boolean status) {
        Document replyMsg = new Document();
        replyMsg.append(Commands.COMMAND, response);
        replyMsg.append(Commands.PATH_NAME, pathName);
        replyMsg.append(Commands.MESSAGE, message);
        replyMsg.append(Commands.STATUS, status);
        this.responses.add(replyMsg);
    }

    /**
     * Generate a request to the peer for a section of a file. The maximum number of bytes read is
     * controlled by the blockSize configuration value.
     * @param fileDescriptor the description of the file as a Document object
     * @param pathName the path of the file
     * @param position byte offset to start reading from
     * @param fileSize the total size of the file
     */
    private void requestBytes(Document fileDescriptor, String pathName, long position, long fileSize) {
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

        this.responses.add(msg);
    }

    /**
     * Send bytes from a file to the peer.
     * @param fileDescriptor the description of the file as a Document object
     * @param pathName the path of the file
     * @param position the byte offset in the file where this data starts from
     * @param length the length of the data being sent
     * @param content data to be sent. Should be base64 encoded.
     * @param message details of why the request succeeded/failed
     * @param status whether the request was successfully fulfilled
     */
    private void returnBytes(Document fileDescriptor, String pathName, long position, long length,
                                           String content, String message, Boolean status) {
        Document msg = new Document();
        msg.append(Commands.COMMAND, Commands.FILE_BYTES_RESPONSE);
        msg.append(Commands.FILE_DESCRIPTOR, fileDescriptor);
        msg.append(Commands.PATH_NAME, pathName);
        msg.append(Commands.POSITION, position);
        msg.append(Commands.LENGTH, length);
        msg.append(Commands.CONTENT, content);
        msg.append(Commands.MESSAGE, message);
        msg.append(Commands.STATUS, status);
        this.responses.add(msg);
    }

}