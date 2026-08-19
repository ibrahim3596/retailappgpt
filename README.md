# RetailPOS V2

> A shopkeeper-first, offline-first Android POS built for fast everyday retail operations.

RetailPOS is being rebuilt as a modern retail operating system: fast billing, reliable inventory, flexible product identification, customer credit/Khata, and useful business reporting — with the local store remaining usable even when the internet is unavailable.

## Product goals

- **Fast billing** — scan, search, add to cart, collect payment, finish.
- **Smart products** — support SKU, barcode/GTIN identifiers, multiple barcodes, and intelligent product capture.
- **Reliable inventory** — stock movements should be traceable rather than silently changing quantities.
- **Offline first** — core retail workflows should not depend on a live network connection.
- **Shopkeeper friendly** — clear workflows, minimal friction, and sensible defaults.
- **Maintainable architecture** — business rules stay separate from UI so the app can grow without becoming fragile.

## Current development status

**Active development — RetailPOS V2**

The `retailpos-v2` branch is the active development line. `main` contains the previous implementation/reference archive.

### Major areas being built

| Area | Direction |
|---|---|
| Billing / POS | Fast scan-and-sell workflow |
| Products | Product master, SKU and barcode management |
| Intelligent Product Capture | Barcode + camera/OCR + catalog-assisted identification |
| Inventory | Stock, adjustments, movements and alerts |
| Customers | Customer profiles and transaction history |
| Khata | Credit sales and payment ledger |
| Reports | Sales, profit, inventory and business insights |
| Settings | Store, tax, receipt, staff and operational configuration |

## Intelligent Product Capture

RetailPOS is designed to go beyond simply reading a barcode.

The intended pipeline is:

```text
Product
   ↓
Camera / Barcode / OCR / Visual signals
   ↓
Identifier & evidence analysis
   ↓
Catalog lookup when available
   ↓
Candidate product + confidence
   ↓
Retailer review
   ↓
Product saved to the local catalog
```

The app should **suggest**, not blindly invent. Store-controlled fields such as selling price, purchase price, stock and retailer-specific SKU remain under the shopkeeper's control.

## Barcode & product identifiers

The product model is being designed to distinguish between:

- retailer SKU
- primary barcode
- additional/alternate barcodes
- barcode type
- globally issued identifiers such as GTIN/EAN/UPC where applicable
- products that have no usable barcode

This allows the same product to be found reliably through scanning, search, or intelligent capture without conflating a retailer's SKU with a global identifier.

## Architecture principles

```text
Compose UI
    ↓
ViewModel / UI state
    ↓
Use cases / business rules
    ↓
Repositories
    ↓
Local database + device services + network services
```

Core retail functionality is being kept local-first, with cloud identity/sync added only after the local foundation is stable.

## Technology

- Kotlin
- Jetpack Compose
- Android SDK
- Room
- CameraX
- Google ML Kit
- Gradle

## Repository structure

```text
app/
  src/main/java/com/retailpos/app/
    core/        # Shared infrastructure and utilities
    data/        # Local/network data sources and repositories
    domain/      # Business models and rules
    feature/     # Feature-oriented application modules
    ui/          # Shared UI/design-system components

docs/            # Product and engineering documentation
.github/         # Repository automation and contribution configuration
```

The structure will evolve as the feature set grows; new functionality should be placed by responsibility rather than accumulating everything in a single activity or screen.

## Development workflow

We develop in coherent feature slices rather than using CI as a debugging loop.

```text
Design the slice
      ↓
Implement
      ↓
Local/unit validation
      ↓
Integrate with existing flows
      ↓
Review architecture
      ↓
CI verification when available
```

GitHub Actions is treated as a verification gate, not as the primary development/debugging environment.

## Documentation

Engineering and product documentation will live under `docs/` as the project expands.

Planned documents include:

- `ARCHITECTURE.md` — application architecture and boundaries
- `PRODUCT_SPEC.md` — functional product specification
- `DATA_MODEL.md` — entities, identifiers and relationships
- `BARCODE_SYSTEM.md` — SKU/barcode/GTIN handling
- `INTELLIGENT_CAPTURE.md` — recognition and confidence model
- `OFFLINE_FIRST.md` — local-first and sync strategy
- `TESTING.md` — test strategy and validation rules
- `ROADMAP.md` — development roadmap

## Branches

- `main` — previous implementation/reference archive
- `retailpos-v2` — active V2 development line
- `feature/*` — focused feature work when isolation is useful

Temporary verification files and throwaway development artifacts should not be added to the product codebase.

## Project philosophy

RetailPOS is being built as a real retail product, not a demo. Correct data, fast workflows, predictable offline behaviour, and maintainable code take priority over adding superficial features quickly.
