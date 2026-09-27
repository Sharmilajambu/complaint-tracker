# Category → authority mapping

Defined in `AuthorityMapper.java` and seeded as separate logins in `AuthoritySeeder.java`.

| Category | Level 0 login | Level 0 designation | Level 1 login | Level 1 designation | Level 2 |
|----------|---------------|---------------------|---------------|---------------------|---------|
| mess | `auth_mess_l0` | Mess Committee | `auth_mess_l1` | Hostel Warden | `auth_principal_l2` → Principal |
| hostel | `auth_hostel_l0` | Hostel Warden | `auth_hostel_l1` | Dean of Student Affairs | Principal |
| faculty | `auth_faculty_l0` | Department HOD | `auth_faculty_l1` | Dean of Academics | Principal |
| electrical | `auth_electrical_l0` | Maintenance/Estate Office | `auth_electrical_l1` | Estate Officer | Principal |
| water | `auth_water_l0` | Estate Office | `auth_water_l1` | Estate Officer | Principal |
| harassment | `auth_harassment_l0` | Anti-Ragging Committee | `auth_harassment_l1` | Dean of Student Affairs | Principal |
| other | `auth_other_l0` | Department Office | `auth_other_l1` | Warden/HOD | Principal |

**Notes**

- Each Level 0 and Level 1 row is a **separate account**, even when the designation name repeats across categories (e.g. Hostel Warden at mess L1 vs hostel L0).
- **Principal** is one shared login (`auth_principal_l2`, category `all`, level 2) for every category after final escalation.
- Passwords are printed **once** in the server console when each account is first created.
- Complaints route via `assigned_designation` (current holder), set on submit to Level 0 and updated on each escalation to match `escalated_to` in the log.
