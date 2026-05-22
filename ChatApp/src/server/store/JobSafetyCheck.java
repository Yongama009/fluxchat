package server.store;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class JobSafetyCheck {
    private static final Set<String> SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly"
    );

    private static final List<String> BLOCKED_PHRASES = List.of(
            "application fee",
            "registration fee",
            "pay before",
            "pay upfront",
            "send money",
            "whatsapp only",
            "telegram",
            "investment required",
            "no interview",
            "guaranteed job",
            "bank details",
            "copy of your bank card"
    );

    private JobSafetyCheck() {
    }

    public static List<String> validate(String title,
                                        String company,
                                        String description,
                                        String sourceUrl) {
        List<String> problems = new ArrayList<>();
        String normalizedUrl = sourceUrl == null ? "" : sourceUrl.trim();

        if (!normalizedUrl.isBlank()) {
            validateUrl(normalizedUrl, company, problems);
        }

        String fullText = (title + " " + company + " " + description).toLowerCase(Locale.ROOT);
        for (String phrase : BLOCKED_PHRASES) {
            if (fullText.contains(phrase)) {
                problems.add("Job text contains suspicious phrase: " + phrase);
            }
        }

        if (description.length() < 30) {
            problems.add("Description is too short to verify basic job details.");
        }

        return problems;
    }

    private static void validateUrl(String sourceUrl, String company, List<String> problems) {
        URI uri;
        try {
            uri = new URI(sourceUrl);
        } catch (URISyntaxException e) {
            problems.add("Source link is not a valid URL.");
            return;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
            problems.add("Source link must start with http:// or https://.");
        }

        if (host == null || host.isBlank()) {
            problems.add("Source link must include a valid domain.");
            return;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (SHORTENERS.contains(normalizedHost)) {
            problems.add("Shortened links are not allowed for job posts.");
        }

        String companyToken = company.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String hostToken = normalizedHost.replaceAll("[^a-z0-9]", "");
        boolean isKnownJobBoard = normalizedHost.endsWith("linkedin.com")
                || normalizedHost.contains(".linkedin.com")
                || normalizedHost.endsWith("indeed.com")
                || normalizedHost.endsWith("pnet.co.za")
                || normalizedHost.endsWith("careers24.com");

        if (companyToken.length() >= 4 && !isKnownJobBoard && !hostToken.contains(companyToken)) {
            problems.add("Source link domain does not appear to match the company name.");
        }
    }
}
