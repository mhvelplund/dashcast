package dk.tmnet.dashcast;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.builder.ToStringBuilder;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;

@Slf4j
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class DiscoveredChromeCast {
	@NonNull
	private String name;
	
	@NonNull
	private String address;
	
	private int port;

	public static List<DiscoveredChromeCast> discover() {
		return discover(5000);
	}

	public static List<DiscoveredChromeCast> discover(int timeout) {
		return discover(timeout, timeout/2);
	}

	public static List<DiscoveredChromeCast> discover(int timeout, int minWait) {
		return discover(timeout, minWait, 10);
	}

	public static List<DiscoveredChromeCast> discover(int timeout, int minWait, int segments) {
		List<DiscoveredChromeCast> dccs = new ArrayList<>();

		ChromeCasts chromeCasts = null;
		try {
			ChromeCasts.startDiscovery();

			long start = System.currentTimeMillis();
			for (int i = 0; i < segments; i++) {
				log.trace("Looking for ChromeCasts ...");

				chromeCasts = ChromeCasts.get();

				long now = System.currentTimeMillis();

				if (chromeCasts.size() > 0 && (now - start) > minWait) {
					break;
				}

				try {
					Thread.sleep(timeout / segments);
				} catch (InterruptedException e) {
					break;
				}
			}

			for (ChromeCast chromeCast : chromeCasts) {
				try {
					chromeCast.connect();
					DiscoveredChromeCast dcc = new DiscoveredChromeCast(chromeCast.getName(),
							chromeCast.getAddress(), chromeCast.getPort());
					dccs.add(dcc);
				} catch (IOException | GeneralSecurityException e) {
					log.warn("Unable to connect to ChromeCast: {}",
							ToStringBuilder.reflectionToString(chromeCast), e);
				} finally {
					if (chromeCast != null) {

						try {
							chromeCast.disconnect();
						} catch (IOException e) {
							log.warn("Unable to close ChromeCast connection: {}",
									ToStringBuilder.reflectionToString(chromeCast), e);
						}
					}
				}
			}
		} catch (IOException e) {
			log.warn("Unable to start ChromeCast discovery", e);
		} finally {
			try {
				ChromeCasts.stopDiscovery();
			} catch (IOException e) {
				log.warn("Unable to stop ChromeCast discovery", e);
			}
		}

		return dccs;
	}

}