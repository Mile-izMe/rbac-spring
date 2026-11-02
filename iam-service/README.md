# GENERAL FLOW
iam_user ──1:N── iam_user_role ──N:1── iam_role
│ 1:N
▼
iam_role_permission
│ N:1 (composite-key: resource_id + scope_code)
▼
iam_resource ───────1:N──────── iam_resource_scope
│      │
│      └─ parent_id: point to self (menu tree)
│
└─ iam_resource_dependency: nối resource với resource (inherit VIEW)

# KEY TABLE
- IAM_ROLE_PERMISSION: Main table to permit actions for all users.
- IAM_RESOURCE: Provide name of resource that allow user to access.
- IAM_RESOURCE_SCOPE: Provide the action for the resource.
- IAM_RESOURCE_DEPENDENCY: Provide the child of the parent resource (Dropdown, ...).