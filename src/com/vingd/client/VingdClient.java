package com.vingd.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import sun.misc.BASE64Encoder;

import com.google.gson.Gson;

import com.vingd.client.exception.VingdOperationException;
import com.vingd.client.exception.VingdTransportException;


public class VingdClient {
	public static final String userAgent = "vingd-api-java/0.2";

	// production/default Vingd endpoint and Vingd user frontend base
	public static final String productionEndpointURL = "https://api.vingd.com/broker/v1";
	public static final String productionFrontendURL = "https://www.vingd.com";

	// sandbox/testing Vingd endpoint and Vingd user frontend base
	public static final String sandboxEndpointURL = "https://api.vingd.com/sandbox/broker/v1";
	public static final String sandboxFrontendURL = "https://www.sandbox.vingd.com";

	// default order lifespan: 15min
	public static final long defaultOrderExpiresMilliseconds = 15 * 60 * 1000;

	// default voucher lifespan: 1 month (i.e. 31 day)
	public static final long defaultVoucherExpiresMilliseconds = 31 * 24 * 60 * 60 * 1000;

	// connection parameters
	private String username;
	private String pwhash;
	private String backendURL;
	private String frontendURL;

	public VingdClient(String username, String pwhash, String backendURL, String frontendURL) {
		this.username = username;
		this.pwhash = pwhash;
		this.backendURL = backendURL;
		this.frontendURL = frontendURL;
	}

	public VingdClient(String username, String pwhash) {
		this(username, pwhash, VingdClient.productionEndpointURL, VingdClient.productionFrontendURL);
	}

	private String jsonStringify(Object obj) {
		return new Gson().toJson(obj);
	}

	private Object jsonParseMap(String json) {
		return new Gson().fromJson(json, HashMap.class);
	}

	/**
	 * Calculates SHA1 hash of a string `buffer` and returns hex digest.
	 * 
	 * @param buffer  message
	 * @return  SHA1(message).hexdigest()
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	public static String SHA1(String buffer) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		byte[] bytes = buffer.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		bytes = md.digest(bytes);
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			hex.append(String.format("%02x", bytes[i]));
		}
		return hex.toString();
	}

	/**
	 * Converts local `date` to ISO8601 format with UTC timezone.
	 * 
	 * @param date  local date/time
	 * @return ISO8601  representation of local date in UTC timezone
	 */
	public static String localDateToISO8601(Date date) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		return df.format(date);
	}

	@SuppressWarnings("unchecked")
	public Object request(String method, String resource, Object parameters) throws VingdTransportException, VingdOperationException, IOException {
		URL url = new URL(backendURL + resource);
		HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
		
		method = method.toUpperCase();
		if (method.equals("GET") || method.equals("POST") || method.equals("PUT")) {
			conn.setRequestMethod(method);
		} else {
			throw new VingdTransportException("Unsupported HTTP method.", "Request");
		}
		
		BASE64Encoder encoder = new BASE64Encoder();
		String credentials = encoder.encode((username + ":" + pwhash).getBytes("ASCII"));
		conn.setRequestProperty("Authorization", "Basic " + credentials);
		conn.setRequestProperty("User-Agent", userAgent);
		
		if (parameters != null) {
			conn.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			String parametersString = jsonStringify(parameters);
			writer.write(parametersString);
			writer.close();
		}

		int statusCode = 0;
		try {
			conn.connect();
			statusCode = conn.getResponseCode();
		} catch (IOException e) {
			throw new VingdTransportException("Connecting to Vingd Broker failed.", e);
		}

		StringBuffer response = new StringBuffer();
		String jsonContent;
		try {
			InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
			BufferedReader in = new BufferedReader(reader);
			String line;
			while ((line = in.readLine()) != null) response.append(line);
			in.close();
			jsonContent = response.toString();
		} catch (IOException e) {
			throw new VingdTransportException("Communication with Vingd Broker failed.", e);
		} finally {
			conn.disconnect();
		}

		// return data response if request successful
		Map<String,Object> contentMap;
		try {
			contentMap = (Map<String,Object>) jsonParseMap(jsonContent);
		} catch (Exception e) {
			throw new VingdTransportException("Non-JSON response or unexpected JSON structure.", "ParsingResponse", statusCode, e);
		}

		if (statusCode >= 200 && statusCode < 300) {
			Object data = contentMap.get("data");
			if (data == null) {
				throw new VingdTransportException("Invalid JSON error response.", "ParsingDataResponse");
			}
			return data;
		}

		// raise exception describing the vingd error condition
		String message, context;
		try {
			message = (String) contentMap.get("message");
			context = (String) contentMap.get("context");
		} catch (Exception e) { 
			throw new VingdTransportException("Invalid JSON error response.", "ParsingErrorResponse", statusCode, e);
		}
		throw new VingdOperationException(message, context, statusCode);
	}

	// {"key": [0, 1, 2..], ..}
	private Object getMapListElem(Map<String,Object> dict, String key, int index) {
		if (dict.get(key) == null)  {
			return null;
		}
		if (dict.get(key) instanceof ArrayList<?> == false) { 
			return null;
		}
		@SuppressWarnings("unchecked")
		List<Object> array = (ArrayList<Object>) dict.get(key);
		try {
			return array.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String,Object> convertToMap(Object response) throws VingdTransportException {
		if (response instanceof Map<?,?> == false) {
			throw new VingdTransportException("Unexpected service response; expecting JSON dictionary.", "Decode");
		}
		return (Map<String,Object>) response;
	}
	
	// Unholy, forward-compatible, mess for extraction of id/oid from a
	// soon-to-be (deprecated) batch response.
	@SuppressWarnings("unchecked")
	private long unpackBatchResponse(Object response, String name) throws VingdTransportException {
		String names = name+"s";
		Object id;
		Map<String,Object> responseMap = convertToMap(response);
		if (responseMap.containsKey(names)) {
			// soon-to-be deprecated (batch) response
			Map<String,String> errors = (Map<String,String>) getMapListElem(responseMap, "errors", 0);
			if (errors != null) {
				throw new VingdTransportException(errors.get("desc"), errors.get("code"));
			}
			id = getMapListElem(responseMap, names, 0);
		} else {
			// new-style simplified api response
			id = responseMap.get(name);
		}
		return new Double((Double)id).longValue();
	}

	/**
	 * Registers (enrolls) an object into the Vingd Objects Registry.
	 * 
	 * For minimal object description ({'name': <name>, 'url': <callback_url>}), 
	 * use RegisterObject(string name, string url) method.
	 * 
	 * @param description  Object description as HashMap<String,Object> containing object `name` and objectURL (`url`).
	 * @throws IOException 
	 * @throws HttpException 
	 * @throws VingdOperationException 
	 * @throws VingdTransportException 
	 * 
	 */
	public long createObject(Object description) throws VingdTransportException, VingdOperationException, IOException {
		Map<String,Object> parameters = new TreeMap<String,Object>();
		parameters.put("description", description);
		Object response = request("POST", "/registry/objects/", parameters);
		return unpackBatchResponse(response, "oid");
	}

	public long createObject(String name, String url) throws VingdTransportException, VingdOperationException, IOException {
		Map<String,Object> description = new TreeMap<String,Object>();
		description.put("name", name);
		description.put("url", url);
		return createObject((Object) description);
	}

	public Map<String,Object> getObject(long oid) throws VingdTransportException, VingdOperationException, IOException {
		return convertToMap(request("GET", "/registry/objects/"+oid, null));
	}

	/**
	 * Contacts Vingd Broker and generates a new order for selling the object (`oid`)
	 * under the defined terms (`price`). Order shall be valid until `expiresLocal`.
	 * Context shall be stored with order and returned upon purchase verification.
	 * 
	 * @param oid  Object ID.
	 * @param price  Price in vingd cents.
	 * @param context  Order-bound context, received upon purchase verify. 
	 * @param expiresLocal  Order expiry timestamp (in local time zone).
	 * @return VingdOrder
	 * 
	 * @throws IOException 
	 * @throws HttpException 
	 * @throws VingdOperationException 
	 * @throws VingdTransportException 
 	 * 
	 */
	public VingdOrder createOrder(long oid, int price, String context, Date expiresLocal) throws VingdTransportException, VingdOperationException, IOException {
		String expiresWithTimezone = localDateToISO8601(expiresLocal);
		Map<String,Object> parameters = new TreeMap<String,Object>();
		parameters.put("price", price);
		parameters.put("context", context);
		parameters.put("order_expires", expiresWithTimezone);

		Object response = request("POST", "/objects/"+oid+"/orders", parameters);
		long id = unpackBatchResponse(response, "id");

		return new VingdOrder(id, oid, price, context, expiresWithTimezone, frontendURL);
	}

	/**
	 * Create order for object `oid` with `price` in vingd cents. Order expires in
	 *   
	 * @param oid  Object ID.
	 * @param price  Price in vingd cents.
	 * @param context  Order-bound context, received upon purchase verify. 
	 * @param expiresMilliseconds  Order expiry time (in milliseconds).
	 * @return VingdOrder
	 * 
	 * @throws IOException 
	 * @throws HttpException 
	 * @throws VingdOperationException 
	 * @throws VingdTransportException 
	 */
	public VingdOrder createOrder(long oid, int price, String context, long expiresMilliseconds) throws VingdTransportException, VingdOperationException, IOException {
		Date now = new Date();
		Date expiresLocal = new Date(now.getTime() + expiresMilliseconds);
		return createOrder(oid, price, context, expiresLocal);
	}

	public VingdOrder createOrder(long oid, int price, String context) throws VingdTransportException, VingdOperationException, IOException {
		return createOrder(oid, price, context, defaultOrderExpiresMilliseconds);
	}

	public VingdOrder createOrder(long oid, int price, long expiresMilliseconds) throws VingdTransportException, VingdOperationException, IOException {
		return createOrder(oid, price, null, expiresMilliseconds);
	}

	public VingdOrder createOrder(long oid, int price) throws VingdTransportException, VingdOperationException, IOException {
		return createOrder(oid, price, null, defaultOrderExpiresMilliseconds);
	}

	private long toLong(Object val) {
		return new Double((Double)val).longValue();
	}

	/**
	 * Verifies purchase referenced by ObjectID `oid` and TokenID `tid`.
	 * 
	 * @param oid  ObjectID
	 * @param tid  TokenID
	 * @return  Purchase info (purchase id, transaction id, hashed user id, etc.)
	 * 
	 * @throws VingdTransportException
	 * @throws VingdOperationException
	 * @throws HttpException
	 * @throws IOException
	 */
	public VingdPurchase verifyPurchase(long oid, String tid) throws VingdTransportException, VingdOperationException, IOException {
		if (!isTokenFormatValid(tid)) {
			throw new VingdTransportException("Invalid token format.", "TokenVerification");
		}
		Object response = request("GET", "/objects/"+oid+"/tokens/"+tid, null);
		Map<String,Object> purchase = convertToMap(response);
		
		return new VingdPurchase(
			purchase.get("huid").toString(),
			oid,
			toLong(purchase.get("orderid")),
			toLong(purchase.get("purchaseid")),
			toLong(purchase.get("transferid")),
			purchase.get("context").toString()
		);
	}

	private boolean isTokenFormatValid(String tid) {
		// valid tid is alphanum(40)
		return Pattern.matches("^[a-fA-F\\d]{1,40}$", tid);
	}

	public VingdPurchase verifyPurchase(String token) throws VingdTransportException, VingdOperationException, IOException {
		// parse `oid`, `tid` from JSON-encoded `token`, where: `token` = {"oid": <oid>, "tid": <tid>}
		long oid;
		String tid;
		try {
			@SuppressWarnings("unchecked")
			Map<String,Object> tokenMap = (Map<String,Object>) jsonParseMap(token);
			oid = new Long((String)tokenMap.get("oid"));
			tid = (String) tokenMap.get("tid");
		} catch(Exception e) {
			throw new VingdTransportException("Invalid token.", "TokenVerification");
		}
		
		return verifyPurchase(oid, tid);
	}

	/**
	 * Commits user's reserved funds to seller account. Call CommitPurchase() upon
	 * successful delivery of paid content to the user. 
	 * 
	 * If you do not call commitPurchase() user shall be automatically refunded.
	 * 
	 * @param purchase  VingdPurchase object (purchase
	 * @throws VingdTransportException
	 * @throws VingdOperationException
	 * @throws HttpException
	 * @throws IOException
	 */
	public void commitPurchase(VingdPurchase purchase) throws VingdTransportException, VingdOperationException, IOException {
		Map<String,Object> parameters = new TreeMap<String,Object>();
		parameters.put("transferid", purchase.getTransferID());
		request("PUT", "/purchases/"+purchase.getPurchaseID(), parameters);
	}
}
