-- INCREMENT BY phải khớp allocationSize trong entity
CREATE SEQUENCE iam_user_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE iam_role_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE iam_resource_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE iam_user (
    id              NUMBER(19) NOT NULL,
    username        VARCHAR2(50 CHAR) NOT NULL,
    password_hash   VARCHAR2(100 CHAR), -- null khi đăng nhập bằng SSO
    full_name       VARCHAR2(70 CHAR) NOT NULL,
    email           VARCHAR2(254 CHAR) NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by      NUMBER(19),
    updated_at      TIMESTAMP WITH TIME ZONE,
    updated_by      NUMBER(19),

    CONSTRAINT pk_iam_user PRIMARY KEY (id),
    CONSTRAINT uq_iam_user_username UNIQUE (username)
);

CREATE TABLE iam_role (
    id              NUMBER(19) NOT NULL,
    code            VARCHAR2(50 CHAR) NOT NULL,
    name            VARCHAR2(100 CHAR) NOT NULL,
    is_system       BOOLEAN DEFAULT FALSE NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by      NUMBER(19),
    updated_at      TIMESTAMP WITH TIME ZONE,
    updated_by      NUMBER(19),

    CONSTRAINT pk_iam_role PRIMARY KEY (id),
    CONSTRAINT uq_iam_role_code UNIQUE (code)
);

CREATE TABLE iam_user_role (
    user_id         NUMBER(19) NOT NULL,
    role_id         NUMBER(19) NOT NULL,

    CONSTRAINT pk_iam_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES iam_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES iam_role (id) ON DELETE CASCADE
);
-- Oracle không tự tạo index cho khóa ngoại
CREATE INDEX ix_user_role_role ON iam_user_role (role_id);

CREATE TABLE iam_resource (
    id              NUMBER(19) NOT NULL,
    code            VARCHAR2(200 CHAR) NOT NULL,
    name            VARCHAR2(100 CHAR) NOT NULL,
    parent_id       NUMBER(19), -- null ở mục gốc của cây menu
    resource_type   VARCHAR2(10 CHAR) NOT NULL,
    path            VARCHAR2(200 CHAR), -- null ở GROUP và API
    icon            VARCHAR2(100 CHAR),
    sort_order      NUMBER(10) DEFAULT 999 NOT NULL,
    is_visible      BOOLEAN DEFAULT TRUE NOT NULL,

    created_at      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by      NUMBER(19),
    updated_at      TIMESTAMP WITH TIME ZONE,
    updated_by      NUMBER(19),

    CONSTRAINT pk_iam_resource PRIMARY KEY (id),
    CONSTRAINT uq_iam_resource_code UNIQUE (code),
    CONSTRAINT fk_resource_parent FOREIGN KEY (parent_id) REFERENCES iam_resource (id),
    CONSTRAINT ck_resource_type CHECK (resource_type IN ('GROUP', 'MENU', 'API'))
);
CREATE INDEX ix_resource_parent ON iam_resource (parent_id);

-- Các scope mà resource hỗ trợ
CREATE TABLE iam_resource_scope (
    resource_id     NUMBER(19) NOT NULL,
    scope_code      VARCHAR2(10 CHAR) NOT NULL,

    CONSTRAINT pk_iam_resource_scope PRIMARY KEY (resource_id, scope_code),
    CONSTRAINT fk_resource_scope_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id) ON DELETE CASCADE,
    CONSTRAINT ck_resource_scope_code CHECK (scope_code IN ('CREATE', 'VIEW', 'UPDATE', 'DELETE'))
);

-- Khóa ngoại kép: chỉ cấp được scope mà resource đã khai báo
CREATE TABLE iam_role_permission (
    role_id         NUMBER(19) NOT NULL,
    resource_id     NUMBER(19) NOT NULL,
    scope_code      VARCHAR2(10 CHAR) NOT NULL,

    CONSTRAINT pk_iam_role_permission PRIMARY KEY (role_id, resource_id, scope_code),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES iam_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_scope FOREIGN KEY (resource_id, scope_code)
        REFERENCES iam_resource_scope (resource_id, scope_code) ON DELETE CASCADE
);
CREATE INDEX ix_role_permission_scope ON iam_role_permission (resource_id, scope_code);

-- Có VIEW trên resource_id thì được VIEW trên depends_on_id
CREATE TABLE iam_resource_dependency (
    resource_id     NUMBER(19) NOT NULL,
    depends_on_id   NUMBER(19) NOT NULL,

    CONSTRAINT pk_iam_resource_dependency PRIMARY KEY (resource_id, depends_on_id),
    CONSTRAINT fk_dependency_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id) ON DELETE CASCADE,
    CONSTRAINT fk_dependency_depends_on FOREIGN KEY (depends_on_id) REFERENCES iam_resource (id) ON DELETE CASCADE,
    CONSTRAINT ck_dependency_not_self CHECK (resource_id <> depends_on_id)
);
CREATE INDEX ix_dependency_depends_on ON iam_resource_dependency (depends_on_id);
