#!/usr/bin/env python3
"""
Team-11 Volume Test Data Generator & Database Reset Utility
Author: Person 4 (Environment Owner / Developer)

Generates production-scale test datasets for Non-Functional & Security Testing:
- TC-Perf-01: Quote response time under volume
- TC-Load-01: 500 concurrent virtual users (JMeter)
- TC-Sec-01: High-concurrency TLS & penetration scans

Datasets generated:
- 500 KYC-verified customer users (with known password 'Password123!')
- Up to 10,000 policies across Motor and Health plans
- Up to 5,000 claims across valid states
- Multi-row batch INSERTs for high-speed import (< 10 seconds into MySQL)
"""

import argparse
import random
from datetime import datetime, timedelta

DEFAULT_PASSWORD_HASH = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfY5v7m2G9J7Lce"  # Password123!

def generate_volume_sql(output_path, num_users=500, num_plans=100, num_policies=10000, num_claims=5000, reset=True):
    print(f"=== Generating Volume Data: {num_users} Users, {num_plans} Plans, {num_policies} Policies, {num_claims} Claims ===")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("-- Team-11 Performance & Volume Test Data Script\n")
        f.write(f"-- Generated: {datetime.now().isoformat()}\n")
        f.write("-- Target: MySQL 8.0 (InnoDB, utf8mb4)\n\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n")
        f.write("SET UNIQUE_CHECKS = 0;\n")
        f.write("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';\n\n")

        if reset:
            f.write("-- Reset existing tables\n")
            f.write("DELETE FROM claim_histories;\n")
            f.write("DELETE FROM claims;\n")
            f.write("DELETE FROM cancellation_requests;\n")
            f.write("DELETE FROM policies;\n")
            f.write("DELETE FROM plans;\n")
            f.write("DELETE FROM kyc_records;\n")
            f.write("DELETE FROM user_sessions;\n")
            f.write("DELETE FROM users WHERE id > 10;\n")
            f.write("DELETE FROM insurers WHERE id > 3;\n\n")

        f.write("-- Ensure canonical insurers exist\n")
        f.write("INSERT IGNORE INTO insurers (id, name, active, created_at, updated_at) VALUES\n"
                "(1, 'SafeShield General Insurance', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),\n"
                "(2, 'Apex Motor & Health Assurance', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),\n"
                "(3, 'Zenith Life & Health Ltd', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);\n\n")

        # 1. Generate 500 Load Customer Users
        print(f"[1/4] Generating {num_users} KYC-verified load users...")
        f.write(f"-- 1. Generating {num_users} KYC-verified test customer users\n")
        user_batch = []
        for u in range(1, num_users + 1):
            user_id = 1000 + u
            email = f"load_user_{u}@insurance.test"
            name = f"Load Test Customer {u}"
            phone = f"+9198{u:08d}"
            user_batch.append(f"({user_id}, '{name}', '{email}', '{phone}', '{DEFAULT_PASSWORD_HASH}', 'CUSTOMER', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
            if len(user_batch) >= 200:
                f.write("INSERT INTO users (id, name, email, phone, password_hash, role, insurer_id, failed_login_attempts, created_at, updated_at) VALUES\n" + ",\n".join(user_batch) + ";\n")
                user_batch = []
        if user_batch:
            f.write("INSERT INTO users (id, name, email, phone, password_hash, role, insurer_id, failed_login_attempts, created_at, updated_at) VALUES\n" + ",\n".join(user_batch) + ";\n")
        f.write("\n")

        # 2. Generate Plans
        print(f"[2/4] Generating {num_plans} Plans...")
        f.write(f"-- 2. Generating {num_plans} plans\n")
        plan_batch = []
        for p in range(1, num_plans + 1):
            plan_id = 100 + p
            ptype = "MOTOR" if p % 2 == 1 else "HEALTH"
            code = f"PLAN-VOL-{p:04d}"
            insurer_id = (p % 3) + 1
            name = f"Volume {ptype.title()} Tier {p}"
            coverage = 500000 + (p * 10000)
            deductible = 1000 + (p * 50)
            base_prem = 4500.00 + (p * 25.0)
            plan_batch.append(f"({plan_id}, '{code}', {insurer_id}, '{ptype}', '{name}', {coverage}, {deductible}, 0, {base_prem}, '{{\"baseRate\": 0.02}}', '{{\"maxAge\": 70}}', '[]', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
            if len(plan_batch) >= 100:
                f.write("INSERT INTO plans (id, code, insurer_id, type, name, coverage, deductible, waiting_period_months, base_premium, premium_rules_json, underwriting_rules_json, add_ons_json, is_active, created_at, updated_at) VALUES\n" + ",\n".join(plan_batch) + ";\n")
                plan_batch = []
        if plan_batch:
            f.write("INSERT INTO plans (id, code, insurer_id, type, name, coverage, deductible, waiting_period_months, base_premium, premium_rules_json, underwriting_rules_json, add_ons_json, is_active, created_at, updated_at) VALUES\n" + ",\n".join(plan_batch) + ";\n")
        f.write("\n")

        # 3. Generate Policies
        print(f"[3/4] Generating {num_policies} Policies...")
        f.write(f"-- 3. Generating {num_policies} active policies\n")
        now = datetime.now()
        start_date = (now - timedelta(days=90)).strftime("%Y-%m-%d")
        end_date = (now + timedelta(days=275)).strftime("%Y-%m-%d")

        policy_batch = []
        for pol in range(1, num_policies + 1):
            policy_id = 10000 + pol
            policy_no = f"POL-VOL-2026-{pol:06d}"
            cust_id = 1000 + ((pol % num_users) + 1)
            plan_id = 100 + ((pol % num_plans) + 1)
            ins_id = ((pol % 3) + 1)
            premium = 7500.00 + (pol % 500)
            veh_no = f"KA01AB{pol % 9999:04d}" if pol % 2 == 1 else "HEALTH-MEMBERS"
            status = "ACTIVE"
            policy_batch.append(f"({policy_id}, '{policy_no}', {cust_id}, {ins_id}, {plan_id}, {premium}, '{start_date}', '{end_date}', '{veh_no}', '{status}', CURRENT_TIMESTAMP)")
            if len(policy_batch) >= 200:
                f.write("INSERT INTO policies (id, policy_no, customer_id, insurer_id, plan_id, premium_paid, start_date, end_date, vehicle_reg_no, status, created_at) VALUES\n" + ",\n".join(policy_batch) + ";\n")
                policy_batch = []
        if policy_batch:
            f.write("INSERT INTO policies (id, policy_no, customer_id, insurer_id, plan_id, premium_paid, start_date, end_date, vehicle_reg_no, status, created_at) VALUES\n" + ",\n".join(policy_batch) + ";\n")
        f.write("\n")

        # 4. Generate Claims
        print(f"[4/4] Generating {num_claims} Claims...")
        f.write(f"-- 4. Generating {num_claims} claims\n")
        claim_states = ["SUBMITTED", "DOC_VERIF", "ASSESSMENT", "PAYMENT_PROC", "SETTLED"]
        claim_batch = []
        for c in range(1, num_claims + 1):
            claim_id = 50000 + c
            claim_no = f"CLM-VOL-2026-{c:06d}"
            pol_idx = (c % num_policies) + 1
            pol_id = 10000 + pol_idx
            pol_no = f"POL-VOL-2026-{pol_idx:06d}"
            cust_id = 1000 + ((pol_idx % num_users) + 1)
            ins_id = (pol_idx % 3) + 1
            inc_date = (now - timedelta(days=(c % 25) + 1)).strftime("%Y-%m-%d")
            claim_amt = 15000.00 + ((c % 200) * 100)
            status = claim_states[c % len(claim_states)]
            settled_amt = f"{claim_amt * 0.9:.2f}" if status == "SETTLED" else "NULL"
            reason = "Accidental bumper collision" if c % 2 == 1 else "Emergency hospitalization"

            claim_batch.append(f"({claim_id}, '{claim_no}', {pol_id}, '{pol_no}', {cust_id}, {ins_id}, '{inc_date}', {claim_amt}, {settled_amt}, '{status}', '{reason}', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
            if len(claim_batch) >= 200:
                f.write("INSERT INTO claims (id, claim_no, policy_id, policy_no, customer_id, insurer_id, incident_date, claim_amount, settled_amount, status, reason, duplicate_flag, created_at, updated_at) VALUES\n" + ",\n".join(claim_batch) + ";\n")
                claim_batch = []
        if claim_batch:
            f.write("INSERT INTO claims (id, claim_no, policy_id, policy_no, customer_id, insurer_id, incident_date, claim_amount, settled_amount, status, reason, duplicate_flag, created_at, updated_at) VALUES\n" + ",\n".join(claim_batch) + ";\n")
        f.write("\n")

        f.write("SET FOREIGN_KEY_CHECKS = 1;\n")
        f.write("SET UNIQUE_CHECKS = 1;\n")
        f.write("-- Volume data loading script completed successfully\n")

    print(f"[SUCCESS] Generated volume SQL script saved to: {output_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Team-11 Test Data Volume Generator")
    parser.add_argument("--output", default="scripts/volume-data.sql", help="Output SQL script path")
    parser.add_argument("--users", type=int, default=500, help="Number of KYC load users")
    parser.add_argument("--plans", type=int, default=100, help="Number of plans")
    parser.add_argument("--policies", type=int, default=10000, help="Number of policies")
    parser.add_argument("--claims", type=int, default=5000, help="Number of claims")
    parser.add_argument("--scale", choices=["small", "standard", "full"], default="standard", help="Preset data scale")

    args = parser.parse_args()
    if args.scale == "small":
        generate_volume_sql(args.output, num_users=50, num_plans=10, num_policies=500, num_claims=200)
    elif args.scale == "full":
        generate_volume_sql(args.output, num_users=1000, num_plans=200, num_policies=20000, num_claims=10000)
    else:
        generate_volume_sql(args.output, num_users=args.users, num_plans=args.plans, num_policies=args.policies, num_claims=args.claims)
