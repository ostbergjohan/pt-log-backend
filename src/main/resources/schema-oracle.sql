-- Oracle Database Schema for PT-Log
-- Place this file in src/main/resources/

-- Create PTLOG_PROJEKT table
CREATE TABLE PTLOG_PROJEKT (
                               NAMN VARCHAR2(255) PRIMARY KEY
);

-- Create sequence for PTLOG primary key
CREATE SEQUENCE PTLOG_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Create PTLOG table
CREATE TABLE PTLOG (
                       ID NUMBER DEFAULT PTLOG_SEQ.NEXTVAL PRIMARY KEY,
                       DATUM TIMESTAMP NOT NULL,
                       TYP VARCHAR2(50) NOT NULL,
                       TESTNAMN VARCHAR2(255) NOT NULL,
                       SYFTE VARCHAR2(1000),
                       ANALYS CLOB,
                       PROJEKT VARCHAR2(255) NOT NULL,
                       TESTARE VARCHAR2(255),
                       CONSTRAINT FK_PROJEKT FOREIGN KEY (PROJEKT) REFERENCES PTLOG_PROJEKT(NAMN) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IDX_PTLOG_PROJEKT ON PTLOG(PROJEKT);
CREATE INDEX IDX_PTLOG_DATUM ON PTLOG(DATUM DESC);
CREATE INDEX IDX_PTLOG_TESTNAMN ON PTLOG(TESTNAMN);

-- Commit changes
COMMIT;