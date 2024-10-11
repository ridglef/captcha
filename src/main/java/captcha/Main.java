package captcha;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private static final Gson gson = new Gson();
    private static final Random random = ThreadLocalRandom.current();
    private static final int range = 20;

    public static String getNearbyMcdonaldsLocationUrl() {
        String jsonResponse;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("http://ip-api.com/json/").openStream()))) {
            jsonResponse = reader.readLine();
        } catch (Exception e) {
            return "Failed";
        }

        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
        if (jsonObject == null) return "Failed";

        Float lat = jsonObject.has("lat") ? jsonObject.get("lat").getAsFloat() : null;
        Float lon = jsonObject.has("lon") ? jsonObject.get("lon").getAsFloat() : null;

        if (lat == null || lon == null) return "Failed";

        return String.format("https://www.mcdonalds.com/googleappsv2/geolocation?latitude=%s&longitude=%s&radius=" + range + "&maxResults=1&country=us&language=en-us", URLEncoder.encode(lat.toString(), StandardCharsets.UTF_8), URLEncoder.encode(lon.toString(), StandardCharsets.UTF_8));
    }

    public static BufferedImage buildCaptcha(String str) {
        int width = 800;
        int height = 800;

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bufferedImage.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        for (int i = 0; i < width; i++) {
            g2d.setColor(random.nextBoolean() ? Color.WHITE : Color.LIGHT_GRAY);
            g2d.fillRect(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 34));
        g2d.rotate(Math.toRadians(random.nextBoolean() ? random.nextInt(21) : -random.nextInt(21)));
        g2d.setColor(Color.BLACK);

        g2d.drawString(str, 350, 350);

        g2d.setColor(Color.LIGHT_GRAY);

        for (int i = 0; i < 800; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.dispose();

        return bufferedImage;
    }

    public static void main(String[] args) throws IOException {
        String jsonString;

        try {
            URL url = new URL(getNearbyMcdonaldsLocationUrl());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                jsonString = reader.readLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch or read McDonalds data", e);
        }

        JsonObject jsonResponse = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray featuresArray = jsonResponse.getAsJsonArray("features");

        if (featuresArray != null && !featuresArray.isEmpty()) {
            JsonObject firstFeature = featuresArray.get(0).getAsJsonObject();
            JsonObject properties = firstFeature.getAsJsonObject("properties");

            String mcAddress = properties.get("addressLine1").getAsString();
            String mcHours = properties.get("todayHours").getAsString();

            char upperCaseRandom = (char) (65 + random.nextInt(26));
            char lowerCaseRandom = ((char) (97 + random.nextInt(26)));

            BufferedImage captcha = buildCaptcha(mcAddress + upperCaseRandom + lowerCaseRandom);

            ImageIO.write(captcha, "png", new File("captcha.png"));

            System.out.println("Captcha Created");
            System.out.println("Instructions: Find the Opening Hour of the listed McDonalds and append the 2 last characters to the end. (CASE SENSITIVE)");
            System.out.println("Answer: " + mcHours.substring(0,2) + upperCaseRandom + lowerCaseRandom);
        }
    }
}