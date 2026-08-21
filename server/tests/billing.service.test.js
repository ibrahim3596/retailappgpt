const test = require("node:test");
const assert = require("node:assert/strict");

const { calculateBilling } = require("../dist/services/billing.service.js");

function product(overrides = {}) {
  return {
    mrpPaise: 1120n,
    sellingPricePaise: 1120n,
    purchasePricePaise: 800n,
    gstRate: 12,
    isTaxInclusive: true,
    ...overrides,
  };
}

test("tax-inclusive billing does not double-count GST", () => {
  const result = calculateBilling(
    new Map([["p1", product()]]),
    [{ productId: "p1", quantity: 1 }],
    false,
    0n
  );

  assert.equal(result.subtotalPaise, 1120n);
  assert.equal(result.taxableValuePaise, 1000n);
  assert.equal(result.cgstPaise, 60n);
  assert.equal(result.sgstPaise, 60n);
  assert.equal(result.grandTotalPaise, 1120n);
});

test("tax-exclusive billing adds GST exactly once", () => {
  const result = calculateBilling(
    new Map([["p1", product({ sellingPricePaise: 1000n, isTaxInclusive: false })]]),
    [{ productId: "p1", quantity: 1 }],
    false,
    0n
  );

  assert.equal(result.subtotalPaise, 1120n);
  assert.equal(result.taxableValuePaise, 1000n);
  assert.equal(result.cgstPaise, 60n);
  assert.equal(result.sgstPaise, 60n);
  assert.equal(result.grandTotalPaise, 1120n);
});

test("interstate sales use IGST instead of CGST and SGST", () => {
  const result = calculateBilling(
    new Map([["p1", product()]]),
    [{ productId: "p1", quantity: 1 }],
    true,
    0n
  );

  assert.equal(result.cgstPaise, 0n);
  assert.equal(result.sgstPaise, 0n);
  assert.equal(result.igstPaise, 120n);
  assert.equal(result.grandTotalPaise, 1120n);
});

test("quantity decimals are handled without binary floating-point money drift", () => {
  const result = calculateBilling(
    new Map([["p1", product({ sellingPricePaise: 100n, gstRate: 0 })]]),
    [{ productId: "p1", quantity: 1.5 }],
    false,
    0n
  );

  assert.equal(result.subtotalPaise, 150n);
  assert.equal(result.grandTotalPaise, 150n);
});

test("discount cannot make a sale negative", () => {
  assert.throws(
    () => calculateBilling(
      new Map([["p1", product()]]),
      [{ productId: "p1", quantity: 1 }],
      false,
      1121n
    ),
    /Discount cannot exceed subtotal/
  );
});
