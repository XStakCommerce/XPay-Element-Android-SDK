package com.xstak.xpaykotlinsdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.xstak.xpay_element.InputFieldsStyle
import com.xstak.xpay_element.InputFieldsConfig
import com.xstak.xpay_element.PaymentElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val publicKey: String =
            "your_account_public_api_key"
        val hamcSecretKey: String =
            "your_account_hmac_api_key"
        val accountID: String = "your_account_id"
        val container: ViewGroup = findViewById(R.id.elementView)
        val payButton: Button = findViewById(R.id.payButton)
        val clearButton: Button = findViewById(R.id.clearButton)
        val config = InputFieldsConfig(
            creditCardPlaceholder = "0000 0000 0000 0000",
            creditCardLabel = "Card Number",
            expiryPlaceholder = "MM/YY",
            expiryLabel = "Expiry Date",
            cvcPlaceholder = "CVC",
            cvcLabel = "CVC"
        )
        val styleConfig = InputFieldsStyle(
            borderColor = "#e6e6e6",
            borderWidth = 1,
            borderRadius = 5,
            textSize = 18f,
            textColor = "#000000",
        )

        val paymentElement = PaymentElement(
            this,
            publicKey = publicKey,
            hmacSecretKey = hamcSecretKey,
            accountId = accountID,
            inputFieldsConfig = config, inputFieldsStyle = styleConfig
        )
        paymentElement.onReady { isSuccess ->
            payButton.isEnabled = isSuccess
        }
        paymentElement.onBinDiscount { data ->
            runOnUiThread {
                Toast.makeText(this, data, Toast.LENGTH_LONG).show()
            }
        }
        container.addView(paymentElement)
        payButton.setOnClickListener {
            // for local backend server, replace your_device_ip_address with your device ip address
            val url = "http://your_device_ip_address:4242/create-payment-intent"
            val customerEmail = "amir@gmail.com"
            val customerName = "John Doe"
            val customerPhone = "1234567890"
            val shippingAddress1 = "123 Main St"
            val shippingCity = "lahore"
            val shippingCountry = "pakistan"
            val shippingProvince = "punjab"
            val shippingZip = "54000"
            val randomDigits = Random.nextInt(100000, 999999)
            val orderReference = "order-$randomDigits"
            val jsonPayload = """
{
    "amount": 1,
    "currency": "PKR",
    "payment_method_types": "card",
    "customer": {
        "email": "$customerEmail",
        "name": "$customerName",
        "phone": "$customerPhone"
    },
    "shipping": {
        "address1": "$shippingAddress1",
        "city": "$shippingCity",
        "country": "$shippingCountry",
        "province": "$shippingProvince",
        "zip": "$shippingZip"
    },
    "metadata": {
        "order_reference": "$orderReference"
    }
}
""".trimIndent()

            fun paymentResponse(response: String) {
                runOnUiThread {
                    Toast.makeText(this, response, Toast.LENGTH_LONG).show()
                }

            }

            makeNetworkRequest(
                url = url,
                method = "POST",
                bodyJson = jsonPayload,
                onSuccess = { responseBody ->
                    Log.d("Create Intent response",responseBody)
                    val dataObject = JSONObject(responseBody)
                    val encpKeyValue = dataObject.getString("encryptionKey")
                    val KeyValue = dataObject.getString("clientSecret")
                    paymentElement.confirmPayment(
                        "Amir Ghafoor",
                        KeyValue,
                        encpKeyValue,
                        callback = { response -> paymentResponse(response) }
                    )
                },
                onFailure = { error, errorBody ->
                    Log.d("Api Error", error.message.toString())
                    // Now you also have access to the errorBody, which is the server's response body for errors
                    errorBody?.let {
                        println("Server error response: $it")
                    }
                }
            )
        }
        clearButton.setOnClickListener {
            paymentElement.clear()
        }
    }

    fun makeNetworkRequest(
        url: String,
        method: String = "GET",
        bodyJson: String? = null,
        onSuccess: (responseBody: String) -> Unit,
        onFailure: (e: IOException, Any?) -> Unit
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES) // Increase the connection timeout
            .readTimeout(5, TimeUnit.MINUTES)    // Increase the read timeout
            .writeTimeout(5, TimeUnit.MINUTES)   // Optional: Increase the write timeout if needed
            .build()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = bodyJson?.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .apply {
                when (method.uppercase()) {
                    "GET" -> get()
                    "POST" -> body?.let { post(it) }
                    "PUT" -> body?.let { put(it) }
                    else -> throw IllegalArgumentException("HTTP method $method not supported")
                }
            }
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e, null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    onFailure(IOException("Unexpected code ${response.code}"), errorBody)
                } else {
                    onSuccess(response.body?.string() ?: "")
                }
            }
        })
    }
}