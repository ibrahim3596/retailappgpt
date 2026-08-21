# Google Owner Authentication Setup

RetailPOS uses Supabase Auth for the OWNER Google identity while local Room/RBAC remains the authority for on-device permissions.

## Android callback

The app callback is:

`retailpos://login-callback`

The Android manifest and Supabase Auth configuration both use the `retailpos` scheme and `login-callback` host.

Add the callback URL above to the Supabase Auth redirect allow-list.

## Supabase Google provider

In the Supabase project, enable the Google provider and supply the Google OAuth Web Client ID and Client Secret generated from the Google Auth Platform.

For Supabase's Google provider, the Google OAuth client uses the Supabase Auth callback URL shown in the Supabase dashboard. That URL is configured as an authorized redirect URI in Google.

## Google Cloud

Create/select the Google Cloud project, configure the OAuth consent screen/audience, then create a Web application OAuth client. Keep the Client ID and Client Secret in the Supabase dashboard only; do not commit them to this repository.

## App configuration

The Android app reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from BuildConfig. CI uses non-secret placeholders so the project can build without cloud credentials.

Production credentials must be supplied through the configured secret-management mechanism.

## Identity flow

1. Owner selects **Continue with Google**.
2. Supabase Auth performs Google OAuth.
3. Google redirects to `retailpos://login-callback`.
4. RetailPOS handles the deep link and restores the Supabase session.
5. If the Supabase identity is already linked, the corresponding local OWNER session is activated.
6. Otherwise, the owner can link the Google identity to an existing OWNER account or create a new store OWNER account.

Google identities are store-owner identities only. Manager/Cashier access continues to use the existing local RBAC and PIN flow.
