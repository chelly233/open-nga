package gov.anzong.androidnga.core.util;

/**
 * NGA attachment images support server-side preview variants by appending
 * ".medium.jpg" or ".thumb.jpg" to the original URL.
 */
public final class ImagePreviewUtils {

    private ImagePreviewUtils() {
    }

    public static String originalUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceFirst("(http\\S+)\\.(png|jpg|jpeg)\\.(thumb_s|medium|thumb|thumb_ss)\\.jpg", "$1.$2")
                .replaceFirst("(http\\S+)\\.gif\\.(thumb_s|medium|thumb|thumb_ss)\\.jpg", "$1.gif");
    }

    public static String previewUrl(String url) {
        String original = originalUrl(url);
        if (original == null || original.endsWith(".gif")) {
            return original;
        }
        if (original.matches("(?i).*\\.(png|jpg|jpeg)$")) {
            return original + ".medium.jpg";
        }
        return original;
    }
}
