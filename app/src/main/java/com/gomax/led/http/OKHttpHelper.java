package com.gomax.led.http;

import android.util.Log;

import com.gomax.led.fragment.FragmentHelper;
import com.gomax.led.fragment.FragmentSettings;
import com.gomax.led.fragment.FragmentSystem;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OKHttpHelper {

    private static final String TAG = "OKHttpHelper";
    private OkHttpClient client;
    private String url = "";
    private int cmdNumber = 0;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OKHttpHelper(String deviceIP, String cmd, int cmdNumber) {
        this.cmdNumber = cmdNumber;
        this.url = "http://" + deviceIP + ":8080/" + cmd;
        this.client = new OkHttpClient();
        Log.d(TAG, "url = " + this.url);
    }

    /**
     * Restful GET Method
     *
     * @ : response json
     */
    public void methodPost(final String jsonBody) {
        RequestBody body = RequestBody.Companion.create(jsonBody, JSON);
        Call call = this.client.newCall(new Request.Builder().url(url).post(body).build());
        call.enqueue(new Callback() {

            public void onResponse(Call call, Response response) throws IOException {

                switch (cmdNumber) {
                    case FragmentHelper.FRAGMENT_EVENT_POST_HOSTNAME:
                        FragmentSettings.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_POST_HOSTNAME, 0, -1, response.body().string()).sendToTarget();
                        break;
                }
            }

            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "failed " + e.getMessage());
                switch (cmdNumber) {
                    case FragmentHelper.FRAGMENT_EVENT_POST_HOSTNAME:
                        FragmentSettings.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_POST_HOSTNAME, 0, -1, "{\"result\": \"failed\"\n}").sendToTarget();
                        break;
                }
            }
        });
    }


    /**
     * Restful POST Method
     *
     * @ : response json
     */
    public void methodGet() {

        Call call = this.client.newCall(new Request.Builder().url(url).get().build());
        call.enqueue(new Callback() {

            public void onResponse(Call call, Response response) throws IOException {

                switch (cmdNumber) {
                    case FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS:
                        FragmentSettings.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS, 0, -1, response.body().string()).sendToTarget();
                        break;
                    case FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM:
                        FragmentSystem.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM, 0, -1, response.body().string()).sendToTarget();
                        break;
                }
            }

            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "failed " + e.getMessage());
                switch (cmdNumber) {
                    case FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS:
                        FragmentSettings.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS, 0, -1, "{\"result\": \"failed\"\n}").sendToTarget();
                        break;
                    case FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM:
                        FragmentSystem.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM, 0, -1, "{\"result\": \"failed\"\n}").sendToTarget();
                        break;
                }
            }
        });
    }
}


