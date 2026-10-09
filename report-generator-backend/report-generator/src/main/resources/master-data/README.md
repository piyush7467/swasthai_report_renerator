# Master Data Seeding Infrastructure

## Overview
This directory contains the production-safe master data catalog definitions for laboratory categories, tests, and test parameters.

The seeding infrastructure ingests clinical master data into the database in an idempotent, atomic, and validated manner.

---

## 1. Natural Keys vs. Public Reference IDs (refId)
- **Seed Relationship Identity (`code`)**:
  - The master data JSON definitions use stable, readable, machine-normalized business codes (`categoryCode`, `testCode`, `parameterCode`).
  - Hierarchical relationships in seed files connect through these stable codes.
  - RefIds (`TC-...`, `TEST-...`, `PARAM-...`) must **never** be hardcoded in JSON files.
- **Public API Identity (`refId`)**:
  - Public REST APIs and frontend clients continue to communicate via cryptographic `refId` values.
  - When new records are seeded, their `refId` is generated automatically via JPA lifecycle `@PrePersist` and `RefIdGenerator`.
  - For existing records, their existing `refId` is preserved completely.

---

## 2. Hierarchy Resolution
- **Category → Test**:
  - In `tests.json`, each test declares `categoryCode`.
  - The seeder resolves `categoryCode` against known categories (from `categories.json` or existing database records).
  - If a referenced category code does not exist, validation **fails immediately** before modifying the database.
- **Test → Parameter**:
  - In parameter JSON files (`test-parameters/*.json`), each parameter declares `testCode`.
  - The seeder resolves `testCode` against known tests (from `tests.json` or existing database records).
  - If a referenced test code does not exist, validation **fails immediately** before modifying the database.

---

## 3. Idempotent Behavior & Conflict Handling
- **Insert Missing**: If a record does not exist in the database (evaluated by natural key `code`), it is inserted.
- **Skip Existing**: If a record already exists in the database with the matching code, it is **skipped** (not duplicated, not re-generated).
- **Conflict Detection**:
  - If a database record exists with the same code but attributes differ from the JSON seed definition (e.g., modified description, display order, or units), the seeder identifies and logs a detailed warning.
  - The existing database record is **never** silently overwritten.
  - When `master-data.seed.fail-on-conflict=true` is set (e.g. in CI or test suites), detected conflicts will cause the seeder to reject the run.

---

## 4. Calculation Validation & Integration
- Master-data JSON defines configuration, **not formulas**.
- The existing `CalculationEngine` and `CalculationDependencyResolver` evaluate all calculated parameters:
  - Validates that `calculationType` is supported.
  - Validates that `dataType` is compatible with the formula.
  - Verifies that all required dependency parameters are present in the same test.
  - Detects and rejects any circular dependency graph (e.g. A → B → A).

---

## 5. How to Enable / Disable Seeding
Seeding is **disabled by default** to ensure safety across environments.

To enable seeding:
- In `application.yaml` or `.env`:
  ```bash
  MASTER_DATA_SEED_ENABLED=true
  ```
- To enforce zero conflicts:
  ```bash
  MASTER_DATA_SEED_FAIL_ON_CONFLICT=true
  ```

---

## 6. Where Future JSON Data Should Be Placed
- Categories: `src/main/resources/master-data/categories.json`
- Tests: `src/main/resources/master-data/tests.json`
- Test Parameters: `src/main/resources/master-data/test-parameters/<test-or-group>.json`
  - Multiple parameter files can be created inside `test-parameters/` (e.g. organized by panel or test).
  - Every parameter JSON file must be a JSON array of parameter definitions.

---

## 7. How to Add New Master Data Later
1. **Category**:
   - Add an entry to `categories.json` with a unique `code`, `name`, and optional `description`.
2. **Test**:
   - Add an entry to `tests.json` with a unique `code`, `name`, valid `sampleType`, and `categoryCode` matching an existing category.
3. **Parameters**:
   - Add entries in `test-parameters/<file>.json` specifying `code`, `testCode`, `name`, `dataType`, `inputType`, `displayOrder`, etc.
4. Run the application with `MASTER_DATA_SEED_ENABLED=true` (or run automated seeder tests).
