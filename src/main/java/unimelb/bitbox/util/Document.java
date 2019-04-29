package unimelb.bitbox.util;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.BadMessageException;

/**
 * Helper class for using JSON.
 *
 * Provides append() for building JSON and get*() methods for each type of data
 * to extract it, in general throwing BadMessageException when data is malformed or missing.
 *
 * Adapted from a class provided by the Aaron.
 *
 * @author aaron
 * @author TransfictionRailways
 *
 */
public class Document {
	
	protected JSONObject obj;
	
	public Document(){
		obj=new JSONObject();
	}
	
	public Document(JSONObject obj){
		this.obj = obj;
	}
	
	@SuppressWarnings("unchecked")
	public void append(String key,String val){
		if(val==null){
			obj.put(key, null);
		} else {
			obj.put(key, new String(val));
		}
	}
	
	@SuppressWarnings("unchecked")
	public void append(String key,Document doc){
		obj.put(key, doc.obj);
	}
	
	@SuppressWarnings("unchecked")
	public void append(String key,boolean val){
		obj.put(key, new Boolean(val));
	}
	
	@SuppressWarnings("unchecked")
	public void append(String key,ArrayList<?> val){
		JSONArray list = new JSONArray();
		for(Object o : val){
			if(o instanceof Document){
				list.add(((Document)o).obj);
			} else {
				list.add(o);
			}
		}
		obj.put(key,list);
	}
	
	@SuppressWarnings("unchecked")
	public void append(String key,long val){
		obj.put(key, new Long(val));
	}
	
	@SuppressWarnings("unchecked")
	public void append(String key,int val){
		obj.put(key, new Integer(val));
	}
	
	public String toJson(){
		return obj.toJSONString();
	}

	public String toString() {
	    return toJson();
    }
	
	public static Document parse(String json) throws BadMessageException {
		JSONParser parser = new JSONParser();
		try {
			JSONObject obj = (JSONObject) parser.parse(json);
			return new Document(obj);
		} catch (ParseException | ClassCastException e) {
            throw new BadMessageException("Malformed JSON");
        }
	}
	
	public boolean containsKey(String key){
		return obj.containsKey(key);
	}
	
	public String getString(String key) throws BadMessageException {
	    String val;
	    try {
            val = (String) obj.get(key);
            if (val == null) throw new ClassCastException();
        }
	    catch (ClassCastException e) {
            throw new BadMessageException("No string field " + key);
        }
	    return val;
	}

	public Document getDocument(String key) throws BadMessageException {
	    JSONObject subObj;
	    try {
	        subObj = (JSONObject) obj.get(key);
	        if (subObj == null) throw new ClassCastException();
        }
	    catch (ClassCastException e) {
	        throw new BadMessageException("No object field " + key);
        }
	    return new Document(subObj);
    }

    public long getLong(String key) throws BadMessageException {
	    Long val;
	    try {
	        val = (Long) obj.get(key);
	        if (val == null) throw new ClassCastException();
        }
	    catch (ClassCastException e) {
            throw new BadMessageException("No long integer field " + key);
        }
	    return val;
    }

    public boolean getBoolean(String key) throws BadMessageException {
	    Boolean val;
        try {
            val = (Boolean) obj.get(key);
            if (val == null) throw new ClassCastException();
        }
        catch (ClassCastException e) {
            throw new BadMessageException("No boolean field " + key);
        }
        return val;
    }

    public List<Document> getListOfDocuments(String key) throws BadMessageException {
	    JSONArray array;
	    ArrayList<Document> list = new ArrayList<>();
	    try {
	        array = (JSONArray) obj.get(key);
	        for (Object o : array) {
	            list.add(new Document((JSONObject) o));
            }
        }
	    catch (ClassCastException e) {
	        throw new BadMessageException("No array of objects field " + key);
        }
	    return list;
    }
}
