import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spider {
    private final Queue <String> frontier = new LinkedList<>();
    private final Set <String> visited = new HashSet<>();
    private final HttpClient client;
    private final String domain;
    private final int maxPages;

    public Spider(String startUrl, int maxPages) {
        frontier.add(startUrl);
        URI uri = URI.create(startUrl);
        domain = uri.getHost();
        this.maxPages = maxPages;
        client = HttpClient.newHttpClient();
    }

    public void start(){
        int pagesCrawled = 0;
        while (!frontier.isEmpty() && pagesCrawled < maxPages){
            String url = frontier.poll();
            if (visited.contains(url)) continue;
            if (!isSameDomain(url)) continue;

            System.out.println();
            System.out.println("Crawling: " + url);
            visited.add(url);

            try {
                String html = downloadPage(url);
                System.out.println("Downloaded: " + html.length() + " characters");
                Set <String> links = extractLinks(html, url);
                System.out.println("Links found: " + links.size());
                for (String link : links) {
                    if (!visited.contains(link) && isSameDomain(link)) frontier.add(link);
                }
                pagesCrawled++;

            } catch (Exception e) {
                System.out.println("Failed: " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("Crawl finished");
        System.out.println("Pages crawled: " + pagesCrawled);
        System.out.println("URLs visited:  " + visited.size());
        System.out.println("URLs waiting:  " + frontier.size());
    }

    private String downloadPage (String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "MyCrawler/1.0").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
        return response.body();
    }

    private Set <String> extractLinks(String html, String currentUrl) {
        Set <String> links = new HashSet<>();
        Pattern pattern = Pattern.compile("<a\\s+[^>]*href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String link = matcher.group(1);
            try {
                URI base = URI.create(currentUrl);
                URI resolved = base.resolve(link);
                String normalized = normalizeUrl(resolved.toString());
                if (normalized != null) links.add(normalized);
            } catch(Exception ignored){}
        }
        return links;
    }

    private String normalizeUrl(String url){
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null) return null;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return null;
            return new URI(scheme.toLowerCase(),uri.getUserInfo(),uri.getHost(),uri.getPort(),uri.getPath(),uri.getQuery(),null).toString();
        }
        catch (Exception e){
            return null;
        }
    }

    private boolean isSameDomain(String url) {
        try {
            URI uri = URI.create(url);
            return domain.equalsIgnoreCase(uri.getHost());
        }
        catch (Exception e) {
            return false;
        }
    }
}
