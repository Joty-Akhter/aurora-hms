# Phase 1.1 - Accounting - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- ✅ Phase 0 Complete (Docker containers running)
- ✅ PostgreSQL running on port 5432
- ✅ Redis running on port 6379
- ✅ Eureka running on port 8761

---

## Step 1: Start Accounting Service

```bash
cd easyops-erp
docker-compose up -d accounting-service
```

Wait ~30 seconds for service to start, then verify:
```bash
curl http://localhost:8088/actuator/health
```

---

## Step 2: Access API Documentation

Open in browser: **http://localhost:8088/swagger-ui.html**

You'll see all accounting APIs:
- Chart of Accounts APIs
- Journal Entry APIs
- Reporting APIs

---

## Step 3: Create Your First Account

```bash
curl -X POST http://localhost:8088/api/accounting/coa \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "YOUR_ORG_ID",
    "accountCode": "1010",
    "accountName": "Cash on Hand",
    "accountType": "ASSET",
    "accountCategory": "Current Assets",
    "level": 1,
    "isGroup": false,
    "openingBalance": 10000.00,
    "openingBalanceDate": "2025-01-01"
  }'
```

---

## Step 4: Create a Revenue Account

```bash
curl -X POST http://localhost:8088/api/accounting/coa \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "YOUR_ORG_ID",
    "accountCode": "4010",
    "accountName": "Sales Revenue",
    "accountType": "REVENUE",
    "accountCategory": "Operating Revenue",
    "level": 1,
    "isGroup": false
  }'
```

---

## Step 5: Post Your First Journal Entry

### Create Journal (Draft):
```bash
curl -X POST http://localhost:8088/api/accounting/journals \
  -H "Content-Type: application/json" \
  -H "X-User-Id: YOUR_USER_ID" \
  -d '{
    "organizationId": "YOUR_ORG_ID",
    "journalDate": "2025-10-15",
    "journalType": "MANUAL",
    "description": "Cash sales for the day",
    "lines": [
      {
        "accountId": "CASH_ACCOUNT_ID",
        "debitAmount": 5000.00,
        "creditAmount": 0,
        "description": "Cash received"
      },
      {
        "accountId": "SALES_ACCOUNT_ID",
        "debitAmount": 0,
        "creditAmount": 5000.00,
        "description": "Sales revenue"
      }
    ]
  }'
```

### Post the Journal:
```bash
curl -X POST http://localhost:8088/api/accounting/journals/JOURNAL_ID/post \
  -H "X-User-Id: YOUR_USER_ID"
```

---

## Step 6: Generate Trial Balance

```bash
curl "http://localhost:8088/api/accounting/reports/trial-balance?organizationId=YOUR_ORG_ID&periodId=PERIOD_ID"
```

---

## 🎯 Key Concepts

### Account Types:
- **ASSET** - Things you own (Cash, Inventory, Equipment)
- **LIABILITY** - Things you owe (Loans, Payables)
- **EQUITY** - Owner's stake (Capital, Retained Earnings)
- **REVENUE** - Income (Sales, Services)
- **EXPENSE** - Costs (Salaries, Rent, Utilities)

### Double-Entry Rule:
- **Every transaction has TWO sides**
- **Debits MUST equal Credits**
- Example: Cash sales
  - Debit: Cash (Asset increases)
  - Credit: Sales (Revenue increases)

### Account Structure:
```
1000 - Assets (Group)
  1010 - Cash (Posting)
  1020 - Bank (Posting)
  1100 - Receivables (Group)
    1110 - Trade Receivables (Posting)
```

---

## 📊 Sample Data

### Create Sample CoA:

```bash
# Assets
curl -X POST http://localhost:8088/api/accounting/coa -H "Content-Type: application/json" -d '{"organizationId":"ORG_ID","accountCode":"1010","accountName":"Cash","accountType":"ASSET","level":1,"isGroup":false}'
curl -X POST http://localhost:8088/api/accounting/coa -H "Content-Type: application/json" -d '{"organizationId":"ORG_ID","accountCode":"1030","accountName":"Bank","accountType":"ASSET","level":1,"isGroup":false}'

# Liabilities
curl -X POST http://localhost:8088/api/accounting/coa -H "Content-Type: application/json" -d '{"organizationId":"ORG_ID","accountCode":"2010","accountName":"Accounts Payable","accountType":"LIABILITY","level":1,"isGroup":false}'

# Equity
curl -X POST http://localhost:8088/api/accounting/coa -H "Content-Type: application/json" -d '{"organizationId":"ORG_ID","accountCode":"3010","accountName":"Share Capital","accountType":"EQUITY","level":1,"isGroup":false}'

# Revenue
curl -X POST http://localhost:8088/api/accounting/coa -H "Content-Type: application/json" -d '{"organizationId":"ORG_ID","accountCode":"4010","accountName":"Sales Revenue","accountType":"REVENUE","level":1,"isGroup":false}'

# Expenses
curl -X POST http://localhost:8088/api/accounting/coa -H "Content-Type: application/json" -d '{"organizationId":"ORG_ID","accountCode":"6110","accountName":"Salaries","accountType":"EXPENSE","level":1,"isGroup":false}'
```

---

## 🐛 Troubleshooting

### Service Won't Start:
```bash
# Check logs
docker-compose logs accounting-service

# Check dependencies
docker-compose ps postgres redis eureka
```

### Journal Won't Post:
- ✅ Check debits = credits
- ✅ Verify period is OPEN
- ✅ Confirm accounts exist and are active
- ✅ Check accounts are posting accounts (not groups)

### Trial Balance Not Showing Data:
- ✅ Ensure journals are POSTED (not DRAFT)
- ✅ Verify correct period ID
- ✅ Check account balances table populated

---

## 📞 Quick Links

| Resource | URL |
|----------|-----|
| **API Documentation** | http://localhost:8088/swagger-ui.html |
| **Health Check** | http://localhost:8088/actuator/health |
| **Prometheus Metrics** | http://localhost:8088/actuator/prometheus |
| **Eureka Dashboard** | http://localhost:8761 |

---

## 🎓 Learning Path

1. **Understand CoA**: Read about account types and structure
2. **Create Accounts**: Use Swagger UI to create test accounts
3. **Post Journals**: Create and post simple journal entries
4. **View Reports**: Generate trial balance
5. **Explore API**: Try all endpoints in Swagger UI

---

## 📚 Related Documentation

- [Phase 1.1 Complete Guide](./PHASE_1.1_COMPLETE.md)
- [Phase 1 Implementation Plan](../requirements/Module-Accounting/PHASE_1_IMPLEMENTATION_PLAN.md)
- [Accounting Module Overview](../requirements/Module-Accounting/README.md)
- [Core Accounting Features](../requirements/Module-Accounting/core_accounting_features.md)

---

**Ready to start your accounting journey!** 💰✨

Get started with Swagger UI: http://localhost:8088/swagger-ui.html

