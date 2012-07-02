package org.ark;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.soap.Node;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

public class App {

	/*
	 * 
	 * http://i.imgur.com/jsyzf.gif http://i.imgur.com/vAjRx.gif
	 * http://i.imgur.com/XJn7g.gif
	 */
	final private static int PROP_CHARBUF_SIZE_16K = 16384;
	final private static String PROP_URL_SERVICE = "http://quickthumbnail.com/";

	private HttpClient m_httpClient;
	private HttpGet m_httpget;
	private String m_phpSessionID;
	private Tidy m_tidy;
	private XPath m_xpath;

	private static void L(String msg) {
		System.err.println(msg);
		System.err.flush();
	}

	private static void Lo(String msg) {
		System.out.println(msg);
		System.out.flush();
	}

	private static void safeClose(InputStream is) {
		if (is == null) {
			return;
		}
		try {
			is.close();
		} catch (Exception e) {
			// sink it
		}
	}

	private boolean initConnection() {
		boolean l_rc = false;// fail

		return l_rc;
	}

	/* set it all up */
	public void init() {
		this.m_httpClient = new DefaultHttpClient();

		m_tidy = new Tidy();
		m_tidy.setXHTML(true);
		m_tidy.setDropEmptyParas(true);
		m_tidy.setFixComments(true);
		m_tidy.setForceOutput(true);
		m_tidy.setIndentAttributes(false);
		m_tidy.setIndentContent(true);
		m_tidy.setFixBackslash(true);
		m_tidy.setWrapAttVals(false);
		m_tidy.setTrimEmptyElements(true);
		m_tidy.setLowerLiterals(true);
		m_tidy.setErrout(new PrintWriter(new OutputStreamWriter(
				new OutputStream() {
					@Override
					public void write(int b) throws IOException {
						// SINK IT
					}
				})));
		XPathFactory factory = XPathFactory.newInstance();
		m_xpath = factory.newXPath();

	}

	public String firstStep() {
		String repsonseBody = null;
		String l_key = null;
		try {
			HttpGet httpget = new HttpGet(PROP_URL_SERVICE);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			repsonseBody = m_httpClient.execute(httpget, responseHandler);
			httpget.releaseConnection();
		} catch (IOException ioe) {
			L(ioe.getMessage());
			return null;
		}

		Writer w = new StringWriter(PROP_CHARBUF_SIZE_16K);
		Document d = m_tidy.parseDOM(new StringReader(repsonseBody), w);
		org.w3c.dom.Node key = null;

		// <input id="key" name="key" type="hidden"
		// value="25734fff-565f-4a0c-aa27-b02acf09e35a" />
		// get the value for key form value
		try {
			key = (org.w3c.dom.Node) m_xpath.evaluate(
					"//input[@id='key']/@value", d, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (key != null) {
			l_key = key.getNodeValue();
			Lo(String.format("key found is[%s]", l_key));
		}
		return l_key;
	}

	public String requestThumbnail(String key, String picUrl) {

		String responseBody=null;
		HttpPost postThumbnailer = new HttpPost(PROP_URL_SERVICE
				+ "/online-image-resizer.php");
	
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("key", key));
        nvps.add(new BasicNameValuePair("url", picUrl));

        postThumbnailer.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
		
		try {
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseBody =	this.m_httpClient.execute(postThumbnailer,responseHandler);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		postThumbnailer.releaseConnection();
		return picUrl;
	}

	public void shutdown() {
		m_httpClient.getConnectionManager().shutdown();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		App app = new App();

		app.init();
		//
		for (int i = 0; i < 1000; i++) {
			String key = app.firstStep();
			if (key == null) {
				L("no key found");
				System.exit(1);
			}

			app.requestThumbnail(key, "http://i.imgur.com/jsyzf.gif");
		}
		app.shutdown();

	}
}
