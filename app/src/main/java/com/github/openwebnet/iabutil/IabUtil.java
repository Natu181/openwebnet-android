package com.github.openwebnet.iabutil;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.annimon.stream.Stream;
import com.github.openwebnet.BuildConfig;
import com.github.openwebnet.R;
import com.github.openwebnet.iabutil.IabHelper.IabAsyncInProgressException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author niqdev
 */
public class IabUtil {

    private static final Logger log = LoggerFactory.getLogger(IabUtil.class);

    private static IabUtil mIabUtil;
    private Activity mActivity;
    private IabHelper mHelper;

    private IabUtil() {
        // fake instance if base64EncodedPublicKey is missing
    }

    private IabUtil(Activity activity) {
        this.mActivity = activity;
    }

    /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console).
     * This is not your developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program, construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key. The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
    private static final String base64EncodedPublicKey = BuildConfig.IAB_KEY;

    private static final String SKU_COFFEE = "coffee";
    private static final String SKU_BEER = "beer";
    private static final String SKU_PIZZA = "pizza";

    private final List<String> skus = ImmutableList.of(
        //SKU_TEST_PURCHASED, SKU_TEST_CANCELED, SKU_TEST_REFUNDED, SKU_TEST_ITEM_UNAVAILABLE,
        SKU_COFFEE, SKU_BEER, SKU_PIZZA);
    private final Map<String, DonationEntry> donations = new HashMap<>();

    // RUN ~/Android/Sdk/platform-tools/adb shell pm clear com.android.vending
    private static final String SKU_TEST_PURCHASED = "android.test.purchased";
    private static final String SKU_TEST_CANCELED = "android.test.canceled";
    private static final String SKU_TEST_REFUNDED = "android.test.refunded";
    private static final String SKU_TEST_ITEM_UNAVAILABLE = "android.test.item_unavailable";

    private static final boolean DEBUG_IAB = false;

    private static boolean isInvalidIabKey() {
        return TextUtils.isEmpty(Strings.emptyToNull(base64EncodedPublicKey));
    }

    /**
     *
     */
    public static IabUtil newInstance(Activity activity) {
        if (mIabUtil != null) {
            // singleton
            return mIabUtil;
        }

        if (isInvalidIabKey()) {
            log.warn("missing IAB_KEY: fake instance");
            mIabUtil = new IabUtil();
        } else {
            log.debug("found IAB_KEY: new instance");
            mIabUtil = new IabUtil(activity);
        }

        return mIabUtil;
    }

    /**
     *
     */
    public static IabUtil getInstance() {
        Preconditions.checkNotNull(mIabUtil, "iab is not instantiated");
        return mIabUtil;
    }

    /**
     *
     */
    public void init() {
        if (isInvalidIabKey()) {
            log.warn("missing IAB_KEY: do nothing");
            return;
        }

        mIabUtil.mHelper = new IabHelper(mActivity, base64EncodedPublicKey);
        mHelper.enableDebugLogging(DEBUG_IAB);

        mHelper.startSetup(result -> {

            if (!result.isSuccess()) {
                log.error("Problem setting up in-app billing: {}", result);
                return;
            }

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            log.debug("in-app billing setup successful: querying inventory");
            try {
                mHelper.queryInventoryAsync(true, skus, null, mGotInventoryListener);
            } catch (Throwable e) {
                // https://github.com/openwebnet/openwebnet-android/issues/77
                log.error("Error querying inventory. Another async operation in progress.");
            }
        });
    }

    private IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                log.error("Failed to query inventory: {}", result);
                return;
            }

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See verifyDeveloperPayload().
             */

            if (inventory != null) {
                Stream.of(skus).forEach(sku -> donations.put(sku, consumableDonation(inventory, sku)));
            }
        }
    };

    private DonationEntry consumableDonation(Inventory inventory, String sku) {
        Preconditions.checkArgument(inventory.hasDetails(sku), "missing sku details");
        SkuDetails details = inventory.getSkuDetails(sku);

        boolean isPurchased = false;
        if (inventory.hasPurchase(sku)) {
            Purchase purchase = inventory.getPurchase(sku);
            isPurchased = (purchase != null && verifyDeveloperPayload(purchase));
            log.debug("User has purchased {}: {}", sku, isPurchased);
            consumeItem(purchase);
        }

        return new DonationEntry.Builder(details.getSku())
            .name(details.getTitle())
            .description(details.getDescription())
            .price(details.getPrice())
            .currencyCode(details.getPriceCurrencyCode())
            .purchased(isPurchased)
            .build();
    }

    // TODO
    private boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * Verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    /**
     *
     */
    public List<DonationEntry> getDonationEntries() {
        if (isInvalidIabKey()) {
            log.warn("missing IAB_KEY: do nothing");
            return Lists.newArrayList();
        }

        //return new ArrayList<>(donations.values());

        // show entries also if there is no internet connection and prices tax excluded
        final String CURRENCY = "€";
        return Lists.newArrayList(
            new DonationEntry.Builder(SKU_COFFEE)
                .name(mActivity.getString(R.string.donation_coffee_name))
                .description(mActivity.getString(R.string.donation_coffee_description))
                .price("1")
                .currencyCode(CURRENCY)
                .build(),
            new DonationEntry.Builder(SKU_BEER)
                .name(mActivity.getString(R.string.donation_beer_name))
                .description(mActivity.getString(R.string.donation_beer_description))
                .price("2")
                .currencyCode(CURRENCY)
                .build(),
            new DonationEntry.Builder(SKU_PIZZA)
                .name(mActivity.getString(R.string.donation_pizza_name))
                .description(mActivity.getString(R.string.donation_pizza_description))
                .price("5")
                .currencyCode(CURRENCY)
                .build()
        );
    }

    /**
     *
     */
    public void purchase(String sku) {
        if (isInvalidIabKey()) {
            log.warn("missing IAB_KEY: do nothing");
            return;
        }

        // if we were disposed of in the meantime, quit.
        if (mHelper == null || !mHelper.mSetupDone) return;

        log.debug("Launching purchase flow for donation.");

        /* TODO
         * (arbitrary) request code for the purchase flow
         */
        final int RC_REQUEST = 10001;

        /* TODO
         * for security, generate your payload here for verification. See the comments on
         * verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         * an empty string, but on a production app you should carefully generate this.
         */
        final String payload = "";

        try {
            mHelper.launchPurchaseFlow(mActivity, sku, RC_REQUEST,  mPurchaseFinishedListener, payload);
        } catch (IabAsyncInProgressException e) {
            log.error("Error launching purchase flow. Another async operation in progress.");
        }
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            log.debug("Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                log.error("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                log.error("Error purchasing. Authenticity verification failed.");
                return;
            }

            log.debug("Purchase successful: {}", purchase.getSku());

            consumeItem(purchase);
        }
    };

    private void consumeItem(Purchase purchase) {
        try {
            mHelper.consumeAsync(purchase, mConsumeFinishedListener);
        } catch (IabAsyncInProgressException e) {
            log.error("Error consuming donation. Another async operation in progress.");
            return;
        }
    }

    private IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            log.debug("Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isSuccess()) {
                log.debug("Consumption successful. Ready to donate again.");
                donations.get(purchase.getSku()).setPurchased(false);
            } else {
                log.error("Error while consuming: " + result);
            }
        }
    };

    /**
     *
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (isInvalidIabKey()) {
            log.warn("missing IAB_KEY: do nothing");
            return false;
        }

        boolean isIabResult = mHelper.handleActivityResult(requestCode, resultCode, data);
        log.debug("onActivityResult({}, {}, {}) handled by IABUtil: {}", requestCode, requestCode, data, isIabResult);
        return isIabResult;
    }

    /**
     *
     */
    public void destroy() {
        if (isInvalidIabKey()) {
            log.warn("missing IAB_KEY: do nothing");
            return;
        }

        log.debug("Destroying iab helper");
        if (mHelper != null && mHelper.mSetupDone) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
    
}
