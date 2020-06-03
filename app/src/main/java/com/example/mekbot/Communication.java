package com.example.mekbot;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseAndJSONObjectRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import okhttp3.Response;

/** @Rambir
 * Writing for chatbot
 * This class can handle communication to and from our cloud chatbot.
 * It is made singleton to make sure all communication is made to a single Bot.
 */
public class Communication {

    // mekbot api address
    private static final String MekbotUrl = "https://rambirchatbot.herokuapp.com/";

    // singleton instance of communication
    private static volatile Communication sSoleInstance;
    private static String sessionID;


    // private constructor.
    private Communication(){

        // prevent form the reflection api.
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static Communication getInstance() {
        if (sSoleInstance == null) { // if there is no instance available... create new one
            synchronized (Communication.class) {
                if (sSoleInstance == null) {
                    sSoleInstance = new Communication();
                    sessionID = UUID.randomUUID().toString();
                }
            }
        }

        return sSoleInstance;
    }

    static void toChatbot(Communication instance, JSONArray previousContext, String query, CompleteListener listener) {

        if(!instance.equals(sSoleInstance)){

            // this won't executive unless someone knowingly made effort to trigger this
            listener.Error(new RuntimeException("This instance is not acceptable !!"));
            return;
        }

        JSONObject inputJson = new JSONObject();

        try {
            inputJson.put("question", query);
            inputJson.put("context", previousContext);
            inputJson.put("sessionId",sessionID);

        } catch (JSONException e) {
            listener.Error(e);
            return;
        }

        AndroidNetworking.post(MekbotUrl)
                .addHeaders("Content-Type","application/json")
                .addHeaders("Authorization","Bearer djkfhjsdfjhdfjkdf")
                .addJSONObjectBody(inputJson)
                .setPriority(Priority.HIGH)
                .build()
                .getAsOkHttpResponseAndJSONObject(new OkHttpResponseAndJSONObjectRequestListener() {
                    @Override
                    public void onResponse(Response okHttpResponse, JSONObject response) {
                        // process the response
                        JSONArray textMessages = new JSONArray();
                        JSONArray quickReplies = new JSONArray();
                        JSONObject payload = new JSONObject();

                        try{
                            if(response.has("queryResult")){
                                JSONObject result = response.getJSONObject("queryResult");

                                // it must have fulfillmentMessages otherwise the bot hasn't replied
                                if(result.has("fulfillmentMessages")) {
                                    JSONArray fulfillmentMessages = result.getJSONArray("fulfillmentMessages");

                                    for (int i = 0; i < fulfillmentMessages.length(); i++) {
                                        JSONObject fulfillmentMessage = fulfillmentMessages.getJSONObject(i);
                                        String messageType = fulfillmentMessage.getString("message");

                                        switch (messageType) {
                                            case "text":
                                                textMessages.put(
                                                        fulfillmentMessage.getJSONObject(messageType)
                                                                .getJSONArray(messageType)
                                                                .get(0)
                                                );
                                                break;

                                            case "quickReplies":
                                                quickReplies =
                                                        fulfillmentMessage.getJSONObject(messageType)
                                                                .getJSONArray(messageType);
                                                break;

                                            case "payload":
                                                payload = fulfillmentMessage.getJSONObject(messageType);
                                                break;

                                            default:
                                        }
                                    }

                                    listener.TextMessages(textMessages);
                                    listener.QuickReplies(quickReplies);
                                    listener.CustomPayload(payload);
                                }

                                if(result.has("outputContexts"))
                                    // we got outputContexts
                                    listener.Contexts(result.getJSONArray("outputContexts"));
                            }
                            else{
                                listener.Error(new Exception("this is not Mekbot's query result"));
                            }

                        } catch (JSONException e){
                            // let user know about error
                            listener.Error(e);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        // let user know about error
                        listener.Error(anError);
                    }
                });
    }


    interface CompleteListener {
        void Error(Exception error);
        void TextMessages(JSONArray fulfillmentMessages);
        void QuickReplies(JSONArray quickReplies);
        void CustomPayload(JSONObject payload);
        void Contexts(JSONArray outputContexts);
    }
}
