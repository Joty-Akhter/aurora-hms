#!/usr/bin/env python3

"""
Generate Liquibase SQL seed file for hospital_pharmacy.manufacturers and hospital_pharmacy.drugs
from the scraped brands_data.json file.

Usage:
  cd easyops-erp
  python tools/generate_hospital_pharmacy_seed_from_brands.py \
    --json /Users/til/workspace/mine/ehr/scraped_data_paginated/brands_data.json

This will overwrite:
  services/hospital-service/src/main/resources/db/changelog/changesets/011-hospital-pharmacy-drug-master-seed.sql
"""

import argparse
import json
import uuid
from collections import OrderedDict
from pathlib import Path
from typing import Any, Dict, Iterable, Tuple


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate hospital_pharmacy drug master Liquibase seed SQL from brands_data.json"
    )
    parser.add_argument(
        "--json",
        required=True,
        help="Path to brands_data.json (scraped_data_paginated)",
    )
    parser.add_argument(
        "--output",
        default="services/hospital-service/src/main/resources/db/changelog/changesets/011-hospital-pharmacy-drug-master-seed.sql",
        help="Output SQL file path (relative to repo root or absolute).",
    )
    return parser.parse_args()


def load_brands(json_path: Path) -> Iterable[Dict[str, Any]]:
    """
    Load the brands JSON.

    The user mentioned a large file with many brands under scraped_data_paginated.
    We assume it's either:
      - a JSON array of brand objects, or
      - a JSON object with a top-level "results" list.
    """
    with json_path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        # Try common container keys
        for key in ("results", "items", "data"):
            if key in data and isinstance(data[key], list):
                return data[key]
    raise ValueError("Unsupported JSON structure for brands_data.json")


def sql_escape_literal(value: str) -> str:
    """
    Escape text for SQL single-quoted literal.
    """
    return value.replace("'", "''")


def normalize_str(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


def build_seed_sets(
    brands: Iterable[Dict[str, Any]],
) -> Tuple[Dict[str, uuid.UUID], Iterable[Tuple[uuid.UUID, Dict[str, Any]]]]:
    """
    Build:
      - manufacturers dict: company_name (normalized) -> UUID
      - drugs iterable: (drug_id, brand_record_with_manufacturer_id)
    """
    manufacturers: "OrderedDict[str, uuid.UUID]" = OrderedDict()
    drugs_with_ids: list[Tuple[uuid.UUID, Dict[str, Any]]] = []

    for brand in brands:
        company_name = normalize_str(brand.get("company_name"))
        if not company_name:
            # Skip drugs with no manufacturer
            continue

        if company_name.lower() not in manufacturers:
            manufacturers[company_name.lower()] = uuid.uuid4()

        manufacturer_id = manufacturers[company_name.lower()]

        # Attach manufacturer_id to the brand dict copy
        brand_copy = dict(brand)
        brand_copy["__manufacturer_id__"] = manufacturer_id

        drug_id = uuid.uuid4()
        drugs_with_ids.append((drug_id, brand_copy))

    return manufacturers, drugs_with_ids


def write_sql(
    output_path: Path,
    manufacturers: Dict[str, uuid.UUID],
    drugs_with_ids: Iterable[Tuple[uuid.UUID, Dict[str, Any]]],
) -> None:
    """
    Write Liquibase-formatted SQL for manufacturers and drugs.
    """
    with output_path.open("w", encoding="utf-8") as out:
        # Header
        out.write(
            "--liquibase formatted sql\n"
            "--changeset easyops:hosp-pharm-011-drug-master-seed context:hospital-pharmacy\n"
            "--comment: Seed hospital_pharmacy.manufacturers and hospital_pharmacy.drugs from scraped brands_data.json\n\n"
        )

        # Manufacturers
        out.write("-- Insert manufacturers (deduplicated by company_name)\n")
        for company_norm, manufacturer_id in manufacturers.items():
            company_display = company_norm  # we only had the normalized name
            out.write(
                "INSERT INTO hospital_pharmacy.manufacturers (id, name, short_code, country, contact_info, is_active, created_at, updated_at)\n"
                f"VALUES ('{manufacturer_id}', '{sql_escape_literal(company_display)}', NULL, NULL, NULL, TRUE, NOW(), NOW());\n"
            )
        out.write("\n")

        # Drugs
        out.write("-- Insert drugs derived from brands_data.json\n")
        for drug_id, brand in drugs_with_ids:
            manufacturer_id: uuid.UUID = brand["__manufacturer_id__"]

            generic_name = normalize_str(brand.get("generic_name") or brand.get("generic"))
            brand_name = normalize_str(brand.get("brand_name") or brand.get("brand"))
            dose = normalize_str(brand.get("dose") or brand.get("strength"))
            dose_form = normalize_str(brand.get("dose_form") or brand.get("form"))
            route = normalize_str(brand.get("route"))

            # Map scraped fields to schema columns
            strength = dose or None
            form = dose_form or None

            # For now, we leave these as NULL / defaults
            pack_size = None
            unit_of_measure = None
            therapeutic_class_id = None
            controlled_drug_flag = False
            batch_required = True
            expiry_required = True

            def lit(val: Any) -> str:
                if val is None or val == "":
                    return "NULL"
                return f"'{sql_escape_literal(str(val))}'"

            out.write(
                "INSERT INTO hospital_pharmacy.drugs (\n"
                "    id, generic_name, brand_name, strength, form, route,\n"
                "    pack_size, unit_of_measure, therapeutic_class_id,\n"
                "    is_active, controlled_drug_flag, batch_required, expiry_required,\n"
                "    manufacturer_id, created_at, updated_at\n"
                ")\n"
                "VALUES (\n"
                f"    '{drug_id}',\n"
                f"    {lit(generic_name)},\n"
                f"    {lit(brand_name)},\n"
                f"    {lit(strength)},\n"
                f"    {lit(form)},\n"
                f"    {lit(route)},\n"
                f"    {lit(pack_size)},\n"
                f"    {lit(unit_of_measure)},\n"
                f"    {lit(therapeutic_class_id)},\n"
                "    TRUE,\n"
                f"    {str(controlled_drug_flag).upper()},\n"
                f"    {str(batch_required).upper()},\n"
                f"    {str(expiry_required).upper()},\n"
                f"    '{manufacturer_id}',\n"
                "    NOW(), NOW()\n"
                ");\n\n"
            )


def main() -> None:
    args = parse_args()
    json_path = Path(args.json).expanduser().resolve()
    output_path = Path(args.output).expanduser()

    if not json_path.exists():
        raise SystemExit(f"JSON file not found: {json_path}")

    brands = list(load_brands(json_path))
    manufacturers, drugs_with_ids = build_seed_sets(brands)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    write_sql(output_path, manufacturers, drugs_with_ids)

    print(f"Wrote seed SQL to {output_path}")
    print(f"Manufacturers: {len(manufacturers)}")
    print(f"Drugs:        {len(list(drugs_with_ids))}")


if __name__ == "__main__":
    main()

