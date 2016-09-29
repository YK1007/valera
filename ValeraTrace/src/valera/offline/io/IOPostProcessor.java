package valera.offline.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


abstract class IOEntry {
	int tag;

	IOEntry(int tag) {
		this.tag = tag;
	}
}

abstract class NetworkEntry extends IOEntry {
	int connId;
	
	NetworkEntry(int tag, int connId) {
		super(tag);
		this.connId = connId;
	}
	
	abstract void write(ValeraLogWriter writer);
}

class PlainSocketRead extends NetworkEntry {
	long time;
	Exception exception;
	int nread;
	byte[] buffer;
	
	PlainSocketRead(int tag, int connId, long time, Exception e, int nread, byte[] buffer) {
		super(tag, connId);
		this.time = time;
		this.exception = e;
		this.nread = nread;
		this.buffer = buffer;
	}
	
	void write(ValeraLogWriter writer) {
		try {
			writer.writeInt(tag);
			writer.writeInt(connId);
			writer.writeLong(time);
			if (exception != null) {
				writer.writeInt(1);
				writer.writeException(exception);
			} else {
				writer.writeInt(0);
				writer.writeInt(nread);
				if (nread > 0) {
					writer.writeByteArray(buffer, 0, buffer.length);
				}
			}
			writer.flush();
		} catch (Exception e) {
			ValeraUtil.valeraAbort(e);
		}
	}
}

class PlainSocketWrite extends NetworkEntry {
	long time;
	Exception exception;
	byte[] buffer;
	
	PlainSocketWrite(int tag, int connId, long time, Exception e, byte[] buffer) {
		super(tag, connId);
		this.time = time;
		this.exception = e;
		this.buffer = buffer;
	}
	
	void write(ValeraLogWriter writer) {
		try {
			writer.writeInt(tag);
			writer.writeInt(connId);
			writer.writeLong(time);
			if (exception != null) {
				writer.writeInt(1);
				writer.writeException(exception);
			} else {
				writer.writeInt(0);
				writer.writeByteArray(buffer, 0, buffer.length);
			}
			writer.flush();
		} catch (Exception e) {
			ValeraUtil.valeraAbort(e);
		}
	}
}

class SSLSocketRead extends NetworkEntry {
	long time;
	Exception exception;
	int nread;
	byte[] buffer;
	
	SSLSocketRead(int tag, int connId, long time, Exception e, int nread, byte[] buffer) {
		super(tag, connId);
		this.time = time;
		this.exception = e;
		this.nread = nread;
		this.buffer = buffer;
	}
	
	void write(ValeraLogWriter writer) {
		try {
			writer.writeInt(tag);
			writer.writeInt(connId);
			writer.writeLong(time);
			if (exception != null) {
				writer.writeInt(1);
				writer.writeException(exception);
			} else {
				writer.writeInt(0);
				writer.writeInt(nread);
				if (nread > 0) {
					writer.writeByteArray(buffer, 0, buffer.length);
				}
			}
			writer.flush();
		} catch (Exception e) {
			ValeraUtil.valeraAbort(e);
		}
	}
}

class SSLSocketWrite extends NetworkEntry {
	long time;
	Exception exception;
	byte[] buffer;
	
	SSLSocketWrite(int tag, int connId, long time, Exception e, byte[] buffer) {
		super(tag, connId);
		this.time = time;
		this.exception = e;
		this.buffer = buffer;
	}
	
	void write(ValeraLogWriter writer) {
		try {
			writer.writeInt(tag);
			writer.writeInt(connId);
			writer.writeLong(time);
			if (exception != null) {
				writer.writeInt(1);
				writer.writeException(exception);
			} else {
				writer.writeInt(0);
				writer.writeByteArray(buffer, 0, buffer.length);
			}
			writer.flush();
		} catch (Exception e) {
			ValeraUtil.valeraAbort(e);
		}
	}
}

class NetworkConn {
	int connId;
	String method;
	String uri;
	ArrayList<NetworkEntry> data;
	int numRequest, numResponse;
	
	NetworkConn(int connId, String method, String uri) {
		this.connId = connId;
		this.method = method;
		this.uri = uri;
		data = new ArrayList<NetworkEntry>();
		numRequest = numResponse = 0;
	}
	
	void addEntry(NetworkEntry entry) {
		data.add(entry);
		if (entry instanceof PlainSocketWrite || entry instanceof SSLSocketWrite)
			numRequest++;
		if (entry instanceof PlainSocketRead || entry instanceof SSLSocketRead)
			numResponse++;
	}
	
	boolean checkValidity() {
		ValeraUtil.valeraAssert(connId > 0, "Invalid connection ID " + connId);
		ValeraUtil.valeraAssert(method != null, "Null method for this connection " + connId);
		ValeraUtil.valeraAssert(uri != null, "Null uri for this connection " + connId);
		
		System.out.println(String.format("Conn[%d] %s %s %d", connId, method, uri, data.size()));
		
		//ValeraUtil.valeraAssert(data.size() > 0, "Empty data for this connection " + connId);
		// Find some connections do not have data... let them pass.
		if (data.size() <= 0) {
			return true;
		}
		
		if (data.get(0) instanceof PlainSocketWrite) {
			int flag = 0;
			for (NetworkEntry entry : data) {
				if (entry instanceof PlainSocketWrite) {
					ValeraUtil.valeraAssert(flag == 0 | flag == 1, "No read should happen before write, plain socket connId=" + connId);
					//System.out.println(String.format("Conn[%d] plain request: %s", connId, new String(((PlainSocketWrite) entry).buffer)));
					flag = 1;
				} else if (entry instanceof PlainSocketRead) {
					ValeraUtil.valeraAssert(flag == 1 | flag == 2, "Read can only happen after write, plain socket connId=" + connId);
					if (flag == 1) {
						// First read, print the header.
						//System.out.println(String.format("Conn[%d] plain response: %s", connId, new String(((PlainSocketRead) entry).buffer)));
					}
					flag = 2;
				} else {
					ValeraUtil.valeraAssert(false, "PlainSocket connection " + connId + " contains non-plain socket data " + entry.toString());
				}
			}
		} else if (data.get(0) instanceof SSLSocketWrite) {
			int flag = 0;
			for (NetworkEntry entry : data) {
				if (entry instanceof SSLSocketWrite) {
					ValeraUtil.valeraAssert(flag == 0 | flag == 1, "No read should happen before write, ssl socket connId=" + connId);
					//System.out.println(String.format("Conn[%d] ssl request: %s", connId, new String(((SSLSocketWrite) entry).buffer)));
					flag = 1;
				} else if (entry instanceof SSLSocketRead) {
					ValeraUtil.valeraAssert(flag == 1 | flag == 2, "Read can only happen after write, ssl socket connId=" + connId);
					if (flag == 1) {
						// First read, print the header.
						//System.out.println(String.format("Conn[%d] ssl response: %s", connId, new String(((SSLSocketRead) entry).buffer)));
					}
					flag = 2;
				} else {
					ValeraUtil.valeraAssert(false, "SSLSocket connection " + connId + " contains non-ssl socket data " + entry.toString());
				}
			}
		} else {
			ValeraUtil.valeraAssert(false, "First data should be (Plain|SSL)SocketWrite " + data.get(0).toString());
		}
		
		return true;
	}
}

public class IOPostProcessor {
	
	public static final int PRINT_LEVEL_NONE = 0;
	public static final int PRINT_LEVEL_NETWORK_EVENT = 1;
	public static final int PRINT_LEVEL_NETWORK_CONTENT = 2;

	private File mDir;
	private ValeraLogReader mReader;
	private HashMap<Integer, NetworkConn> mHash;
	
	private final static int IO_PLAINSOCKET_READ	= 1;
	private final static int IO_PLAINSOCKET_WRITE	= 2;
	private final static int IO_SSL_READ			= 3;
	private final static int IO_SSL_WRITE			= 4;
	private final static int IO_CONNECTION			= 5;
	
	private final static int CONN_TYPE_HTTP = 1;
	private final static int CONN_TYPE_HTTPS = 2;

	public IOPostProcessor(String inputFile) throws Exception {
		File file = new File(inputFile);
		if (!file.exists() || !file.isFile())
			throw new IllegalArgumentException("Illegal input file " + inputFile);
		mDir = file.getParentFile();
		if (mDir == null)
			throw new IllegalArgumentException("Can not get directory from " + inputFile);
		mReader = new ValeraLogReader(new FileInputStream(file));
		mHash = new HashMap<Integer, NetworkConn>();
	}

	public void parse(int printLevel) {
		int counter[] = new int [10];
		Arrays.fill(counter, 0);
		try {
			while (true) {
				int tag = mReader.readInt();
				long relativeTime = mReader.readLong();
				switch (tag) {
				case IO_PLAINSOCKET_READ:
				{
					int connId = mReader.readInt();
					long time = mReader.readLong();
					int hasException = mReader.readInt();
					Exception exception = null;
					int nread = -1;
					byte[] buffer = null;
					if (hasException != 0)
						exception = mReader.readException();
					else {
						nread = mReader.readInt();
						if (nread > 0)
							buffer = mReader.readByteArray();
					}
					
					NetworkConn nc = mHash.get(connId);
					ValeraUtil.valeraAssert(nc != null, "Conn " + connId + " does not exist.");
					nc.addEntry(new PlainSocketRead(IO_PLAINSOCKET_READ, connId, time, exception, nread, buffer));
					counter[IO_PLAINSOCKET_READ]++;
					
					switch (printLevel) {
					case PRINT_LEVEL_NETWORK_CONTENT:
						String content = buffer == null ? exception.toString() : new String(buffer);
						System.out.println(String.format("### PLAINSOCKET_READ: time=%d connId=%d content=%s", 
								relativeTime, connId, content));
						System.out.println();
						break;
					case PRINT_LEVEL_NETWORK_EVENT:
						System.out.println(String.format("### PLAINSOCKET_READ: time=%d connId=%d",
								relativeTime, connId));
						System.out.println();
						break;
					}
				}
					break;
				case IO_PLAINSOCKET_WRITE:
				{
					int connId = mReader.readInt();
					long time = mReader.readLong();
					int hasException = mReader.readInt();
					Exception exception = null;
					byte[] buffer = null;
					if (hasException != 0)
						exception = mReader.readException();
					else
						buffer = mReader.readByteArray();
					
					NetworkConn nc = mHash.get(connId);
					ValeraUtil.valeraAssert(nc != null, "Conn " + connId + " does not exist.");
					nc.addEntry(new PlainSocketWrite(IO_PLAINSOCKET_WRITE, connId, time, exception, buffer));
					counter[IO_PLAINSOCKET_WRITE]++;
					
					switch (printLevel) {
					case PRINT_LEVEL_NETWORK_CONTENT:
						String content = buffer == null ? exception.toString() : new String(buffer);
						System.out.println(String.format("### PLAINSOCKET_WRITE: time=%d connId=%d content=%s", 
								relativeTime, connId, content));
						System.out.println();
						break;
					case PRINT_LEVEL_NETWORK_EVENT:
						System.out.println(String.format("### PLAINSOCKET_WRITE: time=%d connId=%d",
								relativeTime, connId));
						System.out.println();
						break;
					}
				}
					break;
				case IO_SSL_READ:
				{
					int connId = mReader.readInt();
					long time = mReader.readLong();
					int hasException = mReader.readInt();
					Exception exception = null;
					int nread = -1;
					byte[] buffer = null;
					if (hasException != 0)
						exception = mReader.readException();
					else {
						nread = mReader.readInt();
						if (nread > 0)
							buffer = mReader.readByteArray();
					}
					
					NetworkConn nc = mHash.get(connId);
					ValeraUtil.valeraAssert(nc != null, "Conn " + connId + " does not exist.");
					nc.addEntry(new SSLSocketRead(IO_SSL_READ, connId, time, exception, nread, buffer));
					counter[IO_SSL_READ]++;
					
					switch (printLevel) {
					case PRINT_LEVEL_NETWORK_CONTENT:
						String content = buffer == null ? exception.toString() : new String(buffer);
						System.out.println(String.format("### SSLSOCKET_READ: time=%d connId=%d content=%s", 
								relativeTime, connId, content));
						System.out.println();
						break;
					case PRINT_LEVEL_NETWORK_EVENT:
						System.out.println(String.format("### SSLSOCKET_READ: time=%d connId=%d",
								relativeTime, connId));
						System.out.println();
						break;
					}
				}
					break;
				case IO_SSL_WRITE:
				{
					int connId = mReader.readInt();
					long time = mReader.readLong();
					int hasException = mReader.readInt();
					Exception exception = null;
					byte[] buffer = null;
					if (hasException != 0)
						exception = mReader.readException();
					else
						buffer = mReader.readByteArray();
					
					NetworkConn nc = mHash.get(connId);
					ValeraUtil.valeraAssert(nc != null, "Conn " + connId + " does not exist.");
					nc.addEntry(new SSLSocketWrite(IO_SSL_WRITE, connId, time, exception, buffer));
					counter[IO_SSL_WRITE]++;
					
					switch (printLevel) {
					case PRINT_LEVEL_NETWORK_CONTENT:
						String content = buffer == null ? exception.toString() : new String(buffer);
						System.out.println(String.format("### SSLSOCKET_WRITE: time=%d connId=%d content=%s", 
								relativeTime, connId, content));
						System.out.println();
						break;
					case PRINT_LEVEL_NETWORK_EVENT:
						System.out.println(String.format("### SSLSOCKET_WRITE: time=%d connId=%d",
								relativeTime, connId));
						System.out.println();
						break;
					}
				}
					break;
				case IO_CONNECTION:
				{
					int connId = mReader.readInt();
					String method = mReader.readString().trim();
					String uri = mReader.readString().trim();
					NetworkConn nc = mHash.get(connId);
					ValeraUtil.valeraAssert(nc == null, "Duplicate connection? " + connId);
					nc = new NetworkConn(connId, method, uri);
					mHash.put(connId, nc);
					
					switch (printLevel) {
					case PRINT_LEVEL_NETWORK_CONTENT:
						System.out.println(String.format("### NEW_CONNECTION: time=%d connId=%d method=%s uri=%s", 
								relativeTime, connId, method, uri));
						System.out.println();
						break;
					case PRINT_LEVEL_NETWORK_EVENT:
						System.out.println(String.format("### NEW_CONNECTION: time=%d connId=%d", 
								relativeTime, connId));
						System.out.println();
						break;
					}
				}
					break;
				}
			}
		} catch (EOFException eof) {
			
		} catch (Exception e) {
			ValeraUtil.valeraAbort(e);
		}
		
		System.out.println(String.format("Finish parsing. plain read=%d write=%d. ssl read=%d write=%d",
				counter[IO_PLAINSOCKET_READ], counter[IO_PLAINSOCKET_WRITE], 
				counter[IO_SSL_READ], counter[IO_SSL_WRITE]));
	}
	
	public void check() {
		boolean result = true;
		for (Map.Entry<Integer, NetworkConn> entry : mHash.entrySet()) {
			NetworkConn nc = entry.getValue();
			result &= nc.checkValidity();
		}
		if (result)
			System.out.println("Validity checking pass!");
		else
			System.out.println("Validity checking fail!");
	}
	
	private void writeIndex(ValeraLogWriter writer, NetworkConn conn) throws IOException {
		int connId = conn.connId;
		int type = 0; // 1 - plain socket; 2 - ssl socket;
		String method = conn.method;
		String uri = conn.uri;
		int numRequest = conn.numRequest;
		int numResponse = conn.numResponse;
		NetworkEntry entry = conn.data.get(0);
		
		if (!method.equals("GET") && !method.equals("POST")) {
			ValeraUtil.valeraAssert(false, "We only support HTTP(S) GET/POST right now. " + method);
		}
		if (entry instanceof PlainSocketWrite) {
			type = CONN_TYPE_HTTP;
		} else if (entry instanceof SSLSocketWrite) {
			type = CONN_TYPE_HTTPS;
		} else {
			ValeraUtil.valeraAssert(false, "Write index failed. First packet should be [Plain|SSL]SocketWrite");
		}
		
		writer.writeInt(connId);
		writer.writeString(method);
		writer.writeString(uri);
		writer.writeInt(type);
		writer.writeInt(numRequest);
		writer.writeInt(numResponse);
		writer.flush();
		System.out.print("index " + connId + " " + method + " " + uri);
	}
	
	public void rewrite() throws Exception {
		String indexFileName = mDir.getAbsolutePath() + File.separator + "io.index";
		ValeraLogWriter indexWriter = new ValeraLogWriter(new FileOutputStream(indexFileName));
		
		// Write Index file
		int size = mHash.size();
		indexWriter.writeInt(size);
		for (int i = 1; i <= size; i++) {
			int connId = i;
			NetworkConn nc = mHash.get(i);
			System.out.print("Rewrite conn[" + connId + "] ... ");
			
			writeIndex(indexWriter, nc);
			
			String ioFilename = mDir.getAbsolutePath() + File.separator + "io.bin" + "." + connId;
			ValeraLogWriter ioWriter = new ValeraLogWriter(new FileOutputStream(ioFilename));
			for (NetworkEntry ne : nc.data) {
				ne.write(ioWriter);
			}
			ioWriter.close();
			System.out.println(" ... Done.");
		}
		indexWriter.close();
	}
	
	public byte[] getConnResponseData(int connId) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		NetworkConn nc = mHash.get(connId);
		for (NetworkEntry entry : nc.data) {
			if (entry instanceof PlainSocketRead) {
				PlainSocketRead plainread = (PlainSocketRead) entry;
				if (plainread.buffer != null)
					bos.write(plainread.buffer);
			} else if (entry instanceof SSLSocketRead) {
				SSLSocketRead sslread = (SSLSocketRead) entry;
				if (sslread.buffer != null)
					bos.write(sslread.buffer);
			}
		}
		
		return bos.toByteArray();
	}

}
