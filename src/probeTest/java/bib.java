import java.nio.file.*;
import java.util.List;

/** Minimal obfuscated-signature fixture for an actual JVM -javaagent launch. */
public final class bib {
    public Object f = new Object();
    public Object m;
    public void az() { }
    public static void main(String[] args) throws Exception {
        bib fixture = new bib();
        for (int i = 0; i < 150; i++) {
            fixture.m = i >= 30 && i < 60 ? new Object() : null;
            fixture.az(); Thread.sleep(10);
        }
        Thread.sleep(1500);
        List<String> rows = Files.readAllLines(Paths.get(args[0], "smoke-frames.csv"));
        if (rows.size() < 40) throw new AssertionError("Probe did not record enough intervals");
        boolean gui = false, gameplay = false;
        for (String row : rows.subList(1, rows.size())) {
            String[] fields = row.split(",");
            if (Long.parseLong(fields[1]) <= 0) throw new AssertionError("Non-positive interval");
            gui |= fields[3].equals("true"); gameplay |= fields[3].equals("false");
        }
        if (!gui || !gameplay) throw new AssertionError("Menu boundary tagging failed");
        System.out.println("Probe JVM integration passed: " + (rows.size() - 1) + " intervals, both GUI and gameplay tagged.");
    }
}
