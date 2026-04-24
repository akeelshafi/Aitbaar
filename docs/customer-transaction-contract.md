# Customer Transaction Contract (Sprint 0)

This document defines the shared transaction schema and lifecycle used by vendor and customer apps.

## Collection
`transactions/{transactionId}`

## Required Fields
- `transactionId: string`
- `vendorId: string`
- `vendorName: string`
- `customerId: string`
- `customerName: string`
- `customerPhone: string`
- `item: string`
- `amount: number`
- `currency: string` (default: `INR`)
- `status: string` (`PENDING | ACCEPTED | REJECTED | PAID`)
- `createdAt: timestamp`
- `updatedAt: timestamp`

## Optional Fields
- `approvedAt: timestamp | null`
- `rejectedAt: timestamp | null`
- `rejectionReason: string | null`
- `paidAt: timestamp | null`

## Lifecycle
1. Vendor creates transaction with `status=PENDING`.
2. Customer reviews pending transaction.
3. Customer updates status to either:
   - `ACCEPTED`, or
   - `REJECTED`
4. Payment flow can move accepted transaction to `PAID` in a later sprint.

## Allowed Status Transitions
| From | To | Allowed |
|---|---|---|
| PENDING | ACCEPTED | ✅ |
| PENDING | REJECTED | ✅ |
| PENDING | PAID | ❌ |
| ACCEPTED | REJECTED | ❌ |
| REJECTED | ACCEPTED | ❌ |
| ACCEPTED | PAID | ✅ (future payment sprint) |

## Read Patterns (for index support)
- Customer inbox by status:
  - where `customerId == uid`
  - where `status == PENDING|ACCEPTED|REJECTED|PAID`
  - order by `createdAt desc`
- Vendor history by status:
  - where `vendorId == uid`
  - where `status == ...`
  - order by `createdAt desc`

## Security Note
- Vendors need read access to customer profile documents for contact-to-Aitbaar mapping in the customer picker flow.
- Customer profile writes remain owner-only (`customers/{uid}` can only be written by that same uid).
