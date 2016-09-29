package valera.offline;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import valera.offline.io.ValeraLogReader;

class IOIndexEntry {
	int connId;
	String operation;
	String uri;
	
	IOIndexEntry(int connId, String op, String uri) {
		this.connId = connId;
		this.operation = op;
		this.uri = uri;
	}
}

public class Test {
	
	public HashMap<String, ArrayList<IOIndexEntry>> readIndex(String path) throws IOException, FileNotFoundException, IOException, ClassNotFoundException {
		HashMap<String, ArrayList<IOIndexEntry>> hash = new HashMap<String, ArrayList<IOIndexEntry>>();
		
		ValeraLogReader reader = new ValeraLogReader(new FileInputStream(path));
		int size = reader.readInt();
		System.out.println("size=" + size);
		for (int i = 0; i < size; i++) {
			int connId = reader.readInt();
			String op  = reader.readString();
			String uri = reader.readString();
			
			System.out.println(String.format("DEBUG: %d %s %s", connId, op, uri));
			
			IOIndexEntry entry = new IOIndexEntry(connId, op, uri);
			String key = op + " " + uri;
			ArrayList<IOIndexEntry> al = hash.get(key);
			if (al == null) {
				al = new ArrayList<IOIndexEntry>();
				hash.put(key, al);
			}
			al.add(entry);
		}
		reader.close();
		return hash;
	}
	
	public void tryReadIO(HashMap<String, ArrayList<IOIndexEntry>> hash) throws StreamCorruptedException, FileNotFoundException, IOException {
		for (Map.Entry<String, ArrayList<IOIndexEntry>> entry : hash.entrySet()) {
			ArrayList<IOIndexEntry> al = entry.getValue();
			for (IOIndexEntry iie : al) {
				int connId = iie.connId;
				FileInputStream fis = new FileInputStream("/home/yhu009/Desktop/io.bin." + connId);
				ValeraLogReader reader = new ValeraLogReader(fis);
				System.out.println(String.format("conn[%d] tag=%d", connId, reader.readInt()));
				reader.close();
			}
		}
	}

	public static void main(String[] arg) throws Exception {
		Test test = new Test();
		HashMap<String, ArrayList<IOIndexEntry>> hash = test.readIndex("/home/yhu009/Desktop/io.index");
		test.tryReadIO(hash);
	}
}
