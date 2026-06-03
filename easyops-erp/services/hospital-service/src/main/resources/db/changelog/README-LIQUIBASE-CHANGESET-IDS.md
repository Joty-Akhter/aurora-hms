# Liquibase changeset IDs (hospital-service)

Liquibase identifies each applied migration by **`author` + `id`** (and file path), not by the numeric prefix on the SQL filename.

## Intentional filename ↔ inner-ID mismatches (do not “fix”)

Two SQL files under `changesets/` use a `--changeset` **id** whose leading number does **not** match the filename prefix. This was introduced by mistake when the files were added, but **the inner id must stay as-is** on every database that has already run these changesets.

| Filename | `--changeset` id (author:id) | Why you must not rename the id |
|----------|------------------------------|--------------------------------|
| `047-prescription-allergy-check-match-type.sql` | `hospital-service:046-prescription-allergy-check-match-type` | Renaming to `047-...` would register as a **new** changeset; Liquibase would re-run the `ALTER TABLE`, which can fail with “column already exists”. |
| `049-prescription-transmission-ncpdp-xml.sql` | `hospital-service:048-prescription-transmission-ncpdp-xml` | Same. Note: `048-ep-encounter-mode-immutable.sql` uses a **different** full id (`hospital-service:048-ep-encounter-mode-immutable-trigger`); there is no collision because Liquibase keys on the complete id string. |

### If you need to change the SQL body

Use `validCheckSum` in a follow-up changeset, or add a new changeset with a **new** id—never change the id of an already-deployed changeset.

### Deliberately aligning ids with filenames

That is only safe on **empty** databases that have never applied the old ids, or after a manual `DATABASECHANGELOG` surgery that is not recommended here. For normal maintenance: **leave these ids frozen** and rely on this document plus the comments in `db.changelog-master.xml` and the affected `.sql` files.
