package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
			connection.getPort());

		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

			List<String> http = new ArrayList<>();
			String line = bufferedReader.readLine();
			if (line == null) {
				return;
			}

			String httpMethod = HttpRequestUtils.getHttpMethod(line);
			String url = HttpRequestUtils.getUrl(line);
			Map<String, String> headers = new HashMap<>();
			while (!"".equals(line)) {
			    log.debug("header : {}", line);
			    line = bufferedReader.readLine();
                String[] headerTokens = line.split(": ");
                if (headerTokens.length == 2)
                    headers.put(headerTokens[0], headerTokens[1]);
            }

            log.debug("Content-Length : {}", headers.get("Content-Length"));

			if ("GET".equals(httpMethod)) {
			    if (url.equals("/") || url.equals("")) {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp/index.html").toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else if (url.startsWith("/user/list")) {
			        boolean isLogined = Boolean.parseBoolean(HttpRequestUtils.parseCookies(headers.get("Cookie")).get("logined"));
                    DataOutputStream dos = new DataOutputStream(out);
                    if (isLogined) {
                        byte[] body = Files.readAllBytes(new File("./webapp" + "/user/list.html").toPath());
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    } else {
                        response302Header(dos, "/user/login.html");
                    }
                } else if (url.endsWith(".css")) {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                    response200CssHeader(dos, body.length);
                    responseBody(dos, body);
                } else {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }
            } else if ("POST".equals(httpMethod)) {
                if (url.startsWith("/create")) {
                    //			    int index = url.indexOf("?");
                    //			    String requestPath = url.substring(0, index);
                    //			    String queryString = url.substring(index + 1);
                    String requestBody = IOUtils.readData(bufferedReader, Integer.parseInt(headers.get("Content-Length")));
                    log.debug("requestBody : {}", requestBody);
                    Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
                    User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
                    log.debug("User : {}", user);
                    DataBase.addUser(user);

                    DataOutputStream dos = new DataOutputStream(out);

                    response302Header(dos, "index.html");
                } else if (url.startsWith("/user/login")) {
                    String requestBody = IOUtils.readData(bufferedReader, Integer.parseInt(headers.get("Content-Length")));
                    log.debug("requestBody : {}", requestBody);
                    Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
                    User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
                    log.debug("User : {}", user);

                    User loginUser = DataBase.findUserById(user.getUserId());
                    boolean isLoginSuccess;
                    String redirectPath;
                    if (loginUser != null && loginUser.getPassword().equals(user.getPassword())) {
                        // 로그인 성공
                        isLoginSuccess = true;
                        redirectPath = "/index.html";
                    } else {
                        // 로그인 실패
                        isLoginSuccess = false;
                        redirectPath = "/user/login_failed.html";
                    }

                    DataOutputStream dos = new DataOutputStream(out);
                    responseLoginHeader(dos, redirectPath, isLoginSuccess);
                }

            }
//			String httpMethod = http.get(0).split(" ")[0];
//			if ("GET".equals(httpMethod)) {
//
//				String url = http.get(0).split(" ")[1];
//				log.info("==========================> {}", url);
//
//				int index = url.indexOf("?");
//				if (index > 0) {
//					url = url.substring(0, index);
//
//					String param = url.substring(index + 1);
//
//					Map<String, String> map = HttpRequestUtils.parseQueryString(param);
//					User user = new User(map.get("userId"), map.get("password"), map.get("name"),
//						map.get("email"));
//				}
//				DataOutputStream dos = new DataOutputStream(out);
//				byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
//				response200Header(dos, body.length);
//				responseBody(dos, body);
//			} else if ("POST".equals(httpMethod)) {
//				log.info(IOUtils.readData(bufferedReader, 44));
//				//                DataOutputStream dos = new DataOutputStream(out);
//				//                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
//				//                response200Header(dos, body.length);
//				//                responseBody(dos, body);
//			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

    /**
     * HTTP/1.1 302 Found
     * Location: http://www.iana.org/domains/example/
     * @link https://en.wikipedia.org/wiki/HTTP_302
     * @param dos
     * @param redirectPath
     */
    private void response302Header(DataOutputStream dos, String redirectPath) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location:" + redirectPath + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseLoginHeader(DataOutputStream dos, String redirectPath, boolean isLoginSuccess) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location:" + redirectPath + "\r\n");
//            dos.writeBytes("HTTP/1.1 200 OK \r\n");
//            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Set-Cookie: logined=" + isLoginSuccess + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
