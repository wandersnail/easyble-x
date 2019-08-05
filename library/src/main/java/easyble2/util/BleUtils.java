package easyble2.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * date: 2019/8/3 09:37
 * author: zengfansheng
 */
public class BleUtils {
    /**
     * 字节数组转字符串
     *
     * @param src 数据源
     */
    public static String bytesToHex(byte[] src) {
        StringBuilder sb = new StringBuilder();
        if (src == null || src.length == 0) {
            return "";
        }
        for (int i = 0; i < src.length; i++) {
            String hv = Integer.toHexString(src[i] & 0xff);
            if (hv.length() < 2) {
                sb.append(0);
            }
            sb.append(hv);
            if (src.length - 1 != i) {
                sb.append(" ");
            }
        }
        return sb.toString().toUpperCase(Locale.ENGLISH);
    }

    /**
     * 将整数转字节数组
     *
     * @param bigEndian true表示高位在前，false表示低位在前
     * @param value     整数，short、int、long
     * @param len       结果取几个字节，如是高位在前，从数组后端向前计数；如是低位在前，从数组前端向后计数
     */
    @NonNull
    public static byte[] numberToBytes(boolean bigEndian, long value, int len) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            int j = bigEndian ? 7 - i : i;
            bytes[i] = (byte) (value >> 8 * j & 0xff);
        }
        if (len > 8) {
            return bytes;
        } else {
            return Arrays.copyOfRange(bytes, bigEndian ? 8 - len : 0, bigEndian ? 8 : len);
        }
    }

    /**
     * 将字节数组转数值
     *
     * @param bigEndian true表示高位在前，false表示低位在前
     * @param src       待转字节数组
     */
    public static long bytesToNumber(boolean bigEndian, @NonNull byte... src) {
        int len = Math.min(8, src.length);
        byte[] bs = new byte[8];
        System.arraycopy(src, 0, bs, bigEndian ? 8 - len : 0, len);
        long value = 0;
        // 循环读取每个字节通过移位运算完成long的8个字节拼装
        for (int i = 0; i < 8; i++) {
            int shift = (bigEndian ? 7 - i : i) << 3;
            value = value | ((long) 0xff << shift & ((long) bs[i] << shift));
        }
        if (src.length == 1) {
            return value & 0xff;
        } else if (src.length == 2) {
            return value & 0xffff;
        } else if (src.length <= 4) {
            return value & 0xffffffffL;
        }
        return value;
    }

    /**
     * 分包
     *
     * @param src  源
     * @param size 包大小，字节
     * @return 分好的包的集合
     */
    @NonNull
    public static List<byte[]> splitPackage(@NonNull byte[] src, int size) {
        List<byte[]> list = new ArrayList<>();
        int loop = src.length / size + (src.length % size == 0 ? 0 : 1);
        for (int i = 0; i < loop; i++) {
            int from = i * size;
            int to = Math.min(src.length, from + size);
            list.add(Arrays.copyOfRange(src, i * size, to));
        }
        return list;
    }

    /**
     * 组包
     *
     * @param src 源
     * @return 组好的字节数组
     */
    @NonNull
    public static byte[] joinPackage(@NonNull byte[]... src) {
        byte[] bytes = new byte[0];
        for (byte[] bs : src) {
            bytes = Arrays.copyOf(bytes, bytes.length + bs.length);
            System.arraycopy(bs, 0, bytes, bytes.length - bs.length, bs.length);
        }
        return bytes;
    }

    /**
     * 16-bit Service Class UUIDs或32-bit Service Class UUIDs
     */
    @NonNull
    public static UUID generateFromBaseUuid(long value) {
        return new UUID(4096 + (value << 32), -9223371485494954757L);
    }

    /**
     * 判断广播字段里是否有此UUID
     *
     * @param advData 广播数据
     * @param uuid    要查询的UUID
     */
    public static boolean hasUUID(@Nullable byte[] advData, @NonNull UUID uuid) {
        return hasUUID(advData, Collections.singletonList(uuid));
    }

    /**
     * 判断广播字段里是否集合中的其中一个UUID
     *
     * @param advData 广播数据
     * @param uuids   要查询的UUID集合
     */
    public static boolean hasUUID(@Nullable byte[] advData, @NonNull List<UUID> uuids) {
        try {
            if (advData == null) {
                return false;
            }
            ByteBuffer buffer = ByteBuffer.wrap(advData).order(ByteOrder.LITTLE_ENDIAN);
            while (buffer.remaining() > 2) {
                int len = buffer.get();
                if (len == 0) {
                    continue;
                }
                switch (buffer.get()) {
                    case 0x02:    // Partial list of 16-bit UUIDs	
                    case 0x03:    // Complete list of 16-bit UUIDs	
                    case 0x14:    // List of 16-bit Service Solicitation UUIDs
                        while (len >= 2) {
                            String name = String.format(Locale.US, "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort());
                            if (uuids.contains(UUID.fromString(name))) {
                                return true;
                            }
                            len -= 2;
                        }
                        break;
                    case 0x04:
                    case 0x05:
                        while (len >= 4) {
                            String name = String.format(Locale.US, "%08x-0000-1000-8000-00805f9b34fb", buffer.getInt());
                            if (uuids.contains(UUID.fromString(name))) {
                                return true;
                            }
                            len -= 4;
                        }
                        break;
                    case 0x06: // Partial list of 128-bit UUIDs
                    case 0x07: // Complete list of 128-bit UUIDs
                    case 0x15: // List of 128-bit Service Solicitation UUIDs
                        while (len >= 16) {
                            long lsb = buffer.getLong();
                            long msb = buffer.getLong();
                            if (uuids.contains(new UUID(msb, lsb))) {
                                return true;
                            }
                            len -= 16;
                        }
                        break;
                    default:
                        buffer.position(buffer.position() + len - 1);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
