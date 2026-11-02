-- Resource: tạo mục cha trước vì mục con cần id của cha
INSERT INTO iam_resource (id, code, name, resource_type, sort_order)
VALUES (iam_resource_seq.NEXTVAL, 'quan-tri-he-thong', 'Quản trị hệ thống', 'GROUP', 1);

INSERT INTO iam_resource (id, code, name, resource_type, sort_order)
VALUES (iam_resource_seq.NEXTVAL, 'danh-muc', 'Danh mục', 'GROUP', 2);

INSERT INTO iam_resource (id, code, name, parent_id, resource_type, path, sort_order)
VALUES (iam_resource_seq.NEXTVAL, 'quan-tri-he-thong/nhom-nguoi-dung', 'Nhóm người dùng',
        (SELECT id FROM iam_resource WHERE code = 'quan-tri-he-thong'),
        'MENU', '/quan-tri-he-thong/nhom-nguoi-dung', 1);

INSERT INTO iam_resource (id, code, name, parent_id, resource_type, path, sort_order)
VALUES (iam_resource_seq.NEXTVAL, 'quan-tri-he-thong/phan-quyen', 'Phân quyền',
        (SELECT id FROM iam_resource WHERE code = 'quan-tri-he-thong'),
        'MENU', '/quan-tri-he-thong/phan-quyen', 2);

INSERT INTO iam_resource (id, code, name, parent_id, resource_type, path, sort_order)
VALUES (iam_resource_seq.NEXTVAL, 'quan-ly-nhan-su/nhan-vien', 'Người dùng',
        (SELECT id FROM iam_resource WHERE code = 'quan-tri-he-thong'),
        'MENU', '/quan-ly-nhan-su/nhan-vien', 3);

INSERT INTO iam_resource (id, code, name, parent_id, resource_type, path, sort_order)
VALUES (iam_resource_seq.NEXTVAL, 'danh-muc/ki-nang', 'Kỹ năng',
        (SELECT id FROM iam_resource WHERE code = 'danh-muc'),
        'MENU', '/danh-muc/ki-nang', 1);

-- API dùng chung, không hiện trên menu
INSERT INTO iam_resource (id, code, name, resource_type, is_visible)
VALUES (iam_resource_seq.NEXTVAL, 'dropdown', 'Dropdown', 'API', FALSE);

-- Scope mà từng resource hỗ trợ; GROUP không có scope
INSERT INTO iam_resource_scope (resource_id, scope_code)
SELECT r.id, s.scope_code
FROM iam_resource r
CROSS JOIN (
    SELECT 'CREATE' AS scope_code FROM dual
    UNION ALL SELECT 'VIEW' FROM dual
    UNION ALL SELECT 'UPDATE' FROM dual
    UNION ALL SELECT 'DELETE' FROM dual
) s
WHERE r.code IN ('quan-tri-he-thong/nhom-nguoi-dung', 'quan-ly-nhan-su/nhan-vien', 'danh-muc/ki-nang');

INSERT INTO iam_resource_scope (resource_id, scope_code)
SELECT r.id, s.scope_code
FROM iam_resource r
CROSS JOIN (
    SELECT 'VIEW' AS scope_code FROM dual
    UNION ALL SELECT 'UPDATE' FROM dual
) s
WHERE r.code = 'quan-tri-he-thong/phan-quyen';

INSERT INTO iam_resource_scope (resource_id, scope_code)
SELECT id, 'VIEW' FROM iam_resource WHERE code = 'dropdown';

-- Có VIEW màn Người dùng thì được VIEW dropdown
INSERT INTO iam_resource_dependency (resource_id, depends_on_id)
SELECT a.id, b.id
FROM iam_resource a
CROSS JOIN iam_resource b
WHERE a.code = 'quan-ly-nhan-su/nhan-vien' AND b.code = 'dropdown';

-- Role
INSERT INTO iam_role (id, code, name, is_system)
VALUES (iam_role_seq.NEXTVAL, 'ADMIN', 'Quản trị viên', TRUE);

INSERT INTO iam_role (id, code, name)
VALUES (iam_role_seq.NEXTVAL, 'HR', 'Nhân sự');

INSERT INTO iam_role (id, code, name)
VALUES (iam_role_seq.NEXTVAL, 'EMPLOYEE', 'Nhân viên');

-- ADMIN có mọi scope đã khai báo
INSERT INTO iam_role_permission (role_id, resource_id, scope_code)
SELECT ro.id, rs.resource_id, rs.scope_code
FROM iam_role ro
CROSS JOIN iam_resource_scope rs
WHERE ro.code = 'ADMIN';

INSERT INTO iam_role_permission (role_id, resource_id, scope_code)
SELECT ro.id, re.id, s.scope_code
FROM iam_role ro
CROSS JOIN iam_resource re
CROSS JOIN (
    SELECT 'VIEW' AS scope_code FROM dual
    UNION ALL SELECT 'UPDATE' FROM dual
) s
WHERE ro.code = 'HR' AND re.code = 'quan-ly-nhan-su/nhan-vien';

INSERT INTO iam_role_permission (role_id, resource_id, scope_code)
SELECT ro.id, re.id, 'VIEW'
FROM iam_role ro
CROSS JOIN iam_resource re
WHERE ro.code = 'EMPLOYEE' AND re.code = 'danh-muc/ki-nang';
