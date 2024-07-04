const express = require("express");
const bodyParser = require("body-parser");
const crypto = require("crypto");
const axios = require("axios");
const cors = require("cors");

const SECRET_KEY = "your_account_secret_api_key";
const HMAC_SECRET = "your_account_hmac_api_key";
const ACCOUNT_ID = "your_account_id";
const XPAY_BASE_URL = "https://xstak-pay.xstak.com";
const requestInterceptor = (req, res, buffer) => (req.rawBody = buffer);
(async () => {
  try {
    const app = express();
    const PORT = 4242;
    app.use(bodyParser.json({ verify: requestInterceptor, limit: "5mb" }));
    app.use(cors());
    app.use(express.json());
    app.use(express.urlencoded({ extended: true }));

    // route
    app.post("/create-payment-intent", async (req, res, next) => {
      try {
        const payload = {
          amount: 10,
          currency: "PKR",
          customer: {
            email: "xpay@xstak.com", //required
            name: "DeJon", //required
            phone: "", //required
          },
          shipping: {
            address1: "lahore", //required
            city: "lahore", //required
            country: "29", //required
            province: "punjab", //required
            zip: "2222", //required
          },
          metadata: {
            order_reference: "", // order id optional
          },
        };
        const signature = crypto
          .createHmac("SHA256", HMAC_SECRET)
          .update(JSON.stringify(req.body))
          .digest("hex");
        try {
          const paymentIntent = await axios.post(
            `${XPAY_BASE_URL}/public/v1/payment/intent`,
            req.body,
            {
              headers: {
                "x-api-key": SECRET_KEY,
                "Content-Type": "application/json",
                "x-signature": signature,
                "x-account-id": ACCOUNT_ID,
              },
            }
          );
          console.log(
            "Response send : ",
            JSON.stringify(paymentIntent?.data?.data)
          );
          res.json({
            encryptionKey: paymentIntent?.data?.data?.encryptionKey,
            clientSecret: paymentIntent?.data?.data?.pi_client_secret,
          });
        } catch (err) {
          throw err;
        }
      } catch (err) {
        console.log("ERROR : ", JSON.stringify(err));
        next(err);
      }
    });

    const ErrorHandler = (err, req, res, next) => {
      const errStatus = err?.statusCode || err?.status || 500;
      const errMsg = err?.message || "Something went wrong";
      res.status(errStatus).json({
        success: false,
        status: errStatus,
        message: errMsg,
      });
      next();
    };
    app.use(ErrorHandler);
    app.listen(PORT, (error) => {
      if (!error)
        console.log(
          "Server is Successfully Running,and App is listening on port " + PORT
        );
      else console.log("Error occurred, server can't start", error);
    });
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
})();
