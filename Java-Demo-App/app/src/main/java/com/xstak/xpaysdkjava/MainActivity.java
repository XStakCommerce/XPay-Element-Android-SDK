package com.xstak.xpaysdkjava;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.xstak.xpay_element.PaymentElement;
import com.xstak.xpay_element.InputFieldsStyle;
import com.xstak.xpay_element.InputFieldsConfig;
import com.xstak.xpay_element.InvalidStyle;
import com.xstak.xpay_element.LabelStyle;
import com.xstak.xpay_element.OnFocusInputFieldsStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import kotlin.Unit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String publicKey = "your_account_public_api_key";
        String hmacSecretKey = "your_account_hmac_api_key";
        String accountId = "your_account_id";
        ViewGroup container = findViewById(R.id.elementView);
        Button payButton = findViewById(R.id.payButton);
        Button clearButton = findViewById(R.id.clearButton);

        InputFieldsConfig inputConfig = new InputFieldsConfig(
                "1234 1234 1234 1234",
                "Credit Card Number",
                "MM/YY",
                "Expiry Date",
                "CVC",
                "CVC"
        );

        InputFieldsStyle inputFieldStyle = new InputFieldsStyle(
                "#e6e6e6",
                1,
                5,
                18f,
                "#000000"
        );

        OnFocusInputFieldsStyle onFocusFieldStyle = new OnFocusInputFieldsStyle(
                "#C8DBF9",
                5,
                1,
                18f,
                "#000000"
        );

        InvalidStyle onErrorInputFieldStyle = new InvalidStyle(
                "#FF0000",
                5,
                1,
                18f,
                "#FF0000",
                15f,
                "#FF0000"
        );

        LabelStyle labelStyle = new LabelStyle(
                "#000000",
                15f
        );
        PaymentElement paymentElement = new PaymentElement(
                this,
                publicKey,
                hmacSecretKey,
                accountId,
                inputConfig,
                inputFieldStyle,
                onFocusFieldStyle,
                labelStyle,
                onErrorInputFieldStyle
        );

        paymentElement.onReady(isSuccess -> {
            payButton.setEnabled(isSuccess);
            return Unit.INSTANCE;
        });

        paymentElement.onBinDiscount(data -> {
            Log.e("Bin Discount Data", data);
            return Unit.INSTANCE; // And here as well
        });
        container.addView(paymentElement);
        Random random = new Random();
        payButton.setOnClickListener(v -> {
            // for local backend server, replace your_device_ip_address with your device ip address
            String url = "http://your_device_ip_address:4242/create-payment-intent";
            String customerEmail = "amir@gmail.com";
            String customerName = "John Doe";
            String customerPhone = "1234567890";
            String shippingAddress1 = "123 Main St";
            String shippingCity = "Lahore";
            String shippingCountry = "Pakistan";
            String shippingProvince = "Punjab";
            String shippingZip = "54000";
            int sixDigitNumber = 100000 + random.nextInt(900000);  // Ensures it's always 6 digits
            String orderReference = "order-" + sixDigitNumber;
            String jsonPayload = "{\n" +
                    "    \"amount\": 1,\n" +
                    "    \"currency\": \"PKR\",\n" +
                    "    \"payment_method_types\": \"card\",\n" +
                    "    \"customer\": {\n" +
                    "        \"email\": \"" + customerEmail + "\",\n" +
                    "        \"name\": \"" + customerName + "\",\n" +
                    "        \"phone\": \"" + customerPhone + "\"\n" +
                    "    },\n" +
                    "    \"shipping\": {\n" +
                    "        \"address1\": \"" + shippingAddress1 + "\",\n" +
                    "        \"city\": \"" + shippingCity + "\",\n" +
                    "        \"country\": \"" + shippingCountry + "\",\n" +
                    "        \"province\": \"" + shippingProvince + "\",\n" +
                    "        \"zip\": \"" + shippingZip + "\"\n" +
                    "    },\n" +
                    "    \"metadata\": {\n" +
                    "        \"order_reference\": \"" + orderReference + "\"\n" +
                    "    }\n" +
                    "}";
            makeNetworkRequest(url, "POST", jsonPayload, responseBody -> {
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String encpKeyValue = jsonObject.getString("encryptionKey");
                    String KeyValue = jsonObject.getString("clientSecret");
                    paymentElement.confirmPayment(
                            "Test User",
                            KeyValue,
                            encpKeyValue,
                            this::paymentResponse);
                } catch (Exception e) {
                    Log.e("JSON Parse Error", e.toString());
                }
            }, (e, errorBody) -> Log.d("Api Error", e.getMessage()));
        });

        clearButton.setOnClickListener(v -> paymentElement.clear());
    }

     Handler handler = new Handler(Looper.getMainLooper());
    private Unit paymentResponse(String response) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e("Payment Response Data", response);
                    JSONObject jsonObject = new JSONObject(response);
                    String message = jsonObject.getString("message");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return Unit.INSTANCE;
    }

    private void makeNetworkRequest(String url, String method, String bodyJson, CallbackResponse onSuccess, CallbackError onFailure) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES) // Increase the connection timeout
                .readTimeout(5, TimeUnit.MINUTES)    // Increase the read timeout
                .writeTimeout(5, TimeUnit.MINUTES)   // Optional: Increase the write timeout if needed
                .build();
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, bodyJson);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json");

        switch (method.toUpperCase()) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(body);
                break;
            case "PUT":
                builder.put(body);
                break;
            default:
                throw new IllegalArgumentException("HTTP method " + method + " not supported");
        }

        Request request = builder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onFailure.onError(e, null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    onFailure.onError(new IOException("Unexpected code " + response.code()), errorBody);
                } else {
                    onSuccess.onSuccess(response.body().string());
                }
            }
        });
    }

    interface CallbackResponse {
        void onSuccess(String responseBody);
    }

    interface CallbackError {
        void onError(IOException e, String errorBody);
    }
}
