package dk.tmnet.dashcast;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.Request;
import su.litvak.chromecast.api.v2.Response;
import su.litvak.chromecast.api.v2.Status;

public class DashCast implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(DashCast.class);

	// DashCast constants
	private static final String DASHCAST_APP_ID = "5C3F0A3C";
	private static final String DASHCAST_NS = "urn:x-cast:es.offd.dashcast";

	private ChromeCast chromeCast;

	public DashCast(ChromeCast chromeCast) {
		this.chromeCast = chromeCast;
	}

	/**
	 * Use DashCast to stream the contents of a URL to ChromeCast.
	 * 
	 * If {@code force} is true, DashCast will try to load the target {@code url}
	 * directly, instead of in an iframe. This cuts the control connection,
	 * meaning that automatic reloading is not possible.
	 * 
	 * @param url
	 *           the URL to stream
	 * @param force
	 *           force direct load
	 * @param reloadTimeMs
	 *           optional reload interval. Doesn't work if {@code force} is
	 *           <code>true</code>
	 * @throws IOException
	 *            if the communication with the ChromeCaste fails
	 */
	public void castUrl(URL url, boolean force, Integer reloadTimeMs) throws IOException {
		Status status = chromeCast.getStatus();

		// Check for DashCast
		if (!chromeCast.isAppAvailable(DASHCAST_APP_ID)) {
			throw new RuntimeException("Unable to locate DashCast app, id: "
					+ DASHCAST_APP_ID);
		}

		Application runningApp = status.getRunningApp();

		LOG.debug("(Re)loading the DashCast receiver");
		runningApp = chromeCast.launchApp(DASHCAST_APP_ID);
		if (runningApp == null) {
			throw new RuntimeException("Unable load DashCast app, id: " + DASHCAST_APP_ID);
		}

		boolean reload = reloadTimeMs == null ? false : true;
		int reloadTime = reload ? reloadTimeMs.intValue() : 0;

		DashCastRequest request = new DashCastRequest(url.toString(), force, reload,
				reloadTime);

		chromeCast.send(DASHCAST_NS, request, DashCastResponse.class);
	}

	@Override
	public void close() throws IOException {
		chromeCast.disconnect();
	}

	public void connect() throws IOException, GeneralSecurityException {
		chromeCast.connect();
	}
}

class DashCastRequest extends DashCastData implements Request {
	DashCastRequest(String url, boolean force, boolean reload, int reloadTime) {
		this.url = url;
		this.force = force;
		this.reload = reload;
		this.reloadTime = reloadTime;
	}
}

class DashCastResponse extends DashCastData implements Response {
}

class DashCastData {
	@JsonProperty
	String url;

	@JsonProperty
	boolean force;

	@JsonProperty
	boolean reload;

	@JsonProperty("reload_time")
	int reloadTime;

	private Long requestId;

	public Long getRequestId() {
		return requestId;
	}

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}
}