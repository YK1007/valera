package valera.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.URI;
import java.util.zip.GZIPInputStream;


public class OfflineHttpParser {
	
	public static final int HTTP_CONTINUE = 100;
	
	private ResponseHeaders responseHeaders;
	private int httpMinorVersion = 1; // Assume HTTP/1.1
	
	
	private static String readAsciiLine(InputStream in) throws IOException {
        // TODO: support UTF-8 here instead

        StringBuilder result = new StringBuilder(80);
        while (true) {
            int c = in.read();
            if (c == -1) {
                throw new EOFException();
            } else if (c == '\n') {
                break;
            }

            result.append((char) c);
        }
        int length = result.length();
        if (length > 0 && result.charAt(length - 1) == '\r') {
            result.setLength(length - 1);
        }
        return result.toString();
    }
	
	private void readHeaders(RawHeaders headers, InputStream in) throws IOException {
        // parse the result headers until the first blank line
        String line;
        while (!(line = OfflineHttpParser.readAsciiLine(in)).isEmpty()) {
            headers.addLine(line);
        }

        //CookieHandler cookieHandler = CookieHandler.getDefault();
        //if (cookieHandler != null) {
        //    cookieHandler.put(uri, headers.toMultimap());
        //}
    }
	
	private void setResponse(ResponseHeaders headers) throws IOException {
        this.responseHeaders = headers;
        this.httpMinorVersion = responseHeaders.getHeaders().getHttpMinorVersion();
    }
	
	private void readResponseHeaders(URI uri, InputStream in) throws IOException {
        RawHeaders headers;
        do {
            headers = new RawHeaders();
            headers.setStatusLine(OfflineHttpParser.readAsciiLine(in));
            readHeaders(headers, in);
        } while (headers.getResponseCode() == HTTP_CONTINUE);
        setResponse(new ResponseHeaders(uri, headers));
    }
	
	/**
     * Returns true if the response must have a (possibly 0-length) body.
     * See RFC 2616 section 4.3.
     */
    public final boolean hasResponseBody() {
        int responseCode = responseHeaders.getHeaders().getResponseCode();

        // HEAD requests never yield a body regardless of the response headers.
        //if (method == HEAD) {
        //    return false;
        //}

        //if (method != CONNECT
        //        && (responseCode < HTTP_CONTINUE || responseCode >= 200)
        //        && responseCode != HttpURLConnectionImpl.HTTP_NO_CONTENT
        //        && responseCode != HttpURLConnectionImpl.HTTP_NOT_MODIFIED) {
        //    return true;
        //}

        /*
         * If the Content-Length or Transfer-Encoding headers disagree with the
         * response code, the response is malformed. For best compatibility, we
         * honor the headers.
         */
        if (responseHeaders.getContentLength() != -1 || responseHeaders.isChunked()) {
            return true;
        }

        return false;
    }
	
	private InputStream getTransferStream(InputStream in) throws IOException {
        if (!hasResponseBody()) {
            return new FixedLengthInputStream(in, null, 0);
        }

        if (responseHeaders.isChunked()) {
            return new ChunkedInputStream(in, null);
        }

        if (responseHeaders.getContentLength() != -1) {
            return new FixedLengthInputStream(in, null,
                    responseHeaders.getContentLength());
        }

        /*
         * Wrap the input stream from the HttpConnection (rather than
         * just returning "socketIn" directly here), so that we can control
         * its use after the reference escapes.
         */
        return new UnknownLengthHttpInputStream(in, null);
    }
	
	private InputStream initContentStream(InputStream transferStream) throws IOException {
		InputStream responseBodyIn;
		
        if (responseHeaders.isContentEncodingGzip()) {
            /*
             * If the response was transparently gzipped, remove the gzip header field
             * so clients don't double decompress. http://b/3009828
             *
             * Also remove the Content-Length in this case because it contains the length
             * of the gzipped response. This isn't terribly useful and is dangerous because
             * clients can query the content length, but not the content encoding.
             */
            responseHeaders.stripContentEncoding();
            responseHeaders.stripContentLength();
            responseBodyIn = new GZIPInputStream(transferStream);
        } else {
            responseBodyIn = transferStream;
        }
        
        return responseBodyIn;
    }
	
	private void outputStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = in.read(buffer, 0, buffer.length)) != -1) {
			out.write(buffer,0,len);
		}
	}
	
	public void parse(byte[] data) throws IOException {
		URI uri = null;
		InputStream in = new ByteArrayInputStream(data);
		OutputStream out = new FileOutputStream(new File("conn.data"));
		
		readResponseHeaders(uri, in);
		
		InputStream responseBodyIn = initContentStream(getTransferStream(in));
		
		outputStream(responseBodyIn, out);
	}

}
