public class Main {
    public static void main(String[] args) {
        String startUrl = "https://example.com";
        Spider crawler = new Spider(startUrl, 100);
        crawler.start();
    }
}
