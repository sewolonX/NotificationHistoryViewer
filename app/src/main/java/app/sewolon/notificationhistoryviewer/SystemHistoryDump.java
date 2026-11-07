package app.sewolon.notificationhistoryviewer;

import android.os.IBinder;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/** Invoked only on demand through a root app_process to read system history, including its buffer. */
public final class SystemHistoryDump {
    public static void main(String[] args) throws Exception {
        Class<?> manager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) manager.getMethod("getService", String.class).invoke(null, "notification");
        Class<?> stub = Class.forName("android.app.INotificationManager$Stub");
        Object service = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        Method historyMethod = service.getClass().getMethod("getNotificationHistory", String.class, String.class);
        Object history = historyMethod.invoke(service, "com.android.shell", null);
        Class<?> historyClass = Class.forName("android.app.NotificationHistory");
        Method hasNext = historyClass.getMethod("hasNextNotification");
        Method next = historyClass.getMethod("getNextNotification");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Method packageMethod = null, timeMethod = null, titleMethod = null, textMethod = null;
        Method channelNameMethod = null, channelIdMethod = null, conversationMethod = null, userMethod = null, iconMethod = null;
        while ((Boolean) hasNext.invoke(history)) {
            Object item = next.invoke(history);
            if (item == null) break;
            if (packageMethod == null) {
                Class<?> itemClass = item.getClass();
                packageMethod = itemClass.getMethod("getPackage");
                timeMethod = itemClass.getMethod("getPostedTimeMs");
                titleMethod = itemClass.getMethod("getTitle");
                textMethod = itemClass.getMethod("getText");
                channelNameMethod = itemClass.getMethod("getChannelName");
                channelIdMethod = itemClass.getMethod("getChannelId");
                conversationMethod = itemClass.getMethod("getConversationId");
                userMethod = itemClass.getMethod("getUserId");
                iconMethod = itemClass.getMethod("getIcon");
            }
            String pkg = (String) packageMethod.invoke(item);
            long time = (Long) timeMethod.invoke(item);
            String title = (String) titleMethod.invoke(item);
            String text = (String) textMethod.invoke(item);
            String channelName = (String) channelNameMethod.invoke(item);
            String channelId = (String) channelIdMethod.invoke(item);
            String conversationId = (String) conversationMethod.invoke(item);
            int userId = (Integer) userMethod.invoke(item);
            Object icon = iconMethod.invoke(item);
            Object iconType = icon == null ? null : icon.getClass().getMethod("getType").invoke(icon);
            String iconPackage = icon == null ? "" : (String) icon.getClass().getMethod("getResPackage").invoke(icon);
            int iconResource = icon == null ? 0 : (Integer) icon.getClass().getMethod("getResId").invoke(icon);
            ByteArrayOutputStream record = new ByteArrayOutputStream();
            writeString(record, 1, pkg);
            writeVarintField(record, 9, time);
            writeString(record, 10, title);
            writeString(record, 11, text);
            writeVarintField(record, 7, userId);
            writeString(record, 3, channelName);
            writeString(record, 5, channelId);
            writeString(record, 13, conversationId);
            if (icon != null) {
                ByteArrayOutputStream iconRecord = new ByteArrayOutputStream();
                writeVarintField(iconRecord, 1, ((Number) iconType).longValue());
                if (iconResource != 0) writeVarintField(iconRecord, 3, iconResource);
                if (iconPackage != null && !iconPackage.isEmpty()) writeString(iconRecord, 4, iconPackage);
                writeBytesField(record, 12, iconRecord.toByteArray());
            }
            writeBytesField(output, 3, record.toByteArray());
        }
        System.out.write(output.toByteArray());
        System.out.flush();
    }

    private static void writeString(ByteArrayOutputStream out, int field, String value) {
        writeBytesField(out, field, (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBytesField(ByteArrayOutputStream out, int field, byte[] value) {
        writeVarint(out, field * 8L + 2L);
        writeVarint(out, value.length);
        out.write(value, 0, value.length);
    }

    private static void writeVarintField(ByteArrayOutputStream out, int field, long value) {
        writeVarint(out, field * 8L);
        writeVarint(out, value);
    }

    private static void writeVarint(ByteArrayOutputStream out, long value) {
        while ((value & ~127L) != 0) {
            out.write(((int) value & 127) | 128);
            value >>>= 7;
        }
        out.write((int) value);
    }
}
