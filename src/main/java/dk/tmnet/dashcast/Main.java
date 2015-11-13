package dk.tmnet.dashcast;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import su.litvak.chromecast.api.v2.ChromeCast;

import com.google.common.collect.Lists;

/**
 * ChromeCast helper.
 * 
 * <p>
 * The main method takes 0-3 args:
 * </p>
 * <p>
 * If no args are provided, a list of local ChromeCast devices is produced.
 * </p>
 * <p>
 * If one arg is provided, it is interpreted as the URL to cast, and it will be
 * cast to the first local ChromeCast discovered.
 * </p>
 * <p>
 * If a second arg is provided, it is interpreted ad the hostname or IP of the
 * target ChromeCast.
 * </p>
 * <p>
 * If a third arg is provided, it is interpreted as the port number on the
 * ChromeCast (default is 8009 if none is provided).
 * </p>
 * <p>
 * To use the "force" option, put "-f" in front of the first arg.
 * </p>
 * <p>
 * Examples:
 * </p>
 * <p>
 * Load the monitor page on a specific ChromeCast device:<br/>
 * <tt>java -jar chromecast.jar.jar http://user:pw@monitor.tmnet.dk/adagios/status/dashboard 192.168.0.224 8009</tt>
 * </p>
 * <p>
 * Force-læoad eb.dk on the first available device:<br/>
 * <tt>java -jar chromecast.jar.jar -f http://www.eb.dk/</tt>
 * </p>
 */
@Slf4j
public class Main {

	public static void main(String[] argies) throws Exception {
		List<String> args = new ArrayList<>(Lists.newArrayList(argies));

		if (args.size() == 0) {
			List<DiscoveredChromeCast> discover = DiscoveredChromeCast.discover();
			log.info("Searching sub-net for ChromeCast devices ...");
			for (DiscoveredChromeCast discoveredChromeCast : discover) {
				log.info("===> {}", discoveredChromeCast.toString());
			}
			return;
		}

		boolean force = false;

		if (args.get(0).equals("-f")) {
			args.remove(0);
			force = true;
		}

		String url = getArg(args, 0);
		String host = getArg(args, 1);
		String port = getArg(args, 2);

		if (host == null) {
			String hostPort[] = getDefaultHostPort();
			host = hostPort[0];
			port = hostPort[1];
		}

		if (port == null) {
			port = "8009";
		}

		log.debug("Streaming '{}' to {}:{}", url, host, port);

		try (DashCast dashCast = new DashCast(new ChromeCast(host, Integer.valueOf(port)))) {
			dashCast.connect();
			dashCast.castUrl(new URL(url), force, 60000);
		}		
	}

	private static String[] getDefaultHostPort() {
		DiscoveredChromeCast chromeCast = DiscoveredChromeCast.discover().get(0);
		return new String[] { chromeCast.getAddress(), "" + chromeCast.getPort() };
	}

	private static String getArg(List<String> args, int i) {
		if (i >= args.size() || i < 0)
			return null;
		return args.get(i);
	}

}
