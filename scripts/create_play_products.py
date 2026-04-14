#!/usr/bin/env python3
"""
Create Google Play consumable credit-pack products for com.katafract.safeopen.

Mirrors the iOS StoreKit catalog 1:1 (SafeOpen/Configuration.storekit + Services/SafeOpenStore.swift):
  credits_starter   — 100   credits, $0.99
  credits_standard  — 500   credits, $2.99
  credits_power     — 2,500 credits, $9.99

All consumable (buy multiple times). Uses the new monetization.onetimeproducts
API (old inappproducts endpoint returns 403 "Please migrate to the new
publishing API"). Globally available via newRegionsConfig with USD+EUR anchors.

Idempotent: patch has create-or-update semantics keyed on productId.

Usage:
  SERVICE_ACCOUNT_KEY=/tmp/PLAY_SERVICE_ACCOUNT_KEY.json \
      python3 create_play_products.py
"""

import os
import sys

try:
    from googleapiclient.discovery import build
    from google.oauth2 import service_account
except ImportError:
    sys.exit("pip install google-api-python-client google-auth")

PACKAGE = "com.katafract.safeopen"
REGIONS_VERSION = "2025/03"


def usd(dollars, cents):
    return {"currencyCode": "USD", "units": str(dollars), "nanos": cents * 10_000_000}


def eur(dollars, cents):
    return {"currencyCode": "EUR", "units": str(dollars), "nanos": cents * 10_000_000}


def product(product_id, title, description, dollars, cents):
    return {
        "packageName": PACKAGE,
        "productId": product_id,
        "listings": [{
            "languageCode": "en-US",
            "title": title,
            "description": description,
        }],
        "purchaseOptions": [{
            "purchaseOptionId": "buy",
            "buyOption": {
                "legacyCompatible": True,
                # Users can buy multiple packs in one transaction
                "multiQuantityEnabled": True,
            },
            "newRegionsConfig": {
                "availability": "AVAILABLE",
                "usdPrice": usd(dollars, cents),
                "eurPrice": eur(dollars, cents),
            },
            "regionalPricingAndAvailabilityConfigs": [
                {"regionCode": "US", "availability": "AVAILABLE", "price": usd(dollars, cents)},
                {"regionCode": "DE", "availability": "AVAILABLE", "price": eur(dollars, cents)},
            ],
            "taxAndComplianceSettings": {
                "withdrawalRightType": "WITHDRAWAL_RIGHT_DIGITAL_CONTENT",
            },
        }],
    }


PRODUCTS = [
    product(
        "com.katafract.safeopen.credits_starter",
        "100 Scan Credits",
        "100 scan credits for SafeOpen. AI summary or Open Safely costs 1 credit. Credits never expire.",
        0, 99,
    ),
    product(
        "com.katafract.safeopen.credits_standard",
        "500 Scan Credits",
        "500 scan credits for SafeOpen. Best value for regular users. Credits never expire.",
        2, 99,
    ),
    product(
        "com.katafract.safeopen.credits_power",
        "2500 Scan Credits",
        "2,500 scan credits for SafeOpen. For power users who inspect dozens of links per day. Credits never expire.",
        9, 99,
    ),
]


def main():
    key_path = os.environ.get("SERVICE_ACCOUNT_KEY", "/tmp/PLAY_SERVICE_ACCOUNT_KEY.json")
    if not os.path.exists(key_path):
        sys.exit(f"Service account key not found at {key_path}")

    creds = service_account.Credentials.from_service_account_file(
        key_path, scopes=["https://www.googleapis.com/auth/androidpublisher"])
    svc = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
    otp = svc.monetization().onetimeproducts()

    created_ids = []
    for prod in PRODUCTS:
        pid = prod["productId"]
        print(f"\nPatching {pid} ...")
        try:
            resp = otp.patch(
                packageName=PACKAGE,
                productId=pid,
                regionsVersion_version=REGIONS_VERSION,
                updateMask="listings,purchaseOptions,taxAndComplianceSettings",
                allowMissing=True,
                body=prod,
            ).execute()
            price = resp["purchaseOptions"][0]["regionalPricingAndAvailabilityConfigs"][0]["price"]
            # `units` is omitted from the response when dollars == 0; handle sub-$1 prices
            dollars = price.get("units", "0")
            cents_str = str(price.get("nanos", 0)).zfill(9)[:2]
            print(f"  OK — {resp['listings'][0]['title']} ${dollars}.{cents_str}")
            created_ids.append(pid)
        except Exception as e:
            print(f"  ERROR: {str(e)[:400]}")

    # Activate purchase options — patch leaves them DRAFT; a second call flips to ACTIVE
    if created_ids:
        print("\nActivating purchase options ...")
        activate_body = {"requests": [
            {"activatePurchaseOptionRequest": {
                "packageName": PACKAGE,
                "productId": pid,
                "purchaseOptionId": "buy",
            }} for pid in created_ids
        ]}
        try:
            result = otp.purchaseOptions().batchUpdateStates(
                packageName=PACKAGE,
                productId="-",
                body=activate_body,
            ).execute()
            for p in result.get("oneTimeProducts", []):
                for po in p.get("purchaseOptions", []):
                    print(f"  {p['productId']}/{po['purchaseOptionId']}: state={po.get('state')}")
        except Exception as e:
            print(f"  activation ERROR: {str(e)[:400]}")


if __name__ == "__main__":
    main()
