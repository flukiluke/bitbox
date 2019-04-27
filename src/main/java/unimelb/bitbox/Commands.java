package unimelb.bitbox;

import java.util.HashMap;

public class Commands {
    // list of protocol commands
    public final static String INVALID_PROTOCOL = "INVALID_PROTOCOL";
    public final static String CONNECTION_REFUSED = "CONNECTION_REFUSED";

    public final static String HANDSHAKE_REQUEST = "HANDSHAKE_REQUEST";
    public final static String HANDSHAKE_RESPONSE = "HANDSHAKE_RESPONSE";

    public final static String FILE_CREATE_REQUEST = "FILE_CREATE_REQUEST";
    public final static String FILE_DELETE_REQUEST = "FILE_DELETE_REQUEST";
    public final static String FILE_MODIFY_REQUEST = "FILE_MODIFY_REQUEST";
    public final static String DIRECTORY_CREATE_REQUEST = "DIRECTORY_CREATE_REQUEST";
    public final static String DIRECTORY_DELETE_REQUEST = "DIRECTORY_DELETE_REQUEST";
    public final static String FILE_BYTES_REQUEST = "FILE_BYTES_REQUEST";

    public final static String FILE_CREATE_RESPONSE = "FILE_CREATE_RESPONSE";
    public final static String FILE_DELETE_RESPONSE = "FILE_DELETE_RESPONSE";
    public final static String FILE_MODIFY_RESPONSE = "FILE_MODIFY_RESPONSE";
    public final static String DIRECTORY_CREATE_RESPONSE = "DIRECTORY_CREATE_RESPONSE";
    public final static String DIRECTORY_DELETE_RESPONSE = "DIRECTORY_DELETE_RESPONSE";
    public final static String FILE_BYTES_RESPONSE = "FILE_BYTES_RESPONSE";

    // list of fields
    public final static String COMMAND = "command";
    // TODO need to implement checks for particular fields needed within FILE_DESCRIPTOR.
    public final static String FILE_DESCRIPTOR = "fileDescriptor";
    public final static String MD5 = "md5";
    public final static String LAST_MODIFIED = "lastModified";
    public final static String FILE_SIZE = "fileSize";
    public final static String PATH_NAME = "pathName";
    public final static String MESSAGE = "message";
    public final static String STATUS = "status";
    public final static String POSITION = "position";
    public final static String LENGTH = "length";
    public final static String CONTENT = "content";

    //TODO need to check the fields are the appropriate type (string, long, boolean, etc.)

    public final static String[] fileReqFields = {
            COMMAND,
            FILE_DESCRIPTOR,
            PATH_NAME
    };

    public final static String[] fileResFields = {
            COMMAND,
            FILE_DESCRIPTOR,
            PATH_NAME,
            MESSAGE,
            STATUS
    };

    public final static String[] dirReqFields = {
            COMMAND,
            PATH_NAME
    };

    public final static String[] dirResFields = {
            COMMAND,
            PATH_NAME,
            MESSAGE,
            STATUS
    };

    public final static String[] bytesReqFields = {
            COMMAND,
            FILE_DESCRIPTOR,
            PATH_NAME,
            POSITION,
            LENGTH
    };

    public final static String[] bytesResFields = {
            COMMAND,
            FILE_DESCRIPTOR,
            PATH_NAME,
            POSITION,
            LENGTH,
            CONTENT,
            MESSAGE,
            STATUS
    };

    public final static HashMap validFields = new HashMap();
    static {
        validFields.put(FILE_CREATE_REQUEST, fileReqFields);
        validFields.put(FILE_CREATE_RESPONSE, fileResFields);
        validFields.put(FILE_DELETE_REQUEST, fileReqFields);
        validFields.put(FILE_DELETE_RESPONSE, fileResFields);
        validFields.put(FILE_MODIFY_REQUEST, fileReqFields);
        validFields.put(FILE_MODIFY_RESPONSE, fileResFields);
        validFields.put(DIRECTORY_CREATE_REQUEST, dirReqFields);
        validFields.put(DIRECTORY_CREATE_RESPONSE, dirResFields);
        validFields.put(DIRECTORY_DELETE_REQUEST, dirReqFields);
        validFields.put(DIRECTORY_DELETE_RESPONSE, dirResFields);
        validFields.put(FILE_BYTES_REQUEST, bytesReqFields);
        validFields.put(FILE_BYTES_RESPONSE, bytesResFields);
    }
}
