import { describe, it, expect } from "vitest";
import { calculateBilling, ProductPriceMetadata } from "./billing.service";

const product = (over: Partial<ProductPriceMetadata> = {}): ProductPriceMetadata => ({
  mrpPaise: 10000n,
  sellingPricePaise: 9000n,
  gstRate: 18,
  isTaxInclusive: true,
  ...over
});

const items = (qty: number = 1) => [{ productId: "p1", quantity: qty }];

describe("calculateBilling", () => {
  it("splits inclusive GST into taxable value and tax", () => {
    const r = calculateBilling(
      new Map([["p1", product()]]),
      items(1),
      false,
      0n
    );
    expect(r.subtotalPaise).toBe(9000n);
    expect(r.taxableValuePaise + r.cgstPaise + r.sgstPaise).toBe(9000n);
    expect(r.grandTotalPaise).toBe(9000n);
    expect(r.cgstPaise + r.sgstPaise).toBe(1373n);
  });

  it("adds tax on top for exclusive pricing", () => {
    const r = calculateBilling(
      new Map([["p1", product({ isTaxInclusive: false })]]),
      items(1),
      false,
      0n
    );
    // ₹90 + 18% = ₹106.20
    expect(r.grandTotalPaise).toBe(10620n);
    expect(r.taxableValuePaise).toBe(9000n);
    expect(r.cgstPaise + r.sgstPaise).toBe(1620n);
  });

  it("routes tax to IGST for interstate sales", () => {
    const r = calculateBilling(
      new Map([["p1", product({ isTaxInclusive: false })]]),
      items(1),
      true,
      0n
    );
    expect(r.igstPaise).toBe(1620n);
    expect(r.cgstPaise).toBe(0n);
    expect(r.sgstPaise).toBe(0n);
  });

  it("applies discount and never returns a negative total", () => {
    const r = calculateBilling(
      new Map([["p1", product()]]),
      items(1),
      false,
      9500n
    );
    expect(r.grandTotalPaise).toBe(0n);
  });

  it("multiplies quantity exactly in integer paise", () => {
    const r = calculateBilling(
      new Map([["p1", product({ sellingPricePaise: 9111n })]]),
      items(3),
      false,
      0n
    );
    expect(r.subtotalPaise).toBe(27333n);
    expect(r.grandTotalPaise).toBe(27333n);
  });

  it("keeps cgst + sgst equal to total tax without remainder loss", () => {
    const r = calculateBilling(
      new Map([["p1", product({ sellingPricePaise: 9999n, gstRate: 12 })]]),
      items(1),
      false,
      0n
    );
    expect(r.cgstPaise + r.sgstPaise).toBe(9999n - r.taxableValuePaise);
    expect(r.grandTotalPaise).toBe(9999n);
  });

  it("throws for a product missing from the master", () => {
    expect(() =>
      calculateBilling(new Map(), items(1), false, 0n)
    ).toThrow(/not found/);
  });

  it("handles fractional quantities without losing paise", () => {
    const r = calculateBilling(
      new Map([["p1", product({ sellingPricePaise: 4000n })]]),
      [{ productId: "p1", quantity: 2.5 }],
      false,
      0n
    );
    expect(r.subtotalPaise).toBe(10000n);
  });
});
