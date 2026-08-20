# Intelligent Capture Chunk Progress

## Completed in this chunk

1. Stronger evidence ranking:
   - candidate text agreement
   - repeated-frame agreement
   - pack/selling-unit compatibility
   - score remains below confirmed identity

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

5. Tests cover pack parsing, pack compatibility, ranking improvements, feedback safety, and metadata mapping.

## Deliberate boundary

The mapper and ProductRepository transaction helper are ready, but the existing ProductViewModel save method has not yet been rewired to call the mapper. That final UI/save integration remains open because the GitHub contents endpoint repeatedly returned stale-blob conflicts for that large file; no blind overwrite was used.
