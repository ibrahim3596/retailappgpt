# Intelligent Capture Pack Units Progress

## Implemented

- OCR pack-size extraction for explicit numeric quantity + unit.
- Normalization for common weight, volume and package-unit spellings.
- `ProductPackCompatibility` separates observed package quantity from the retailer's configured selling unit.
- Measured package quantities can be classified as convertible when the selling unit is another mass/volume unit.
- A packaged product sold by pieces treats captured weight/volume as descriptive metadata instead of changing the selling unit.
- Mismatched units require retailer review.
- Tests cover convertible, descriptive and mismatch cases.

## Deliberate boundary

- Captured pack size is currently a reviewed observation. Automatic persistence into `product_metadata` is not yet wired through the existing product-save ViewModel because that path still needs a safe atomic integration.
- Pack size must never silently overwrite `ProductEntity.unit`.
- Selling unit remains retailer-controlled.
