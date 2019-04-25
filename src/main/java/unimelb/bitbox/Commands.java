package unimelb.bitbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commands {
    // list of protocols
    public final static String INVALID_PROTOCOL = "INVALID_PROTOCOL";
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
    public final static String FILE_DESCRIPTOR = "fileDescriptor";
    public final static String MD5 = "md5";
    public final static String LAST_MODIFIED = "lastModified";
    public final static String FILE_SIZE = "fileSize";
    public final static String PATH_NAME = "pathName";
    public final static String MESSAGE = "message";
    public final static String STATUS = "status";

    public final static String[] fileCreateReqFields = {
            COMMAND,
            FILE_DESCRIPTOR,
            PATH_NAME
    };

    public final static String[] fileCreateResFields = {
            COMMAND,
            FILE_DESCRIPTOR,
            PATH_NAME,
            MESSAGE,
            STATUS
    };

    public final static String[] requests = {
            FILE_CREATE_REQUEST,
            FILE_DELETE_REQUEST,
            FILE_MODIFY_REQUEST,
            DIRECTORY_CREATE_REQUEST,
            DIRECTORY_DELETE_REQUEST,
            FILE_BYTES_REQUEST
    };

    public final static String[] responses = {
            FILE_CREATE_RESPONSE,
            FILE_DELETE_RESPONSE,
            FILE_MODIFY_RESPONSE,
            DIRECTORY_CREATE_RESPONSE,
            DIRECTORY_DELETE_RESPONSE,
            FILE_BYTES_RESPONSE
    };

    public final static HashMap validFields = new HashMap();
    static {
        validFields.put(FILE_CREATE_REQUEST, fileCreateReqFields);
        validFields.put(FILE_CREATE_RESPONSE, fileCreateResFields);

    }

    public static boolean isRequest(String command) {
        for (int i = 0; i < requests.length; i++) {
            if (requests[i].equals(command)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isResponse(String command) {
        for (int i = 0; i < responses.length; i++) {
            if (responses[i].equals(command)) {
                return true;
            }
        }
        return true;
    }
}
