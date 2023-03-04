-- IGNORE-ERROR
ALTER TABLE CA DROP CONSTRAINT FK_CA_CRL_SIGNER1;
-- IGNORE-ERROR
ALTER TABLE CAALIAS DROP CONSTRAINT FK_CAALIAS_CA1;
-- IGNORE-ERROR
ALTER TABLE CA_HAS_REQUESTOR DROP CONSTRAINT FK_CA_HAS_REQUESTOR_REQUESTOR1;
-- IGNORE-ERROR
ALTER TABLE CA_HAS_REQUESTOR DROP CONSTRAINT FK_CA_HAS_REQUESTOR_CA1;
-- IGNORE-ERROR
ALTER TABLE CA_HAS_PUBLISHER DROP CONSTRAINT FK_CA_HAS_PUBLISHER_PUBLISHER1;
-- IGNORE-ERROR
ALTER TABLE CA_HAS_PUBLISHER DROP CONSTRAINT FK_CA_HAS_PUBLISHER_CA1;
-- IGNORE-ERROR
ALTER TABLE CA_HAS_PROFILE DROP CONSTRAINT FK_CA_HAS_PROFILE_PROFILE1;
-- IGNORE-ERROR
ALTER TABLE CA_HAS_PROFILE DROP CONSTRAINT FK_CA_HAS_PROFILE_CA1;
-- IGNORE-ERROR
ALTER TABLE CRL DROP CONSTRAINT FK_CRL_CA1;
-- IGNORE-ERROR
ALTER TABLE CERT DROP CONSTRAINT FK_CERT_CA1;
-- IGNORE-ERROR
ALTER TABLE CERT DROP CONSTRAINT FK_CERT_REQUESTOR1;
-- IGNORE-ERROR
ALTER TABLE CERT DROP CONSTRAINT FK_CERT_PROFILE1;
-- IGNORE-ERROR
ALTER TABLE PUBLISHQUEUE DROP CONSTRAINT FK_PUBLISHQUEUE_PUBLISHER1;
-- IGNORE-ERROR
ALTER TABLE PUBLISHQUEUE DROP CONSTRAINT FK_PUBLISHQUEUE_CERT1;

DROP TABLE IF EXISTS DBSCHEMA;
DROP TABLE IF EXISTS SYSTEM_EVENT;
DROP TABLE IF EXISTS KEYPAIR_GEN;
DROP TABLE IF EXISTS SIGNER;
DROP TABLE IF EXISTS REQUESTOR;
DROP TABLE IF EXISTS PUBLISHER;
DROP TABLE IF EXISTS PROFILE;
DROP TABLE IF EXISTS CA;
DROP TABLE IF EXISTS CAALIAS;
DROP TABLE IF EXISTS CA_HAS_REQUESTOR;
DROP TABLE IF EXISTS CA_HAS_PUBLISHER;
DROP TABLE IF EXISTS CA_HAS_PROFILE;

DROP TABLE IF EXISTS CRL;
DROP TABLE IF EXISTS CERT;
DROP TABLE IF EXISTS PUBLISHQUEUE;

-- changeset xipki:1
CREATE TABLE DBSCHEMA (
    NAME VARCHAR(45) NOT NULL,
    VALUE2 VARCHAR(100) NOT NULL,
    CONSTRAINT PK_DBSCHEMA PRIMARY KEY (NAME)
);

INSERT INTO DBSCHEMA (NAME, VALUE2) VALUES ('VENDOR', 'XIPKI');
INSERT INTO DBSCHEMA (NAME, VALUE2) VALUES ('VERSION', '7');
INSERT INTO DBSCHEMA (NAME, VALUE2) VALUES ('X500NAME_MAXLEN', '350');

CREATE TABLE SYSTEM_EVENT (
    NAME VARCHAR(45) NOT NULL,
    EVENT_TIME BIGINT NOT NULL,
    EVENT_TIME2 TIMESTAMP,
    EVENT_OWNER VARCHAR(255) NOT NULL,
    CONSTRAINT PK_SYSTEM_EVENT PRIMARY KEY (NAME)
);

COMMENT ON COLUMN SYSTEM_EVENT.EVENT_TIME IS 'seconds since January 1, 1970, 00:00:00 GMT';

CREATE TABLE KEYPAIR_GEN (
    NAME VARCHAR(45) NOT NULL,
    TYPE VARCHAR(100) NOT NULL,
    CONF CLOB,
    CONSTRAINT PK_KEYPAIR_GEN PRIMARY KEY (NAME)
);

INSERT INTO KEYPAIR_GEN (NAME, TYPE) VALUES ('software', 'SOFTWARE');

CREATE TABLE SIGNER (
    NAME VARCHAR(45) NOT NULL,
    TYPE VARCHAR(100) NOT NULL,
    CERT VARCHAR(6000),
    CONF CLOB,
    CONSTRAINT PK_SIGNER PRIMARY KEY (NAME)
);

CREATE TABLE REQUESTOR (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    TYPE VARCHAR(100) NOT NULL,
    CONF CLOB,
    CONSTRAINT PK_REQUESTOR PRIMARY KEY (ID)
);

ALTER TABLE REQUESTOR ADD CONSTRAINT CONST_REQUESTOR_NAME UNIQUE (NAME);

CREATE TABLE PUBLISHER (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    TYPE VARCHAR(100) NOT NULL,
    CONF CLOB,
    CONSTRAINT PK_PUBLISHER PRIMARY KEY (ID)
);

COMMENT ON COLUMN PUBLISHER.NAME IS 'duplication is not permitted';

ALTER TABLE PUBLISHER ADD CONSTRAINT CONST_PUBLISHER_NAME UNIQUE (NAME);

CREATE TABLE PROFILE (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    TYPE VARCHAR(100) NOT NULL,
    CONF CLOB,
    CONSTRAINT PK_PROFILE PRIMARY KEY (ID)
);

COMMENT ON COLUMN PROFILE.NAME IS 'duplication is not permitted';
COMMENT ON COLUMN PROFILE.CONF IS 'profile data, depends on the type';

ALTER TABLE PROFILE ADD CONSTRAINT CONST_PROFILE_NAME UNIQUE (NAME);

CREATE TABLE CA (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    STATUS VARCHAR(10) NOT NULL,
    NEXT_CRLNO BIGINT,
    CRL_SIGNER_NAME VARCHAR(45),
    SUBJECT VARCHAR(350) NOT NULL,
    REV_INFO VARCHAR(200),
    CERT VARCHAR(6000) NOT NULL,
    SIGNER_TYPE VARCHAR(100) NOT NULL,
    SIGNER_CONF CLOB NOT NULL,
    CERTCHAIN CLOB,
    CONF CLOB NOT NULL,
    CONSTRAINT PK_CA PRIMARY KEY (ID)
);

COMMENT ON COLUMN CA.NAME IS 'duplication is not permitted';
COMMENT ON COLUMN CA.STATUS IS 'valid values: active, inactive';
COMMENT ON COLUMN CA.REV_INFO IS 'CA revocation information';
COMMENT ON COLUMN CA.CERTCHAIN IS 'Certificate chain without CA''s certificate';

ALTER TABLE CA ADD CONSTRAINT CONST_CA_NAME UNIQUE (NAME);

CREATE TABLE CAALIAS (
    NAME VARCHAR(45) NOT NULL,
    CA_ID SMALLINT NOT NULL,
    CONSTRAINT PK_CAALIAS PRIMARY KEY (NAME)
);

CREATE TABLE CA_HAS_REQUESTOR (
    CA_ID SMALLINT NOT NULL,
    REQUESTOR_ID SMALLINT NOT NULL,
    PERMISSION INT,
    PROFILES VARCHAR(500),
    CONSTRAINT PK_CA_HAS_REQUESTOR PRIMARY KEY (CA_ID, REQUESTOR_ID)
);

CREATE TABLE CA_HAS_PUBLISHER (
    CA_ID SMALLINT NOT NULL,
    PUBLISHER_ID SMALLINT NOT NULL,
    CONSTRAINT PK_CA_HAS_PUBLISHER PRIMARY KEY (CA_ID, PUBLISHER_ID)
);

CREATE TABLE CA_HAS_PROFILE (
    CA_ID SMALLINT NOT NULL,
    PROFILE_ID SMALLINT NOT NULL,
    CONSTRAINT PK_CA_HAS_PROFILE PRIMARY KEY (CA_ID, PROFILE_ID)
);

-- changeset xipki:2
CREATE TABLE CRL (
    ID INT NOT NULL,
    CA_ID SMALLINT NOT NULL,
    CRL_SCOPE SMALLINT NOT NULL,
    CRL_NO BIGINT NOT NULL,
    THISUPDATE BIGINT NOT NULL,
    NEXTUPDATE BIGINT,
    DELTACRL SMALLINT NOT NULL,
    BASECRL_NO BIGINT,
    SHA1 CHAR(28) NOT NULL,
    CRL CLOB NOT NULL,
    CONSTRAINT PK_CRL PRIMARY KEY (ID)
);

COMMENT ON COLUMN CRL.CRL_SCOPE IS 'CRL scope, reserved for future use';
COMMENT ON COLUMN CRL.SHA1 IS 'base64 encoded SHA1 fingerprint of the CRL';

ALTER TABLE CRL ADD CONSTRAINT CONST_CA_CRLNO UNIQUE (CA_ID, CRL_NO);

CREATE TABLE CERT (
    ID BIGINT NOT NULL,
    CA_ID SMALLINT NOT NULL,
    SN VARCHAR(40) NOT NULL,
    PID SMALLINT NOT NULL,
    RID SMALLINT,
    FP_S BIGINT NOT NULL,
    FP_SAN BIGINT,
    FP_RS BIGINT,
    LUPDATE BIGINT NOT NULL,
    NBEFORE BIGINT NOT NULL,
    NAFTER BIGINT NOT NULL,
    REV SMALLINT NOT NULL,
    RR SMALLINT,
    RT BIGINT,
    RIT BIGINT,
    EE SMALLINT NOT NULL,
    SUBJECT VARCHAR(350) NOT NULL,
    TID VARCHAR(43),
    CRL_SCOPE SMALLINT NOT NULL,
    SHA1 CHAR(28) NOT NULL,
    REQ_SUBJECT VARCHAR(350),
    CERT VARCHAR(6000) NOT NULL,
    PRIVATE_KEY VARCHAR(6000),
    CONSTRAINT PK_CERT PRIMARY KEY (ID)
);

COMMENT ON COLUMN CERT.CA_ID IS 'Issuer (CA) id';
COMMENT ON COLUMN CERT.SN IS 'serial number';
COMMENT ON COLUMN CERT.PID IS 'certificate profile id';
COMMENT ON COLUMN CERT.RID IS 'requestor id';
COMMENT ON COLUMN CERT.FP_S IS 'first 8 bytes of the SHA1 sum of the subject';
COMMENT ON COLUMN CERT.FP_SAN IS 'first 8 bytes of the SHA1 sum of the extension value of SubjectAltNames';
COMMENT ON COLUMN CERT.FP_RS IS 'first 8 bytes of the SHA1 sum of the requested subject';
COMMENT ON COLUMN CERT.LUPDATE IS 'last update, seconds since January 1, 1970, 00:00:00 GMT';
COMMENT ON COLUMN CERT.NBEFORE IS 'notBefore, seconds since January 1, 1970, 00:00:00 GMT';
COMMENT ON COLUMN CERT.NAFTER IS 'notAfter, seconds since January 1, 1970, 00:00:00 GMT';
COMMENT ON COLUMN CERT.REV IS 'whether the certificate is revoked';
COMMENT ON COLUMN CERT.RR IS 'revocation reason';
COMMENT ON COLUMN CERT.RT IS 'revocation time, seconds since January 1, 1970, 00:00:00 GMT';
COMMENT ON COLUMN CERT.RIT IS 'revocation invalidity time, seconds since January 1, 1970, 00:00:00 GMT';
COMMENT ON COLUMN CERT.EE IS 'whether it is an end entity cert';
COMMENT ON COLUMN CERT.TID IS 'base64 encoded transactionId, maximal 256 bit';
COMMENT ON COLUMN CERT.CRL_SCOPE IS 'CRL scope, reserved for future use';
COMMENT ON COLUMN CERT.SHA1 IS 'base64 encoded SHA1 fingerprint of the certificate';
COMMENT ON COLUMN CERT.CERT IS 'Base64 encoded certificate';
COMMENT ON COLUMN CERT.PRIVATE_KEY IS 'Base64-encoded encrypted PKCS#8 private key';

ALTER TABLE CERT ADD CONSTRAINT CONST_CA_SN UNIQUE (CA_ID, SN);

CREATE INDEX IDX_CA_FPS ON CERT(CA_ID, FP_S, FP_SAN);

CREATE TABLE PUBLISHQUEUE (
    CID BIGINT NOT NULL,
    PID SMALLINT NOT NULL,
    CA_ID SMALLINT NOT NULL
);

-- changeset xipki:3
ALTER TABLE CA ADD CONSTRAINT FK_CA_CRL_SIGNER1
    FOREIGN KEY (CRL_SIGNER_NAME) REFERENCES SIGNER (NAME)
    ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE CAALIAS ADD CONSTRAINT FK_CAALIAS_CA1
    FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE CA_HAS_REQUESTOR ADD CONSTRAINT FK_CA_HAS_REQUESTOR_REQUESTOR1
    FOREIGN KEY (REQUESTOR_ID) REFERENCES REQUESTOR (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE CA_HAS_REQUESTOR ADD CONSTRAINT FK_CA_HAS_REQUESTOR_CA1
    FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE CA_HAS_PUBLISHER ADD CONSTRAINT FK_CA_HAS_PUBLISHER_PUBLISHER1
    FOREIGN KEY (PUBLISHER_ID) REFERENCES PUBLISHER (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE CA_HAS_PUBLISHER ADD CONSTRAINT FK_CA_HAS_PUBLISHER_CA1
    FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE CA_HAS_PROFILE ADD CONSTRAINT FK_CA_HAS_PROFILE_PROFILE1
    FOREIGN KEY (PROFILE_ID) REFERENCES PROFILE (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE CA_HAS_PROFILE ADD CONSTRAINT FK_CA_HAS_PROFILE_CA1
    FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

-- changeset xipki:4
ALTER TABLE CRL ADD CONSTRAINT FK_CRL_CA1
    FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE CERT ADD CONSTRAINT FK_CERT_CA1
    FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE CERT ADD CONSTRAINT FK_CERT_REQUESTOR1
    FOREIGN KEY (RID) REFERENCES REQUESTOR (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE CERT ADD CONSTRAINT FK_CERT_PROFILE1
    FOREIGN KEY (PID) REFERENCES PROFILE (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE PUBLISHQUEUE ADD CONSTRAINT FK_PUBLISHQUEUE_PUBLISHER1
    FOREIGN KEY (PID) REFERENCES PUBLISHER (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE PUBLISHQUEUE ADD CONSTRAINT FK_PUBLISHQUEUE_CERT1
    FOREIGN KEY (CID) REFERENCES CERT (ID)
    ON UPDATE NO ACTION ON DELETE CASCADE;

