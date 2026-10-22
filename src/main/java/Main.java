public class Main {
    public static void main(String[] args) throws Exception {
        try (Database database = new Database()) {
            String startUrl = "https://example.com";
            Spider crawler = new Spider(startUrl, 100, database);
            crawler.start();
        }
    }
}
