package backend;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DataLoad {
    private InputStream inputStream;
    private long size;

    public DataLoad(byte[] data) {
        this(new ByteArrayInputStream(data), data.length);
    }

    public DataLoad(InputStream stream, long length) {
        this.inputStream = stream;
        this.size = length;
    }

    public static String getChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            return getHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Can't get checksum.");
        }
        return null;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public long getSize() {
        return this.size;
    }

    private static final String    HEXES    = "0123456789ABCDEF";

    static String getHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }


}
