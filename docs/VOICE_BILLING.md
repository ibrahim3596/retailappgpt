# RetailPOS V2 — Voice Billing

## Purpose

Voice Billing allows a shopkeeper to speak a natural-language order and turn it into cart quantities without replacing the normal product lookup flow.

Examples:

- `aadha kilo shakkar` → product `sugar`, quantity `0.5 kg`
- `quarter kilo sugar` / `पाव किलो चीनी` → product `sugar`, quantity `0.25 kg`
- `750 gram sugar` → `0.75 kg` when the product's configured selling unit is `kg`
- `1 litre oil` → product `oil`, quantity `1 litre`
- `2 packets biscuits` → product `biscuits`, quantity `2 pieces`
- `aadha kilo shakkar aur 1 litre tel` → two validated cart lines in one voice action

## Loose-item pricing model

For loose goods, the product's configured selling price is interpreted as the price for its configured base selling unit.

Examples:

- Sugar: unit `kg`, selling price `₹50` → `aadha kilo shakkar` becomes `0.5 × ₹50 = ₹25`
- Rice: unit `kg`, selling price `₹80` → `250 gram rice` becomes `0.25 × ₹80 = ₹20`
- Oil: unit `litre`, selling price `₹120` → `500 ml oil` becomes `0.5 × ₹120 = ₹60`

The voice feature does not contain product-specific prices. It converts the requested quantity into the product's configured selling unit and uses the existing cart pricing calculation.

## Supported quantity families

Weight and volume:

- kg / kilo / kilogram and common Indian-script equivalents
- g / gram
- litre / liter / l
- ml
- numeric decimals such as `0.5 kg`, `1.25 kg`, `750 gram`, `500 ml`
- common fractions such as `aadha`, `pauna`, `sawa`, `dedh`, `quarter`, `pav`, and regional-script equivalents currently covered by deterministic rules

Count-based retail units:

- piece / item
- packet / pack / pouch
- bottle
- box
- tin
- jar
- sachet
- common Indian-script equivalents

Count-based units are normalized to the existing `PIECE` quantity model for cart and stock calculations. The product's configured unit remains the retailer's source of truth for pricing and display.

## Multi-item orders

A single utterance can contain multiple independently parseable requests, separated by common connectors such as `aur`, `and`, `plus`, `और`, `आणि`, `మరియు`, `ഒപ്പം`, `കൂടാതെ`, `மற்றும்`, `ಮತ್ತು`, `এবং`, `અને`, `ਅਤੇ`, or `ଏବଂ`.

The complete utterance is resolved before the cart is mutated. If any item is missing, ambiguous, incompatible with the product unit, invalid, or beyond available stock, the entire voice action is rejected so a partial order cannot accidentally enter the bill.

Repeated requests for the same product are aggregated before stock validation. This prevents two individually valid requests from exceeding total available stock.

## Safety rules

Voice input is never allowed to silently create a product or silently choose between multiple products.

The resolver:

1. parses quantity + unit + product wording
2. searches the local product catalog
3. requires exactly one matching product for every spoken item
4. converts the spoken unit into the product's configured unit
5. validates the complete requested quantity against available stock
6. only then adds the complete validated set to the existing cart

Ambiguous or incompatible requests are rejected and shown to the cashier instead of guessing.

## Indian language support

Hindi (`hi-IN`) is the default voice language. The language picker currently exposes:

- Hindi (`hi-IN`)
- English India (`en-IN`)
- Telugu (`te-IN`)
- Malayalam (`ml-IN`)
- Marathi (`mr-IN`)
- Tamil (`ta-IN`)
- Kannada (`kn-IN`)
- Bengali (`bn-IN`)
- Gujarati (`gu-IN`)
- Punjabi (`pa-IN`)
- Odia (`or-IN`)

The selected language is stored locally so the shopkeeper does not need to choose it on every bill.

The parser also contains deterministic common-product aliases and quantity/unit words across these Indian scripts so recognized text such as Telugu or Tamil product names can still resolve against a local catalog containing English master names.

The app uses Android's speech-recognition service rather than shipping its own large speech model. On Android 13/API 33+ the app can ask the selected recognizer whether a language is installed, pending, supported on-device, or only available online. It can also request a language-model download through Android's speech APIs when the recognizer exposes that capability. Android documents these support states through `RecognitionSupport` and model download through `SpeechRecognizer.triggerModelDownload()`. citeturn804607search0turn804607search1

On Android 14/API 34+ the app can receive model-download progress, scheduled, success, and error callbacks through `ModelDownloadListener`. citeturn804607search0

Important: the app cannot guarantee that every Android phone provides every Indian language. Recognition language availability is controlled by the installed recognition service and device. Unsupported languages are reported rather than silently falling back to a different language.

## Offline direction

The long-term goal is offline-first voice billing. The app prefers the on-device recognizer when the device exposes one, while still allowing devices with only online recognition to use voice billing. Language-model availability is a device/recognizer capability, not a RetailPOS-controlled model installation.

The critical billing transformation is always local and deterministic: speech text is converted into structured quantity/unit/product data, then resolved against the local Room catalog and existing cart rules.

## Current limitations

The current implementation is deliberately deterministic rather than LLM-driven. This keeps critical billing transformations inspectable and prevents arbitrary AI output from changing prices.

Future improvements include broader regional aliases, learned retailer corrections, language-specific number handling, richer fractions, explicit offline-voice status, and more natural regional phrasing.
