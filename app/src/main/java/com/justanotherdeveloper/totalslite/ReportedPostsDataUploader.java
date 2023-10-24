package com.justanotherdeveloper.totalslite;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class ReportedPostsDataUploader {

    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyYUUXXM2p5v40PMwMFfMuR8DNH7oCqB_2xylnb92fLMahTxigpZYhN2uyZeRkmR20P/exec";
    private static final String ACTION = "addReportedPost";

    public void uploadReportedPostData(Context context, String subject, String message) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, SCRIPT_URL, response -> { }, error -> { }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", ACTION);
                params.put("subject", subject);
                params.put("message", message);
                return params;
            }
        };

        int socketTimeOut = 50000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);
    }
}
