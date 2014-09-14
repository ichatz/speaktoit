package com.sensorflare.speaktoit;

import ai.api.AIConfiguration;
import ai.api.AIServiceException;
import ai.api.GsonFactory;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

/**
 * Main controller.
 *
 * @author ichatz@gmail.com
 */
@Controller
public class HomeController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(final Model model) {
        return "home";
    }

    /**
     * The controller used to forward commands in natural language to speaktoit api.ai.
     *
     * @param model the map containing the model.
     * @return the corresponding view.
     */
    @RequestMapping(value = "/speaktoit", method = RequestMethod.POST)
    public String postCreate(final Map<String, Object> model,
                             @RequestParam final String textcommand)
            throws Exception {

        final AIConfiguration config = new AIConfiguration("f3f26396409944c18ac38802184fbc43",
                "25dd818b-d289-460c-b62f-397ffaf3abfe",
                "en-US", AIConfiguration.RecognitionEngine.Google);

        final AIRequest aiRequest = new AIRequest();
        aiRequest.setQuery(textcommand);

        AIResponse aiResponse = request(config, aiRequest);
        System.err.println(aiResponse.getResult().toString());
        model.put("result", aiResponse.getResult().getAction());
        return "response";
    }

    public static AIResponse request(final AIConfiguration config, final AIRequest request) throws AIServiceException {
        if (request == null) {
            throw new IllegalArgumentException("Request argument must not be null");
        }

        final Gson gson = GsonFactory.getGson();

        HttpURLConnection connection = null;

        try {
            final URL url = new URL(config.getQuestionUrl());

            request.setLanguage(config.getLanguage());
            request.setAgentId(config.getAgentId());
            request.setTimezone(Calendar.getInstance().getTimeZone().getID());

            final String queryData = gson.toJson(request);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.addRequestProperty("Authorization", "Bearer " + config.getApiKey());
            connection.addRequestProperty("ocp-apim-subscription-key", config.getSubscriptionKey());
            connection.addRequestProperty("Content-Type","application/json; charset=utf-8");
            connection.addRequestProperty("Accept","application/json");

            connection.connect();

            final BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            IOUtils.write(queryData, outputStream, "UTF-8");
            outputStream.close();

            final InputStream inputStream = new BufferedInputStream(connection.getInputStream());
            final String response = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();

            if (response.isEmpty()) {
                throw new AIServiceException("Empty response from ai service. Please check configuration.");
            }

            System.err.println(response);

            final AIResponse aiResponse = gson.fromJson(response, AIResponse.class);
            return aiResponse;

        } catch (final MalformedURLException e) {
            System.err.println("Malformed url should not be raised");
            e.printStackTrace();
            throw new AIServiceException("Wrong configuration. Please, connect to AI Service support", e);
        } catch (final IOException e) {
            System.err.println("Can't make request to the Speaktoit AI service. Please, check connection settings and API access token.");
            e.printStackTrace();
            throw new AIServiceException("Can't make request to the AI service. Please, check connection settings and API access token.", e);
        } catch (final JsonSyntaxException je) {
            je.printStackTrace();
            throw new AIServiceException("Wrong service answer format. Please, connect to AI Service support", je);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

}
