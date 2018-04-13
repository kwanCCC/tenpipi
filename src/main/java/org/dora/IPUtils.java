package org.dora;

import java.util.Random;

public class IPUtils {
    private static Random random = new Random();

    public static int ipToInt(String ip) {
        String[] ips;
        int netMask = 0;
        if (!ip.contains("/")) {
            ips = ip.split("\\.");
        } else {
            ips = ip.substring(0, ip.indexOf("/")).split("\\.");
            netMask = getNetMask(ip);
        }

        int ip0 = Integer.parseInt(ips[0]);
        int ip1 = Integer.parseInt(ips[1]);
        int ip2 = Integer.parseInt(ips[2]);
        int ip3 = Integer.parseInt(ips[3]);

        int intIP = ip0 << 24 | ip1 << 16 | ip2 << 8 | ip3;
        if (netMask > 0) {
            intIP = getSubNet(netMask, intIP);
        }
        return intIP;
    }

    /**
     * return subnet of the given ip
     *
     * @param netMask
     * @param ip
     *
     * @return returns the ip directly if netMask == 32
     */
    public static int getSubNet(int netMask, int ip) {
        if (netMask < 32) {
            return ip >> (32 - netMask) << (32 - netMask);
        } else {
            return ip;
        }
    }

    public static boolean isSubNet(String ip) {
        return ip.contains("/");
    }

    public static int getNetMask(String ip) {
        if (ip.contains("/")) {
            return Integer.parseInt(ip.substring(ip.indexOf("/") + 1));
        } else {
            return 32;
        }
    }

    /**
     * generate a random ip
     *
     * @return
     */
    public static String random() {
        return intToIP(random.nextInt());
    }

    public static String intToIP(int ip) {
        StringBuilder sb = new StringBuilder();
        int mask = 0xFF;
        // TODO: avoid '0' in ip address?
        int i4 = ip & mask;
        int i3 = (ip >> 8) & mask;
        int i2 = (ip >> 16) & mask;
        int i1 = (ip >> 24) & mask;
        return sb.append(i1).append(".").append(i2).append(".").append(i3).append(".").append(i4).toString();
    }


}
