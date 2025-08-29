-- =====================================================
-- File: create_ptlog.sql
-- Description: Creates table PTLOG
-- =====================================================

CREATE TABLE PTLOG (
    DATUM TIMESTAMP NOT NULL,
    TYP VARCHAR2(50) NOT NULL,
    TESTNAMN VARCHAR2(255) NOT NULL,
    SYFTE VARCHAR2(4000) NOT NULL,
    ANALYS VARCHAR2(4000),
    PROJEKT VARCHAR2(255) NOT NULL,
    TESTARE VARCHAR2(255) NOT NULL,
    CONSTRAINT FK_PROJEKT FOREIGN KEY (PROJEKT) REFERENCES PTLOG_PROJEKT(NAME),
    CONSTRAINT UQ_PROJECT_TEST UNIQUE (PROJEKT, TESTNAMN)
);

-- Optional indexes for faster queries
CREATE INDEX IDX_PTLOG_PROJEKT ON PTLOG(PROJEKT);
CREATE INDEX IDX_PTLOG_DATUM ON PTLOG(DATUM);

-- Optional: insert a sample test log
INSERT INTO PTLOG (DATUM, TYP, TESTNAMN, SYFTE, ANALYS, PROJEKT, TESTARE)
VALUES (SYSTIMESTAMP, 'ReferenceTest', '01_REF_SampleTest', 'Test purpose example', NULL, 'ProjectAlpha', 'Tester1');

COMMIT;
