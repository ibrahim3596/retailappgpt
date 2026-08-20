# Intelligent Capture — Pack Size Progress

## Implemented

- Deterministic OCR pack-size/unit parser.
- Supports explicit printed forms such as `500 g`, `1 kg`, `750 ml`, `1 L`, `6 pieces`, `2 packets`, `1 bottle` and common spelling variants.
- Normalizes units to stable values such as `g`, `kg`, `ml`, `L`, `pcs`, `packet`, `bottle`.
- Capture result now carries detected pack size/unit alongside barcode, OCR name/brand, MRP and visual evidence.
- Capture UI displays the inferred pack size before the retailer accepts the captured result.
- Bare numbers such as MRP/batch numbers are not treated as pack size without an explicit supported unit.

## Deliberate boundary

- The existing ProductViewModel save endpoint could not be safely patched through the current GitHub contents API in this batch, so automatic persistence of the inferred pack size into `product_metadata` is not yet wired.
- `ProductRepository` contains a transaction-safe metadata persistence helper for the eventual integration, but the current capture flow does not claim that metadata is persisted automatically.
- No unit conversion is inferred from pack text; printed pack size remains exactly an observed attribute.

## Next step

Wire the captured pack size/unit into ProductViewModel's existing metadata persistence path, then add product-level normalization and loose-item selling-unit compatibility checks.
