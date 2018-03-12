package net.vvoid.uuidp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Perfect (or paranoid...) collision free UUID(P).
 *
 * This is a ready to use UUID equivivalent for the turely paranoid and bad luck
 * belivers, i.e. those who argue that a collision probability of 1/2^128 is
 * completly unacceptable. Here is your solution... You pay by handling twice
 * the size and getting half the speed. But you can rest assured knowing there
 * can be no collision in your IDs.
 *
 * @author Dr. Daniel Georg Kirschner <Mail@vvoid.net>
 *
 */
public class UUIDP implements Comparable<UUIDP> {

  public static final int UUIDP_LONGS = 4;
  public static final int CRC_POSITION_IN_UUIDP = 12;
  public static final long XOR_PATTERN = 0xDEADBEEFDEADBEEFL;

  // internal storage for uuidp long values
  private long uuidp[] = new long[UUIDP_LONGS];

  /**
   * Get a fresh UUIDP.
   */
  public UUIDP() {
    uuidp = generateUUIDP();
    try {
      assertUuidp(uuidp);
    } catch (UuidpInvalidException ex) {
      throw new RuntimeException("Internal error!", ex);
    }
  }

  /**
   * Generate a hopefully perfectly (paranoid) collision free UUID(P).
   *
   * A UUIDP consists of: (a) UUID Type 4 identifier (random part); (b) the
   * current time from System.currentTimeMillis() (systematic part 1); (c) the
   * current value from System.nanoTime() (systematic part 2). b and c are xored
   * with XOR_PATTERN and the UUID (a) to look more random, i.e. to hide the
   * leading zeros.
   *
   * To get a collision you would have to get a collision of two UUID
   * identifiers (1/2^128) at the same wall clock time (extreemly unlikely) at
   * the same time and runtime time (this should be unlikely enough...). UUIDP
   * can be compared and ordered by creation time. Strong integrity protection
   * is provided by an embedded CRC32.
   *
   * @return UUIDP
   */
  public static long[] generateUUIDP() {
    UUID uuid = UUID.randomUUID();

    long[] uuidp = new long[UUIDP_LONGS];

    uuidp[3] = uuid.getMostSignificantBits();
    uuidp[2] = uuid.getLeastSignificantBits();
    uuidp[1] = System.nanoTime() ^ XOR_PATTERN ^ uuidp[3];
    uuidp[0] = System.currentTimeMillis() ^ XOR_PATTERN ^ uuidp[2];

    ByteBuffer crc32ByteBuffer = checksumUUIDP(longsToBytes(uuidp));

    // write crc32 back
    uuidp[1] = crc32ByteBuffer.getLong(8);

    return uuidp;
  }

  /**
   * custom constructor.
   *
   * @param uuidp
   */
  public UUIDP(long[] uuidp) throws UuidpInvalidException {
    this.uuidp = uuidp;
    assertUuidp(uuidp);
  }

  /**
   * custom constructor.
   *
   * @param uuidp
   */
  public UUIDP(byte[] uuidp) {
    this.uuidp = byteToUuidp(uuidp);
  }

  /**
   * custom constructor.
   *
   * @param string Base64 encoded UUIDP
   */
  public UUIDP(String string) {
    this.uuidp = byteToUuidp(Base64.getUrlDecoder().decode(string));
  }

  /**
   * copy constructor.
   *
   * @param uuidp
   */
  public UUIDP(UUIDP uuidp) throws UuidpInvalidException {
    this.uuidp = uuidp.getUuidp();
    assertUuidp(uuidp.getUuidp());
  }

  /**
   * Convert byte array to long array for uuidp.
   *
   * @param bytes
   * @return
   */
  private static long[] byteToUuidp(byte[] bytes) {

    int size = (bytes.length / 4) * 4 + 4;

    ByteBuffer bb = ByteBuffer.allocate(size);
    bb.put(bytes);
    bb.clear();

    long[] uuidpLong = new long[UUIDP_LONGS];

    uuidpLong[0] = bb.getLong();
    uuidpLong[1] = bb.getLong();
    uuidpLong[2] = bb.getLong();
    uuidpLong[3] = bb.getLong();

    return uuidpLong;
  }

  /**
   * Get UUIDP data as array of long.
   *
   * @return
   */
  public long[] getUuidp() {
    return uuidp;
  }

  /**
   * Get UUIDP data as array of byte.
   *
   * @return
   */
  public byte[] getBytes() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(UUIDP_LONGS * 8);

    for (int i = 0; i < uuidp.length; i++) {
      long v = uuidp[i];
      byteBuffer.putLong(v);
    }

    return byteBuffer.array();
  }

  /**
   * Get UUIDP in Base64 encoding.
   *
   * Be advided: Result is unpadded, i.e. without trailing (=|==|==)!
   *
   * @return
   */
  public String getBase64() {
    String b64 = Base64.getUrlEncoder().encodeToString(getBytes());
    String unpadded;
    int length = b64.length();

    if (b64.endsWith("===")) {
      unpadded = b64.substring(0, length - 3);
    } else if (b64.endsWith("==")) {
      unpadded = b64.substring(0, length - 2);
    } else if (b64.endsWith("=")) {
      unpadded = b64.substring(0, length - 1);
    } else {
      unpadded = b64;
    }

    return unpadded;
  }

  /**
   * Get UUIDP in Base64 encoding.
   *
   * @return
   */
  public String getBase64Padded() {
    return Base64.getUrlEncoder().encodeToString(getBytes());
  }

  /**
   * Get time of creation of this UUIDP in the format of
   * System.currentTimeMillis().
   *
   * @return
   */
  public long getCreationTime() {
    Long time = uuidp[0] ^ XOR_PATTERN ^ uuidp[2];
    return time;
  }

  /**
   * Get time of creation of this UUIDP in the format of System.nanoTime().
   *
   * @return
   */
  public long getCreationNanoTime() {
    Long time = uuidp[1] ^ XOR_PATTERN ^ uuidp[3];
    return time;
  }

  /**
   * Compare/Sort UUIDP by creation time.
   *
   * In case the creation times are equal the UUIDP with the smaller NanoTime is
   * considered junger. In case the NanoTime is equal the UUIDP with the smaller
   * long value is considered junger.
   *
   * @param uuidp
   * @return
   */
  @Override
  public int compareTo(UUIDP uuidp) {

    long delta;

    compareByUnixTime:
      {
        delta = this.getCreationTime() - uuidp.getCreationTime();
        if (delta < 0) {
          return -1;
        } else if (delta > 0) {
          return 1;
        }
      }

    compareByNanoTimer:
      {
        delta = this.getCreationNanoTime() - uuidp.getCreationNanoTime();
        if (delta < 0) {
          return -1;
        } else if (delta > 0) {
          return 1;
        }
      }

    compareByUUID:
      {
        delta = this.uuidp[3] - uuidp.uuidp[3];
        if (delta < 0) {
          return -1;
        } else if (delta > 0) {
          return 1;
        }

        delta = this.uuidp[2] - uuidp.uuidp[2];
        if (delta < 0) {
          return -1;
        } else if (delta > 0) {
          return 1;
        }
      }

    // in this case they are really identical!
    return 0;
  }

  /**
   * Check if uuidp1 is junger than uuidp2.
   *
   * @param uuidp1
   * @param uuidp2
   * @return true if uuidp1 is junger than uuidp2
   */
  public static boolean isJungerThan(UUIDP uuidp1, UUIDP uuidp2) {
    if (uuidp1.compareTo(uuidp2) < 0) {
      return true;
    }
    return false;
  }

  /**
   * Create CRC32 checksum of UUIDP.
   *
   * @param byteBuffer
   * @return
   */
  public static ByteBuffer checksumUUIDP(ByteBuffer byteBuffer) {
    // use the 4 high bytes of the nano time as CRC32 storage

    byteBuffer.putInt(CRC_POSITION_IN_UUIDP, 0);

    CRC32 crc32 = new CRC32();
    crc32.reset();
    crc32.update(byteBuffer.array());

    byteBuffer.putInt(CRC_POSITION_IN_UUIDP, (int) crc32.getValue());

    return byteBuffer;
  }

  /**
   * Check if the integrity of the UUIDP seems to be ok (CRC32 check).
   *
   * @param uuidp
   * @throws net.vvoid.uuidp.UuidpInvalidException
   */
  public static void assertUuidp(long[] uuidp) throws UuidpInvalidException {
    if (!verifyUuidpIntegrity(uuidp)) {
      throw new UuidpInvalidException("UUIDP seftest failed! Checksum inconsistent with data!");
    }
  }

  /**
   * Check if the integrity of the UUIDP seems to be ok (CRC32 check).
   *
   * @param uuidp
   * @return
   */
  public static boolean verifyUuidpIntegrity(long[] uuidp) {
    // use the 4 high bytes of the nano time as CRC32 storage
    ByteBuffer byteBuffer = checksumUUIDP(longsToBytes(uuidp));

    int providedCRC = longsToBytes(uuidp).getInt(CRC_POSITION_IN_UUIDP);
    int calculatedCRC = byteBuffer.getInt(CRC_POSITION_IN_UUIDP);

    return providedCRC == calculatedCRC;
  }

  /**
   * Check if the integrity of the UUIDP seems to be ok (CRC32 check).
   *
   * @return
   */
  public boolean verifyUuidpIntegrity() {
    return verifyUuidpIntegrity(uuidp);
  }

  /**
   * Check if the integrity of the UUIDP seems to be ok (CRC32 check).
   *
   * @throws net.vvoid.uuidp.UuidpInvalidException
   */
  public void assertIntegrety() throws UuidpInvalidException {
    assertUuidp(uuidp);
  }

  /**
   * Check for equality of this UUIDP to other UUIDP.
   *
   * @param uuidp
   * @return true if identical
   */
  public boolean equals(UUIDP uuidp) {
    return compare(uuidp.getUuidp(), this.uuidp);
  }

  /**
   * Compare two UUIDP identifier.
   *
   * @param uuidp1
   * @param uuidp2
   * @return
   */
  public static boolean compare(long[] uuidp1, long uuidp2[]) {

    if (uuidp1.length != uuidp2.length) {
      return false;
    }

    for (int i = 0; i < uuidp2.length; i++) {
      if (uuidp1[i] != uuidp2[i]) {
        return false;
      }

    }

    return true;
  }

  /**
   * Convert long[] to ByteBuffer.
   *
   * @param longs
   * @return
   */
  public static ByteBuffer longsToBytes(long[] longs) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * Long.BYTES);

    for (int i = 0; i < longs.length; i++) {
      byteBuffer.putLong(longs[i]);
    }

    return byteBuffer;
  }

  /**
   * Make a nice String out of a long[].
   *
   * @param uuidp
   * @return
   */
  private static String toHexString(long[] uuidp) {
    StringBuilder sb = new StringBuilder();
    for (int i = uuidp.length - 1; 0 <= i; i--) {
      long l = uuidp[i];
      sb.append(String.format("%1$016x", l));
    }
    return "0x" + sb.toString();
  }

  /**
   * Create a UUIDP from a hex String.
   *
   * @param string
   * @return
   */
  public static long[] getUuidpFromString(String string) throws Exception {

    long[] uuidp = new long[UUIDP_LONGS];

    if (string.startsWith("[") && string.endsWith("]")) {
      string = string.substring(1, string.length() - 1);
      String[] numbers = string.split(",");

      for (int i = UUIDP_LONGS - 1, j = 0; i >= 0; i--, j++) {
        uuidp[j] = new Long(numbers[i].trim());
      }

    } else {

      if (!string.startsWith("0x")) {
        throw new Exception("Unexpected start of string!");
      }

      string = string.substring(2);

      if (string.length() != UUIDP_LONGS * 16) {
        throw new Exception("Length of UUIDP string is not correct!");
      }

      for (int i = UUIDP_LONGS - 1, j = 0; i >= 0; i--, j += 16) {
        String subString = string.substring(j, j + 16);
        uuidp[i] = Long.parseUnsignedLong(subString, 16);
      }

    }
    return uuidp;

  }

  /**
   * Convert UUIDP to String.
   *
   * Be advised: padding is omitted.
   *
   * @return
   */
  @Override
  public String toString() {
    return getBase64();
  }

  @Override
  public boolean equals(Object uuidp) {
    if (!(uuidp instanceof UUIDP)) {
      throw new RuntimeException("Argument has incompatible Calss!");
    }
    return 0 == compareTo(((UUIDP) uuidp));
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 83 * hash + Arrays.hashCode(this.uuidp);
    return hash;
  }

  /**
   * Convert UUIDP to square bracket string array.
   *
   * @return
   */
  public String toStringArray() {
    String result = ""
      + "["
      + uuidp[3] + ", "
      + uuidp[2] + ", "
      + uuidp[1] + ", "
      + uuidp[0]
      + "]";

    return result;
  }

}
