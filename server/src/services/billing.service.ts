export interface CalculationItemInput {
  productId: string;
  quantity: number;
}

export interface ItemBillingResult {
  productId: string;
  quantity: number;
  mrpPaise: bigint;
  unitPricePaise: bigint;
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
  gstRate: number;
  isTaxInclusive: boolean;
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
  let subtotalPaise = 0n;
  let taxableValuePaise = 0n;
  let cgstPaise = 0n;
  let sgstPaise = 0n;
  let igstPaise = 0n;
  const calculatedItems: ItemBillingResult[] = [];

  for (const item of items) {
    const prod = products.get(item.productId);
    if (!prod) throw new Error(`Product ${item.productId} not found in product master`);

    const sellingPrice = prod.sellingPricePaise;
    const grossLineTotal = BigInt(Math.round(Number(sellingPrice) * item.quantity));
    
    // Tax Calculation (Tax Inclusive formula: Taxable = Gross / (1 + Rate/100))
    const gstRateNum = Number(prod.gstRate);
    const rateFactor = 1 + gstRateNum / 100;
    
    let taxable: bigint;
    let taxAmount: bigint;

    if (prod.isTaxInclusive) {
      taxable = BigInt(Math.round(Number(grossLineTotal) / rateFactor));
      taxAmount = grossLineTotal - taxable;
    } else {
      taxable = grossLineTotal;
      taxAmount = BigInt(Math.round(Number(grossLineTotal) * (gstRateNum / 100)));
    }

    let lineCgst = 0n, lineSgst = 0n, lineIgst = 0n;
    if (isInterstate) {
      lineIgst = taxAmount;
    } else {
      lineCgst = taxAmount / 2n;
      lineSgst = taxAmount - lineCgst; // Avoid integer division precision loss
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
      gstRate: prod.gstRate,
      taxableAmountPaise: taxable,
      cgstPaise: lineCgst,
      sgstPaise: lineSgst,
      igstPaise: lineIgst,
      lineTotalPaise: taxable + taxAmount
    });
  }

  const totalTax = isInterstate ? igstPaise : (cgstPaise + sgstPaise);
  const rawGrandTotal = subtotalPaise + totalTax - discountPaise;
  const grandTotalPaise = rawGrandTotal < 0n ? 0n : rawGrandTotal;

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

/**
 * Utility to stringify BigInt properties in objects for safe JSON response serialization
 */
export function serializeBigInt<T>(obj: T): T {
  return JSON.parse(
    JSON.stringify(obj, (_, value) =>
      typeof value === "bigint" ? value.toString() : value
    )
  );
}
