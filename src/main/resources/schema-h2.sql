-- H2 Database Schema for PT-Log
-- Place this file in src/main/resources/

-- Drop tables if they exist (for clean initialization)
DROP TABLE IF EXISTS PTLOG CASCADE;
DROP TABLE IF EXISTS PTLOG_PROJEKT CASCADE;

-- Create PTLOG_PROJEKT table
CREATE TABLE PTLOG_PROJEKT (
                               NAMN VARCHAR(255) PRIMARY KEY
);

-- Create PTLOG table
CREATE TABLE PTLOG (
                       ID IDENTITY PRIMARY KEY,
                       DATUM TIMESTAMP NOT NULL,
                       TYP VARCHAR(50) NOT NULL,
                       TESTNAMN VARCHAR(255) NOT NULL,
                       SYFTE VARCHAR(1000),
                       ANALYS CLOB,
                       PROJEKT VARCHAR(255) NOT NULL,
                       TESTARE VARCHAR(255)
);

-- Add foreign key constraint separately
ALTER TABLE PTLOG ADD CONSTRAINT FK_PROJEKT
    FOREIGN KEY (PROJEKT) REFERENCES PTLOG_PROJEKT(NAMN) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX IDX_PTLOG_PROJEKT ON PTLOG(PROJEKT);
CREATE INDEX IDX_PTLOG_DATUM ON PTLOG(DATUM DESC);
CREATE INDEX IDX_PTLOG_TESTNAMN ON PTLOG(TESTNAMN);

-- Insert some sample data (optional)
INSERT INTO PTLOG_PROJEKT (NAMN) VALUES ('Sample Project') ON DUPLICATE KEY UPDATE NAMN=NAMN;