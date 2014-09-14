package ai.api;

/***********************************************************************************************************************
 *
 * API.AI Android SDK - client-side libraries for API.AI
 * =================================================
 *
 * Copyright (C) 2014 by Speaktoit, Inc. (https://www.speaktoit.com)
 * https://www.api.ai
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************/

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

/**
 * Do simple requests to the AI Service
 */
public class AIDataService {

    public static final String TAG = AIDataService.class.getName();

    private final AIConfiguration config;

    public AIDataService(final AIConfiguration config) {
        this.config = config;
    }

    /**
     * Make request to the ai service. This method must not be called in the UI Thread
     *
     * @param request request object to the service
     * @return response object from service
     */
    public AIResponse request(final AIRequest request) throws AIServiceException {
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
