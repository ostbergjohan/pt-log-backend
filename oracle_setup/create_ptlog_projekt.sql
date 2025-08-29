-- =====================================================
-- File: create_ptlog_projekt.sql
-- Description: Creates table PTLOG_PROJEKT
-- =====================================================

CREATE TABLE PTLOG_PROJEKT (
    NAME VARCHAR2(255) NOT NULL PRIMARY KEY
);

-- Optional: insert some sample projects
INSERT INTO PTLOG_PROJEKT (NAME) VALUES ('ProjectAlpha');
INSERT INTO PTLOG_PROJEKT (NAME) VALUES ('ProjectBeta');

COMMIT;
