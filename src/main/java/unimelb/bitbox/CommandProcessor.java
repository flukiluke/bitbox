package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CommandProcessor {
    private FileSystemManager fileSystemManager;
    private Map<String, List<String[]>> validFields = new HashMap<String, List<String[]>>();

    public CommandProcessor(FileSystemManager fileSystemManager) {
        this.fileSystemManager = fileSystemManager;
    }

    public ArrayList<Document> handleRequest(Document msg) {
        ArrayList<Document> replyMsgs = new ArrayList<Document>();


        switch(msg.getString("command")) {
            case Commands.FILE_CREATE_REQUEST: {
                String message, command;
                Boolean success = false;

                command = Commands.INVALID_PROTOCOL;
                if (!msg.containsKey("command")) {
                    message = missingField("command");
                } else if (!msg.containsKey("fileDescriptor")) {
                    message = missingField("fileDescriptor");
                } else if (!msg.containsKey("md5")) {
                    message = missingField("md5");
                } else if (!msg.containsKey("fileDescriptor")) {
                    message = missingField("fileDescriptor");
                } else if (!msg.containsKey("lastModified")) {
                    message = missingField("lastModified");
                } else if (!msg.containsKey("fileSize")) {
                    message = missingField("fileSize");
                } else {
                    //TODO add test to make sure lastModified and fileSize can be parsed as longs
                    command = Commands.FILE_CREATE_RESPONSE;
                    if (!fileSystemManager.isSafePathName(msg.getString("pathName"))) {
                        message = "unsafe pathname";
                    } else if (!fileSystemManager.fileNameExists(msg.getString("pathName"))) {
                        message = "pathname already exists";
                    }
                    // try to create file loader
                    else {
                        boolean loaderCreated;
                        try {
                            loaderCreated = fileSystemManager.createFileLoader(msg.getString("pathName"), msg.getString("md5"),
                                    msg.getLong("fileSize"), msg.getLong("lastModified"));
                        } catch (NoSuchAlgorithmException e) {
                            loaderCreated = false;
                        } catch (IOException e) {
                            loaderCreated = false;
                        }

                        if (loaderCreated) {
                            message = "file loader ready";
                            success = true;

                            // create a FILE_BYTES_REQUEST


                            Document newMsg = newFileBytesRequest(Document.parse(msg.getString("fileDescriptor")),
                                    msg.getString("pathName"),0);
                            replyMsgs.add(newMsg);

                        } else {
                            message = "file loader not ready";
                            success = false;
                        }


                    }
                }
                Document newReplyMsg = new Document();
                newReplyMsg.append("command", command);
                if (command.equals(Commands.FILE_CREATE_RESPONSE)) {
                    newReplyMsg.append("fileDescriptor",
                            Document.parse(msg.getString("fileDescriptor")));
                    newReplyMsg.append("pathName", msg.getString("pathName"));
                    newReplyMsg.append("message", message);
                    newReplyMsg.append("status", success.toString());
                }
                replyMsgs.add(0, newReplyMsg);

                break;
            }


            case Commands.FILE_BYTES_REQUEST: {

                String message, command;
                Boolean success = false;

                String content = null;

                Document newReplyMsg = new Document();

                //TODO check if longs are casting as longs

                if (!msg.containsKey("md5")) {
                    message = missingField("md5");
                } else if (!msg.containsKey("position")) {
                    message = missingField("position");
                } else if (!msg.containsKey("length")) {
                    message = missingField("length");
                } else {
                    String md5 = msg.getString("md5");
                    long position = msg.getLong("position");
                    long length = msg.getLong("length");

                    ByteBuffer contentBB;
                    try {
                        contentBB = fileSystemManager.readFile(md5, position, length);
                    } catch (IOException e) {
                        contentBB = null;
                    } catch (NoSuchAlgorithmException e) {
                        contentBB = null;
                    }

                    if (contentBB != null) {
                        byte[] contentBytes = contentBB.array();
                        content = Base64.getEncoder().encodeToString(contentBytes);
                        message = "successful read";
                        success = true;

                        replyMsgs.add(newReplyMsg);
                    } else {
                        message = "unsuccessful read";
                        success = false;
                    }
                }


                newReplyMsg.append("command", Commands.FILE_BYTES_RESPONSE);
                newReplyMsg.append("fileDescriptor",
                        Document.parse(msg.getString("fileDescriptor")));
                newReplyMsg.append("pathName", msg.getString("pathName"));
                newReplyMsg.append("position", msg.getLong("position"));
                newReplyMsg.append("length", msg.getLong("length"));

                if (content != null) {
                    newReplyMsg.append("content", content);
                }
                newReplyMsg.append("message", message);
                newReplyMsg.append("status", success.toString());


                replyMsgs.add(newReplyMsg);

            break;
            }
        }

        return replyMsgs;
    }

    private String missingField(String field) {
        return "message must contain a " + field + " field as string";
    }

    public ArrayList<Document> handleResponse(Document msg) {

        ArrayList<Document> replyMsgs = new ArrayList<Document>();


        switch(msg.getString("command")) {

            case Commands.FILE_BYTES_RESPONSE: {

                String message;

                if (!msg.containsKey("pathName")) {
                    message = missingField("pathName");
                } else if (!msg.containsKey("content")) {
                    message = missingField("content");
                } else if (!msg.containsKey("position")) {
                    message = missingField("position");
                } else if (!msg.containsKey("fileSize")) {
                    message = missingField("fileSize");
                }  else if (!msg.containsKey("length")) {
                    message = missingField("length");
                }  else {

                    String pathName = msg.getString("pathName");
                    String content = msg.getString("content");
                    long position = msg.getLong("position");
                    long fileSize = msg.getLong("fileSize");
                    long length = msg.getLong("length");

                    //TODO make sure that length < blockSize

                    byte[] contentBytes = Base64.getDecoder().decode(content);
                    ByteBuffer contentBB = null;
                    contentBB.put(contentBytes);

                    try {
                        if (fileSystemManager.writeFile(pathName, contentBB, position)) {

                            try {
                                if (position < fileSize || !fileSystemManager.checkWriteComplete(pathName)) {

                                } else {
                                    Document newMsg = newFileBytesRequest(Document.parse(msg.getString("fileDescriptor")),
                                            msg.getString("pathName"),position + length);
                                    replyMsgs.add(newMsg);
                                }
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        }

        return replyMsgs;
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

}
