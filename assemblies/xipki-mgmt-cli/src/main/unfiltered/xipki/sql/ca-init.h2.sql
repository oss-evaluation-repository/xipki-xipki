-- IGNORE-ERROR
ALTER TABLE CRL  DROP CONSTRAINT FK_CRL_CA1;
-- IGNORE-ERROR
ALTER TABLE CERT DROP CONSTRAINT FK_CERT_CA1;
-- IGNORE-ERROR
ALTER TABLE CERT DROP CONSTRAINT FK_CERT_REQUESTOR1;
-- IGNORE-ERROR
ALTER TABLE CERT DROP CONSTRAINT FK_CERT_PROFILE1;

DROP TABLE IF EXISTS DBSCHEMA;
DROP TABLE IF EXISTS PROFILE;
DROP TABLE IF EXISTS REQUESTOR;
DROP TABLE IF EXISTS CA;
DROP TABLE IF EXISTS CRL;
DROP TABLE IF EXISTS CERT;

-- changeset xipki:1
CREATE TABLE DBSCHEMA (
    NAME VARCHAR(45) NOT NULL,
    VALUE2 VARCHAR(100) NOT NULL,
    CONSTRAINT PK_DBSCHEMA PRIMARY KEY (NAME)
);

INSERT INTO DBSCHEMA (NAME, VALUE2) VALUES ('VENDOR', 'XIPKI');
INSERT INTO DBSCHEMA (NAME, VALUE2) VALUES ('VERSION', '8');
INSERT INTO DBSCHEMA (NAME, VALUE2) VALUES ('X500NAME_MAXLEN', '350');

CREATE TABLE PROFILE (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    CONSTRAINT PK_PROFILE PRIMARY KEY (ID)
);

COMMENT ON COLUMN PROFILE.NAME IS 'duplication is not permitted';

CREATE TABLE REQUESTOR (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    CONSTRAINT PK_REQUESTOR PRIMARY KEY (ID)
);

COMMENT ON COLUMN REQUESTOR.NAME IS 'duplication is not permitted';

CREATE TABLE CA (
    ID SMALLINT NOT NULL,
    NAME VARCHAR(45) NOT NULL,
    SUBJECT VARCHAR(350) NOT NULL,
    REV_INFO VARCHAR(200),
    CERT VARCHAR(6000) NOT NULL,
    CONSTRAINT PK_CA PRIMARY KEY (ID));

COMMENT ON COLUMN CA.NAME IS 'duplication is not permitted';
COMMENT ON COLUMN CA.REV_INFO IS 'CA revocation information';

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
    RID SMALLINT NOT NULL,
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

-- changeset xipki:4
ALTER TABLE CRL ADD CONSTRAINT FK_CRL_CA1 FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE CERT ADD CONSTRAINT FK_CERT_CA1 FOREIGN KEY (CA_ID) REFERENCES CA (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE CERT ADD CONSTRAINT FK_CERT_REQUESTOR1 FOREIGN KEY (RID) REFERENCES REQUESTOR (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE CERT ADD CONSTRAINT FK_CERT_PROFILE1 FOREIGN KEY (PID) REFERENCES PROFILE (ID)
    ON UPDATE NO ACTION ON DELETE NO ACTION;
