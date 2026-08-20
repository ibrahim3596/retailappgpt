# Intelligent Capture Chunk Progress

## Completed in this chunk

1. Stronger evidence ranking:
   - candidate text agreement
   - repeated-frame agreement
   - pack/selling-unit compatibility
   - ranking remains capped below confirmed identity

2. Normalized capture observations:
   - barcode
   - printed name/brand
   - MRP
   - visual category hint/confidence
   - parsed pack quantity/unit
   - frame count and stable fingerprint

3. Safe retailer feedback policy:
   - accepted/rejected catalog candidate
   - camera/OCR preference
   - corrected name/brand/pack
   - conflicting actions produce no learning signal

4. Capture metadata mapper:
   - converts normalized observations into ProductMetadataEntity
   - preserves retailer-controlled tax, description and image values
   - ready for the existing Room transaction save path

5. Tests cover pack parsing, pack compatibility, ranking improvements and feedback safety.

## Deliberate boundary

The mapper and the existing repository transaction helper are ready, but the current ProductViewModel write endpoint has not been replaced in this chunk because GitHub's contents update endpoint repeatedly returned a stale-blob conflict for that file. No blind overwrite was used. The product-save UI therefore still needs the final mapper wiring before automatic capture metadata persistence can be called complete.
