# RetailPOS V2 — Voice Billing

## Purpose

Voice Billing allows a shopkeeper to speak a natural-language order and turn it into a cart quantity without replacing the normal product lookup flow.

Examples:

- `aadha kilo shakkar` → product `shakkar`, quantity `0.5 kg`
- `500 gram sugar` → product `sugar`, quantity `0.5 kg` when the product's configured selling unit is `kg`
- `1 litre oil` → product `oil`, quantity `1 litre`

## Product pricing model

For loose goods, the product's configured selling price is interpreted as the price for its configured base selling unit.

Examples:

- Sugar: unit `kg`, selling price `₹50` → `aadha kilo shakkar` becomes `0.5 × ₹50 = ₹25`
- Rice: unit `kg`, selling price `₹80` → `250 gram rice` becomes `0.25 × ₹80 = ₹20`
- Oil: unit `litre`, selling price `₹120` → `500 ml oil` becomes `0.5 × ₹120 = ₹60`

This keeps quantity conversion in the cart instead of hard-coding product-specific prices into the voice feature.

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

## Recognition

The Android `SpeechRecognizer` path prefers the on-device recognizer when the device provides one. On Android API 31+ this can be selected through `createOnDeviceSpeechRecognizer`; otherwise the system recognizer is used. Microphone permission is required.

The current recognizer requests `hi-IN` free-form speech so common Hindi/Hinglish kirana phrasing works naturally. Recognition availability and on-device support are device-dependent.

## Current limitation

The current parser intentionally uses deterministic retail rules rather than an LLM. This keeps the critical billing transformation inspectable and safe. Future improvements can add a richer synonym/alias layer and learned corrections without allowing arbitrary AI output to directly change prices.
