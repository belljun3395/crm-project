CREATE
    USER 'crm-local'@'localhost' IDENTIFIED BY 'crm-local';
CREATE
    USER 'crm-local'@'%' IDENTIFIED BY 'crm-local';

GRANT ALL PRIVILEGES ON *.* TO
    'crm-local'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO
    'crm-local'@'%';

CREATE
    DATABASE crm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE
    DATABASE test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
