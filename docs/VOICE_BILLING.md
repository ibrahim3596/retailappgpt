# RetailPOS V2 — Voice Billing

## Purpose

Voice Billing allows a shopkeeper to speak a natural-language order and turn it into a cart quantity without replacing the normal product lookup flow.

Examples:

- `aadha kilo shakkar` → product `sugar`, quantity `0.5 kg`
- `500 gram sugar` → product `sugar`, quantity `0.5 kg` when the product's configured selling unit is `kg`
- `1 litre oil` → product `oil`, quantity `1 litre`

## Product pricing model

For loose goods, the product's configured selling price is interpreted as the price for its configured base selling unit.

Examples:

- Sugar: unit `kg`, selling price `₹50` → `aadha kilo shakkar` becomes `0.5 × ₹50 = ₹25`
- Rice: unit `kg`, selling price `₹80` → `250 gram rice` becomes `0.25 × ₹80 = ₹20`
- Oil: unit `litre`, selling price `₹120` → `500 ml oil` becomes `0.5 × ₹120 = ₹60`

The voice feature does not contain product-specific prices. It converts the requested quantity into the product's configured selling unit and uses the existing cart pricing calculation.

## Safety rules

Voice input is never allowed to silently create a product.

The resolver:

1. parses quantity + unit + product wording
2. searches the local product catalog
3. requires exactly one matching product
4. converts the spoken unit into the product's configured unit
5. enforces available stock
6. adds the fractional quantity to the existing cart line

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

The app uses Android's speech-recognition service rather than shipping its own large speech model. On Android 13/API 33+ the app can ask the selected recognizer whether a language is installed, pending, supported on-device, or only available online. The app can also request a language-model download through Android's speech APIs when the recognizer exposes that capability.

Important: the app cannot guarantee that every Android phone provides every Indian language. Recognition language availability is controlled by the installed recognition service and device. Unsupported languages are reported rather than silently falling back to a different language.

On Android versions below API 33, the app does not have the same public API for checking or managing language-model download state. The selected language can still be sent to the device speech recognizer when that recognizer supports it.

## Offline direction

The long-term goal is offline-first voice billing. The current implementation prefers on-device speech recognition when available, while still supporting devices whose recognition service is online-only. Voice language download is a device/recognizer capability, not a RetailPOS-controlled model installation.

The billing transformation itself is always local and deterministic: speech text is converted into structured quantity/unit/product data, then resolved against the local Room catalog and existing cart rules.

## Current limitation

The current parser intentionally uses deterministic retail rules rather than an LLM. Future improvements can add a richer multilingual alias layer, multi-item voice orders, learned corrections, and offline language packs without allowing arbitrary AI output to directly change prices.
