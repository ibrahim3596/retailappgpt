# RetailPOS Product Identification

## Identifier rules

- **SKU** is the shop's internal identifier. RetailPOS may generate or accept it, but it is not a GS1 identifier.
- **GTIN** is an external product identifier. RetailPOS validates GTIN check digits but never fabricates a GS1 GTIN.
- A product may have multiple scannable identifiers.
- Barcode symbology is stored separately from the identifier value.
- EAN-8, EAN-13, UPC-A and UPC-E are treated as consumer retail GTIN formats.
- ITF-14 is recognized but is treated as a logistics/case identifier rather than an automatic consumer checkout identifier.
- Code 128, Code 39, Code 93, Codabar and ITF are recognized symbologies but are not assumed to contain a GTIN.
- QR Code, Data Matrix, PDF417 and Aztec are supported as 2D scan formats. Their payloads must be parsed before deciding whether they contain a GTIN, batch, expiry or serial number.

## Normalization

Scanned values are trimmed and normalized before lookup. Normalization must not silently change the semantic value of an identifier.

## Future GS1 2D support

The domain model leaves room for GS1 2D payload parsing. A future parser can extract fields such as GTIN, lot/batch, expiry and serial number without changing the core product identity model.
