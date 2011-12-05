package perf;

import java.io.*;

/**
 * Simple manual performance micro-benchmark that compares compress and
 * decompress speeds of this LZF implementation with other codecs.
 */
public final class PerfComparison
{
    private int size = 0;

    private final static class Entry
    {
        
    }

    final static class BogusOutputStream extends OutputStream
    {
        protected int _bytes;
        
        public void write(byte[] buf) { write(buf, 0, buf.length); }
        public void write(byte[] buf, int offset, int len) {
            _bytes += len;
        }

        @Override
        public void write(int b) throws IOException {
            _bytes++;
        }

        public int length() { return _bytes; }
    }
    
    private Entry[] readCsv(byte[] csvInput) throws IOException
    {
        return null;
    }

    private byte[] writeAsJson(Entry[] entries) throws IOException
    {
        return null;
    }
    
    private void test(byte[] csvInput) throws IOException
    {
        final Entry[] entries = readCsv(csvInput);
        final byte[] jsonInput = writeAsJson(entries);
        
        // Let's try to guestimate suitable size... to get to 10 megs to process
        final int REPS = (int) ((double) (10 * 1000 * 1000) / (double) csvInput.length);

        System.out.printf("Input: %d entries; %d bytes as CSV, %d bytes as JSON\n",
                entries.length, csvInput.length, jsonInput.length);
        System.out.printf("Will do %d repetitions per test.\n\n", REPS);

        int i = 0;
        
        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            int round = (i++ % 4);
//            int round = 1;

            String msg;
            boolean lf = (round == 0);

            long msecs;
            
            switch (round) {

            case 0:
                msg = "JSON, read";
                msecs = testJsonRead(REPS, jsonInput);
                break;
            case 1:
                msg = "JSON, write";
                msecs = testJsonWrite(REPS, entries);
                break;
            case 2:
                msg = "CSV, read";
                msecs = testCsvRead(REPS, csvInput);
                break;
            case 3:
                msg = "CSV, write";
                msecs = testCsvWrite(REPS, entries);
                break;
            default:
                throw new Error();
            }
            
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' ["+size+" bytes] -> "+msecs+" msecs");
        }
    }

    private final long testJsonRead(int REPS, byte[] input) throws IOException
    {
        long start = System.currentTimeMillis();
        byte[] comp = null;
        while (--REPS >= 0) {
        }
        size = comp.length;
        return System.currentTimeMillis() - start;
    }

    private final long testCsvRead(int REPS, byte[] input) throws IOException
    {
        long start = System.currentTimeMillis();
        byte[] comp = null;
        while (--REPS >= 0) {
        }
        size = comp.length;
        return System.currentTimeMillis() - start;
    }
    
    private final long testJsonWrite(int REPS, Entry[] entries) throws IOException
    {
        long start = System.currentTimeMillis();
        while (--REPS >= 0) {
            BogusOutputStream bogus = new BogusOutputStream();
            size = bogus.length();
        }
        return System.currentTimeMillis() - start;
    }
    
    private final long testCsvWrite(int REPS, Entry[] entries) throws IOException
    {
        long start = System.currentTimeMillis();
        while (--REPS >= 0) {
            BogusOutputStream bogus = new BogusOutputStream();
            size = bogus.length();
        }
        return System.currentTimeMillis() - start;
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [file]");
            System.exit(1);
        }
        new PerfComparison().test(readAll(args[0]));
    }

    public static byte[] readAll(String filename) throws IOException
    {
        File f = new File(filename);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) f.length());
        byte[] buffer = new byte[4000];
        int count;
        FileInputStream in = new FileInputStream(f);
        
        while ((count = in.read(buffer)) > 0) {
            bytes.write(buffer, 0, count);
        }
        in.close();
        return bytes.toByteArray();
    }
}
