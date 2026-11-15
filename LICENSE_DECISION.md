# License decision — GoldSilverPOS

This repository (native Android point-of-sale and management system for a
gold & silver shop) does **not** yet carry an open-source license. Until a
license is added, all rights are reserved by default.

## Context

GoldSilverPOS is business software that records sales, purchases, inventory,
customer records, and gold/silver transactions. The license choice should
reflect:

- **Commercial intent**: do you want others to reuse/resell it, or reserve
  commercial rights for yourself?
- **Liability**: money-handling software can cause real losses if it
  miscalculates weights, rates, or balances. A strong warranty disclaimer and
  limitation of liability are essential.

## Recommended options

### Option A — Proprietary / "All Rights Reserved" (current state)
- You keep full control; no one may legally reuse or distribute the app.
- Appropriate if you plan to sell licenses or deploy it yourself.

### Option B — MIT License (permissive)
- Allows use, modification, and commercial redistribution, including in
  closed-source products.
- Minimal obligations; strong warranty/liability disclaimer.
- Best if you want other developers to learn from and build on the code.

### Option C — Apache-2.0 (permissive + patent grant)
- Like MIT with an explicit contributor patent grant.
- Good if you want permissive reuse with added patent clarity.

## Recommendation

For a money-handling POS you may wish to monetize, start with **Option A
(proprietary)**. If you choose to open-source it, **MIT or Apache-2.0** is
the better fit.

## How to adopt (if choosing an open-source license)

1. Pick a license (MIT or Apache-2.0 recommended).
2. Add a `LICENSE` file at the repository root with the official license text.
3. Add copyright: `Copyright (c) 2026 Muhammad Subhan`.
4. Keep this `LICENSE_DECISION.md` as the decision record.

> Do not treat GoldSilverPOS as open-source licensed until a `LICENSE` file
> is present.
