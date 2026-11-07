package app.sewolon.notificationhistoryviewer;

import java.io.*;
import java.util.*;

public final class HistoryReader {
    public static final String DIR = "/data/system_ce/0/notification_history/history/";
    public static byte[] rootRead(String command) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buf = new byte[8192]; int n;
        try (InputStream in = p.getInputStream()) {
            while ((n = in.read(buf)) != -1) { if (out.size() + n > 8 * 1024 * 1024) { p.destroyForcibly(); throw new IOException("单个文件超过 8 MB，已跳过"); } out.write(buf, 0, n); }
        }
        if (p.waitFor() != 0) throw new IOException(new String(out.toByteArray(), "UTF-8"));
        return out.toByteArray();
    }
    public static List<Entry> readSystemHistory(String apkPath) throws Exception {
        if (!apkPath.matches("/data/app/[-A-Za-z0-9_./=~]+\\.apk"))
            throw new IOException("Invalid APK path");
        Process process = new ProcessBuilder("su", "-c", "CLASSPATH=" + apkPath
                + " app_process /system/bin app.sewolon.notificationhistoryviewer.SystemHistoryDump").start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int count;
            try (InputStream input = process.getInputStream()) {
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 64 * 1024 * 1024) {
                        process.destroyForcibly();
                        throw new IOException("System history exceeds 64 MB");
                    }
                    output.write(buffer, 0, count);
                }
            }
            int exit = process.waitFor();
            if (exit != 0) throw new IOException("System history query failed: "
                    + new String(process.getErrorStream().readAllBytes(), "UTF-8"));
            List<Entry> entries = parse(output.toByteArray());
            for (Entry entry : entries) entry.source = "@system-memory";
            return entries;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
    public static boolean backupIfMissing(String name, File directory) throws Exception {
        if (!name.matches("[0-9]{10,20}")) throw new IllegalArgumentException("Invalid history filename");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create backup directory");
        File target = new File(directory, name);
        if (target.exists()) return false;
        File temporary = File.createTempFile(name + "-", ".tmp", directory);
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", "cat " + DIR + name).start();
            try (InputStream input = process.getInputStream(); FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
            }
            if (process.waitFor() != 0) throw new IOException("Root copy failed for " + name);
            if (target.exists()) return false;
            if (!temporary.renameTo(target)) throw new IOException("Cannot save backup " + name);
            return true;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            if (temporary.exists()) temporary.delete();
        }
    }
    public static class Entry {
        public long time, userId = -1;
        public int iconType = -1, iconResourceId = 0;
        public String pkg="", title="", text="", channelName="", channelId="", conversationId="", iconPackage="", iconUri="", source="";
        public byte[] iconData;
    }
    static class Field { int number, wire; long value; byte[] bytes; }
    static class Reader {
        final byte[] data; int pos, end;
        Reader(byte[] d) { data=d; end=d.length; }
        long num() throws IOException {
            long value=0; for (int i=0;i<10;i++) { if (pos>=end) throw new EOFException(); int b=data[pos++]&255; value |= (long)(b&127) << (7*i); if ((b&128)==0) return value; }
            throw new IOException("无效 varint");
        }
        Field next() throws IOException {
            if (pos>=end) return null; long tag=num(); Field f=new Field(); f.number=(int)(tag>>>3); f.wire=(int)(tag&7);
            if (f.number==0) throw new IOException("无效字段");
            if (f.wire==0) f.value=num();
            else if (f.wire==2) { long len=num(); if (len<0 || len>end-pos) throw new IOException("无效长度"); f.bytes=Arrays.copyOfRange(data,pos,pos+(int)len); pos+=(int)len; }
            else if (f.wire==1 || f.wire==5) { int n=f.wire==1?8:4; if (n>end-pos) throw new IOException("字段截断"); pos+=n; }
            else throw new IOException("不支持的字段类型");
            return f;
        }
    }
    static String str(byte[] b) throws Exception { return new String(b,"UTF-8"); }
    static String pool(List<String> p, long index) { return index>0 && index<=p.size()?p.get((int)index-1):""; }
    public static List<Entry> parse(byte[] data) throws Exception {
        Reader r=new Reader(data); List<String> pool=new ArrayList<>(); List<byte[]> raw=new ArrayList<>(); Field f;
        while ((f=r.next())!=null) {
            if (f.number==1 && f.wire==2) { Reader q=new Reader(f.bytes); Field g; while ((g=q.next())!=null) if (g.number==2 && g.wire==2) pool.add(str(g.bytes)); }
            else if (f.number==3 && f.wire==2) raw.add(f.bytes);
        }
        List<Entry> result=new ArrayList<>();
        for (byte[] bytes:raw) {
            Reader q=new Reader(bytes); Entry e=new Entry();
            long pkgIndex=0, channelNameIndex=0, channelIdIndex=0, conversationIndex=0;
            byte[] iconMessage=null; Field g;
            while ((g=q.next())!=null) {
                if (g.wire==2) {
                    if (g.number==1) e.pkg=str(g.bytes);
                    else if (g.number==3) e.channelName=str(g.bytes);
                    else if (g.number==4) channelNameIndex=g.value;
                    else if (g.number==5) e.channelId=str(g.bytes);
                    else if (g.number==6) channelIdIndex=g.value;
                    else if (g.number==10) e.title=str(g.bytes);
                    else if (g.number==11) e.text=str(g.bytes);
                    else if (g.number==12) iconMessage=g.bytes;
                    else if (g.number==13) e.conversationId=str(g.bytes);
                    else if (g.number==14) conversationIndex=g.value;
                } else if (g.wire==0) {
                    if (g.number==2) pkgIndex=g.value;
                    else if (g.number==7) e.userId=g.value;
                    else if (g.number==8) e.userId=g.value;
                    else if (g.number==9) e.time=g.value;
                }
            }
            if (e.pkg.isEmpty()) e.pkg=pool(pool,pkgIndex);
            if (e.channelName.isEmpty()) e.channelName=pool(pool,channelNameIndex);
            if (e.channelId.isEmpty()) e.channelId=pool(pool,channelIdIndex);
            if (e.conversationId.isEmpty()) e.conversationId=pool(pool,conversationIndex);
            if (iconMessage!=null) {
                Reader iconReader=new Reader(iconMessage); Field iconField;
                while ((iconField=iconReader.next())!=null) {
                    if (iconField.number==1 && iconField.wire==0) e.iconType=(int)iconField.value;
                    else if (iconField.number==2 && iconField.wire==2) e.iconUri=str(iconField.bytes);
                    else if (iconField.number==3 && iconField.wire==0) e.iconResourceId=(int)iconField.value;
                    else if (iconField.number==4 && iconField.wire==2) e.iconPackage=str(iconField.bytes);
                    else if (iconField.number==5 && iconField.wire==2) e.iconData=iconField.bytes;
                }
            }
            result.add(e);
        }
        return result;
    }
}
