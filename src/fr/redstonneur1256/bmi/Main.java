package fr.redstonneur1256.bmi;

public class Main {

    public static void main(String[] args) throws Throwable {
        // Disable over sized jooq messages
        System.setProperty("org.jooq.no-tips", "true");
        System.setProperty("org.jooq.no-logo", "true");

        BMIRelay server = new BMIRelay();
        server.start();
    }

}
