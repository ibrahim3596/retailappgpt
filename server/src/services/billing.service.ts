export interface CalculationItemInput {
  productId: string;
  quantity: number;
}

export interface ItemBillingResult {
  productId: string;
  quantity: number;
  mrpPaise: bigint;
  unitPricePaise: bigint;
  purchasePricePaise: bigint;
  gstRate: number;
  taxableAmountPaise: bigint;
  cgstPaise: bigint;
  sgstPaise: bigint;
  igstPaise: bigint;
  lineTotalPaise: bigint;
}

export interface ProductPriceMetadata {
  mrpPaise: bigint;
  sellingPricePaise: bigint;
  purchasePricePaise: bigint;
  gstRate: number;
  isTaxInclusive: boolean;
}

function multiplyPaise(pricePaise: bigint, quantity: number): bigint {
  if (!Number.isFinite(quantity) || quantity <= 0) throw new Error("Quantity must be greater than zero");
  const quantityMicros = BigInt(Math.round(quantity * 1_000_000));
  return (pricePaise * quantityMicros + 500_000n) / 1_000_000n;
}

export function calculateBilling(
  products: Map<string, ProductPriceMetadata>,
  items: CalculationItemInput[],
  isInterstate: boolean,
  discountPaise: bigint
): {
  subtotalPaise: bigint;
  taxableValuePaise: bigint;
  cgstPaise: bigint;
  sgstPaise: bigint;
  igstPaise: bigint;
  grandTotalPaise: bigint;
  calculatedItems: ItemBillingResult[];
} {
  if (discountPaise < 0n) throw new Error("Discount cannot be negative");

  let subtotalPaise = 0n;
  let taxableValuePaise = 0n;
  let cgstPaise = 0n;
  let sgstPaise = 0n;
  let igstPaise = 0n;
  const calculatedItems: ItemBillingResult[] = [];

  for (const item of items) {
    const prod = products.get(item.productId);
    if (!prod) throw new Error(`Product ${item.productId} not found in product master`);

    const grossLineTotal = multiplyPaise(prod.sellingPricePaise, item.quantity);
    const purchaseLineTotal = multiplyPaise(prod.purchasePricePaise, item.quantity);
    const gstRateNum = prod.gstRate;

    let taxable: bigint;
    let taxAmount: bigint;

    if (prod.isTaxInclusive) {
      const rateBasisPoints = BigInt(Math.round(gstRateNum * 100));
      if (rateBasisPoints < 0n) throw new Error("GST rate cannot be negative");
      taxable = (grossLineTotal * 10_000n + (10_000n + rateBasisPoints) / 2n) / (10_000n + rateBasisPoints);
      taxAmount = grossLineTotal - taxable;
    } else {
      taxable = grossLineTotal;
      const rateBasisPoints = BigInt(Math.round(gstRateNum * 100));
      taxAmount = (grossLineTotal * rateBasisPoints + 5_000n) / 10_000n;
    }

    let lineCgst = 0n;
    let lineSgst = 0n;
    let lineIgst = 0n;
    if (isInterstate) {
      lineIgst = taxAmount;
    } else {
      lineCgst = taxAmount / 2n;
      lineSgst = taxAmount - lineCgst;
    }

    subtotalPaise += grossLineTotal;
    taxableValuePaise += taxable;
    cgstPaise += lineCgst;
    sgstPaise += lineSgst;
    igstPaise += lineIgst;

    calculatedItems.push({
      productId: item.productId,
      quantity: item.quantity,
      mrpPaise: prod.mrpPaise,
      unitPricePaise: prod.sellingPricePaise,
      purchasePricePaise: prod.purchasePricePaise,
      gstRate: prod.gstRate,
      taxableAmountPaise: taxable,
      cgstPaise: lineCgst,
      sgstPaise: lineSgst,
      igstPaise: lineIgst,
      lineTotalPaise: grossLineTotal
    });
  }

  const totalTax = isInterstate ? igstPaise : cgstPaise + sgstPaise;
  const rawGrandTotal = subtotalPaise - discountPaise;
  const grandTotalPaise = rawGrandTotal < 0n ? 0n : rawGrandTotal;

  if (grandTotalPaise !== subtotalPaise - discountPaise) {
    throw new Error("Invalid discount exceeds subtotal");
  }

  return {
    subtotalPaise,
    taxableValuePaise,
    cgstPaise,
    sgstPaise,
    igstPaise,
    grandTotalPaise,
    calculatedItems
  };
}

export function serializeBigInt<T>(obj: T): T {
  return JSON.parse(
    JSON.stringify(obj, (_, value) =>
      typeof value === "bigint" ? value.toString() : value
    )
  );
}
