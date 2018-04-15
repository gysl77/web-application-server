package http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {
	private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);
	
	private String method;
	private String path;
	private Map<String, String> headers = new HashMap<String, String>();
	private Map<String, String> parameters;
	
	public HttpRequest(InputStream in) throws Exception {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		String requestLine = bufferedReader.readLine();
		
		method = requestLine.split(" ")[0];
		path = requestLine.split(" ")[1];
		
		headers = parseHeaders(bufferedReader);
		
		parameters = parseParameters(bufferedReader);
	}
	
	private Map<String, String> parseHeaders(BufferedReader bufferedReader) throws IOException{
		Map<String, String> headers = new HashMap<>();
		String line = bufferedReader.readLine();
		while(!"".equals(line)) {
			String[] keyValue = line.split(": ");
			headers.put(keyValue[0], keyValue[1]);
			line = bufferedReader.readLine();
			
			if(line == null)
				break;
		}
		
		return headers;
	}
	
	private Map<String, String> parseParameters(BufferedReader bufferedReader) throws NumberFormatException, IOException{
		String requestBody = "";
		if("GET".equals(method)) {
			int index = path.indexOf("?");
			requestBody = path.substring(index + 1);
			path = path.substring(0, index);
			return HttpRequestUtils.parseQueryString(requestBody);
		}
		if("POST".equals(method)) {
			requestBody = IOUtils.readData(bufferedReader, Integer.parseInt(headers.get("Content-Length")));
			return HttpRequestUtils.parseQueryString(requestBody);
		}
		
		return null;
		
	}
	
	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public String getHeader(String string) throws IOException {
		return headers.get(string);
	}

	public String getParameter(String string) {
		return parameters.get(string);
	}

}
