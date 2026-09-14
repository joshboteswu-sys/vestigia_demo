package com.vestigia.app.network;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

// Sends a multipart/form-data POST: text fields + one optional image file.
public class VolleyMultipartRequest extends Request<JSONObject> {

    private final String boundary = "vestigia-boundary-" + System.currentTimeMillis();
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";

    private final Response.Listener<JSONObject> listener;
    private final Response.ErrorListener errorListener;
    private final Map<String, String> textParams;
    private final String token;

    private String fileFieldName;
    private byte[] fileData;
    private String fileName;
    private String fileMimeType;

    public VolleyMultipartRequest(String url, String token, Map<String, String> textParams,
                                  Response.Listener<JSONObject> listener,
                                  Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        this.token = token;
        this.textParams = textParams != null ? textParams : new HashMap<>();
        this.listener = listener;
        this.errorListener = errorListener;
    }

    // Call this before adding the request to the queue, if there's a photo to upload.
    public void setFile(String fieldName, byte[] data, String fileName, String mimeType) {
        this.fileFieldName = fieldName;
        this.fileData = data;
        this.fileName = fileName;
        this.fileMimeType = mimeType;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // text fields
            for (Map.Entry<String, String> entry : textParams.entrySet()) {
                bos.write((twoHyphens + boundary + lineEnd).getBytes());
                bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes());
                bos.write(lineEnd.getBytes());
                bos.write((entry.getValue() != null ? entry.getValue() : "").getBytes(Charset.forName("UTF-8")));
                bos.write(lineEnd.getBytes());
            }

            // optional file
            if (fileData != null) {
                bos.write((twoHyphens + boundary + lineEnd).getBytes());
                bos.write(("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + fileName + "\"" + lineEnd).getBytes());
                bos.write(("Content-Type: " + fileMimeType + lineEnd).getBytes());
                bos.write(lineEnd.getBytes());
                bos.write(fileData);
                bos.write(lineEnd.getBytes());
            }

            bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, "utf-8"));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | org.json.JSONException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }
}