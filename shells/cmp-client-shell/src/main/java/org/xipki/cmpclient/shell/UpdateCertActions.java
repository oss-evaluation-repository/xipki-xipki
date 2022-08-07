/*
 *
 * Copyright (c) 2013 - 2020 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.cmpclient.shell;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.crmf.*;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.crmf.ProofOfPossessionSigningKeyBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.xipki.cmpclient.EnrollCertRequest;
import org.xipki.cmpclient.EnrollCertResult;
import org.xipki.cmpclient.EnrollCertResult.CertifiedKeyPairOrError;
import org.xipki.security.*;
import org.xipki.security.util.X509Util;
import org.xipki.shell.CmdFailure;
import org.xipki.shell.Completers;
import org.xipki.util.*;
import org.xipki.util.exception.ObjectCreationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.LinkedList;
import java.util.List;

/**
 * CMP client actions to update certificates.
 *
 * @author Lijun Liao
 *
 */
public class UpdateCertActions {

  @Command(scope = "xi", name = "cmp-update-cagenkey",
      description = "update certificate (keypair will be generated by the CA)")
  @Service
  public static class CmpUpdateCagenkey extends UpdateAction {

    @Option(name = "--cert-outform", description = "output format of the certificate")
    @Completion(Completers.DerPemCompleter.class)
    private String certOutform = "der";

    @Option(name = "--cert-out", description = "where to save the certificate")
    @Completion(FileCompleter.class)
    private String certOutputFile;

    @Option(name = "--p12-out", required = true, description = "where to save the PKCS#12 keystore")
    @Completion(FileCompleter.class)
    private String p12OutputFile;

    @Option(name = "--password", description = "password of the PKCS#12 file")
    private String password;

    @Override
    protected SubjectPublicKeyInfo getPublicKey()
        throws Exception {
      return null;
    }

    @Override
    protected EnrollCertRequest.Entry buildEnrollCertRequestEntry(String id, String profile,
        CertRequest certRequest)
            throws Exception {
      final boolean caGenKeypair = true;
      final boolean kup = true;
      return new EnrollCertRequest.Entry("id-1", profile, certRequest, null, caGenKeypair, kup);
    }

    @Override
    protected Object execute0()
        throws Exception {
      EnrollCertResult result = enroll();

      X509Cert cert = null;
      PrivateKeyInfo privateKeyInfo = null;
      if (result != null) {
        String id = result.getAllIds().iterator().next();
        CertifiedKeyPairOrError certOrError = result.getCertOrError(id);
        cert = certOrError.getCertificate();
        privateKeyInfo = certOrError.getPrivateKeyInfo();
      }

      if (cert == null) {
        throw new CmdFailure("no certificate received from the server");
      }

      if (privateKeyInfo == null) {
        throw new CmdFailure("no private key received from the server");
      }

      if (StringUtil.isNotBlank(certOutputFile)) {
        saveVerbose("saved certificate to file", certOutputFile,
            encodeCert(cert.getEncoded(), certOutform));
      }

      PrivateKey privateKey = BouncyCastleProvider.getPrivateKey(privateKeyInfo);

      KeyStore ks = KeyStore.getInstance("PKCS12");
      char[] pwd = getPassword();
      ks.load(null, pwd);
      ks.setKeyEntry("main", privateKey, pwd, new Certificate[] {cert.toJceCert()});
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ks.store(bout, pwd);
      saveVerbose("saved key to file", p12OutputFile, bout.toByteArray());

      return null;
    } // method execute0

    private char[] getPassword()
        throws IOException {
      char[] pwdInChar = readPasswordIfNotSet(password);
      if (pwdInChar != null) {
        password = new String(pwdInChar);
      }
      return pwdInChar;
    }
  } // class CmpUpdateCagenkey

  @Command(scope = "xi", name = "cmp-update-p11",
      description = "update certificate (PKCS#11 token)")
  @Service
  public static class CmpUpdateP11 extends UpdateCertAction {

    @Option(name = "--slot", required = true, description = "slot index")
    private Integer slotIndex;

    @Option(name = "--key-id",
        description = "id of the private key in the PKCS#11 device\n"
            + "either keyId or keyLabel must be specified")
    private String keyId;

    @Option(name = "--key-label",
        description = "label of the private key in the PKCS#11 device\n"
            + "either keyId or keyLabel must be specified")
    private String keyLabel;

    @Option(name = "--module", description = "name of the PKCS#11 module")
    private String moduleName = "default";

    private ConcurrentContentSigner signer;

    @Override
    protected ConcurrentContentSigner getSigner()
        throws ObjectCreationException {
      if (signer == null) {
        byte[] keyIdBytes = null;
        if (keyId != null) {
          keyIdBytes = Hex.decode(keyId);
        }

        SignerConf signerConf = getPkcs11SignerConf(moduleName, slotIndex, keyLabel,
            keyIdBytes, getHashAlgo(hashAlgo), getSignatureAlgoControl());
        signer = securityFactory.createSigner("PKCS11", signerConf, (X509Cert[]) null);
      }
      return signer;
    } // method getSigner

    public static SignerConf getPkcs11SignerConf(String pkcs11ModuleName, Integer slotIndex,
        String keyLabel, byte[] keyId, HashAlgo hashAlgo,
        SignatureAlgoControl signatureAlgoControl) {
      Args.notNull(hashAlgo, "hashAlgo");
      Args.notNull(slotIndex, "slotIndex");

      if (keyId == null && keyLabel == null) {
        throw new IllegalArgumentException("at least one of keyId and keyLabel may not be null");
      }

      ConfPairs conf = new ConfPairs();
      conf.putPair("parallelism", Integer.toString(1));

      if (pkcs11ModuleName != null && pkcs11ModuleName.length() > 0) {
        conf.putPair("module", pkcs11ModuleName);
      }

      if (slotIndex != null) {
        conf.putPair("slot", slotIndex.toString());
      }

      if (keyId != null) {
        conf.putPair("key-id", Hex.encode(keyId));
      }

      if (keyLabel != null) {
        conf.putPair("key-label", keyLabel);
      }

      return new SignerConf(conf.getEncoded(), hashAlgo, signatureAlgoControl);
    } // method getPkcs11SignerConf

  } // class CmpUpdateP11

  @Command(scope = "xi", name = "cmp-update-p12",
      description = "update certificate (PKCS#12 keystore)")
  @Service
  public static class CmpUpdateP12 extends UpdateCertAction {

    @Option(name = "--p12", required = true, description = "PKCS#12 keystore file")
    @Completion(FileCompleter.class)
    private String p12File;

    @Option(name = "--password", description = "password of the PKCS#12 keystore file")
    private String password;

    private ConcurrentContentSigner signer;

    @Override
    protected ConcurrentContentSigner getSigner()
        throws ObjectCreationException {
      if (signer == null) {
        if (password == null) {
          try {
            password = new String(readPassword());
          } catch (IOException ex) {
            throw new ObjectCreationException("could not read password: " + ex.getMessage(), ex);
          }
        }

        ConfPairs conf = new ConfPairs("password", password);
        conf.putPair("parallelism", Integer.toString(1));
        conf.putPair("keystore", "file:" + p12File);
        SignerConf signerConf = new SignerConf(conf.getEncoded(),
            getHashAlgo(hashAlgo), getSignatureAlgoControl());
        signer = securityFactory.createSigner("PKCS12", signerConf, (X509Cert[]) null);
      }
      return signer;
    }

  }

  public abstract static class UpdateAction extends Actions.AuthClientAction {

    @Reference
    protected SecurityFactory securityFactory;

    @Option(name = "--subject", aliases = "-s",
        description = "subject to be requested")
    private String subject;

    @Option(name = "--not-before", description = "notBefore, UTC time of format yyyyMMddHHmmss")
    private String notBeforeS;

    @Option(name = "--not-after", description = "notAfter, UTC time of format yyyyMMddHHmmss")
    private String notAfterS;

    @Option(name = "--oldcert", required = true, description = "certificate file")
    @Completion(FileCompleter.class)
    private String oldCertFile;

    protected abstract SubjectPublicKeyInfo getPublicKey()
        throws Exception;

    protected abstract EnrollCertRequest.Entry buildEnrollCertRequestEntry(
        String id, String profile, CertRequest certRequest)
            throws Exception;

    protected EnrollCertResult enroll()
        throws Exception {
      CertTemplateBuilder certTemplateBuilder = new CertTemplateBuilder();

      if (subject != null && !subject.isEmpty()) {
        certTemplateBuilder.setSubject(new X500Name(subject));
      }

      SubjectPublicKeyInfo publicKey = getPublicKey();
      if (publicKey != null) {
        certTemplateBuilder.setPublicKey(getPublicKey());
      }

      if (StringUtil.isNotBlank(notBeforeS) || StringUtil.isNotBlank(notAfterS)) {
        Time notBefore = StringUtil.isNotBlank(notBeforeS)
            ? new Time(DateUtil.parseUtcTimeyyyyMMddhhmmss(notBeforeS)) : null;
        Time notAfter = StringUtil.isNotBlank(notAfterS)
            ? new Time(DateUtil.parseUtcTimeyyyyMMddhhmmss(notAfterS)) : null;
        OptionalValidity validity = new OptionalValidity(notBefore, notAfter);
        certTemplateBuilder.setValidity(validity);
      }

      List<Extension> extensions = new LinkedList<>();

      if (isNotEmpty(extensions)) {
        Extensions asn1Extensions = new Extensions(extensions.toArray(new Extension[0]));
        certTemplateBuilder.setExtensions(asn1Extensions);
      }

      X509Cert oldCert = X509Util.parseCert(new File(oldCertFile));
      CertId oldCertId = new CertId(new GeneralName(oldCert.getIssuer()),
                            oldCert.getSerialNumber());

      Controls controls = new Controls(
          new AttributeTypeAndValue(CMPObjectIdentifiers.regCtrl_oldCertID, oldCertId));

      CertRequest certReq = new CertRequest(1, certTemplateBuilder.build(), controls);

      EnrollCertRequest.Entry reqEntry = buildEnrollCertRequestEntry("id-1", null, certReq);
      EnrollCertRequest request = new EnrollCertRequest(EnrollCertRequest.EnrollType.KEY_UPDATE);
      request.addRequestEntry(reqEntry);

      ReqRespDebug debug = getReqRespDebug();
      EnrollCertResult result;
      try {
        result = client.enrollCerts(caName, getRequestor(), request, debug);
      } finally {
        saveRequestResponse(debug);
      }

      return result;
    } // method enroll

  } // class UpdateAction

  public abstract static class UpdateCertAction extends UpdateAction {

    @Option(name = "--hash", description = "hash algorithm name for the POP computation")
    protected String hashAlgo = "SHA256";

    @Option(name = "--outform", description = "output format of the certificate")
    @Completion(Completers.DerPemCompleter.class)
    private String outform = "der";

    @Option(name = "--out", aliases = "-o", required = true,
        description = "where to save the certificate")
    @Completion(FileCompleter.class)
    private String outputFile;

    @Option(name = "--rsa-pss",
        description = "whether to use the RSAPSS for the POP computation\n"
            + "(only applied to RSA key)")
    private Boolean rsaPss = Boolean.FALSE;

    @Option(name = "--dsa-plain",
        description = "whether to use the Plain DSA for the POP computation\n"
            + "(only applied to DSA and ECDSA key)")
    private Boolean dsaPlain = Boolean.FALSE;

    @Option(name = "--gm",
        description = "whether to use the chinese GM algorithm for the POP computation\n"
            + "(only applied to EC key with GM curves)")
    private Boolean gm = Boolean.FALSE;

    @Option(name = "--embeds-publickey",
        description = "whether to embed the public key in the request")
    private Boolean embedsPulibcKey = Boolean.FALSE;

    protected SignatureAlgoControl getSignatureAlgoControl() {
      return new SignatureAlgoControl(rsaPss, dsaPlain, gm);
    }

    /**
     * Gets the signer.
     * @return the signer.
     * @throws ObjectCreationException
     *           if no signer can be built.
     */
    protected abstract ConcurrentContentSigner getSigner()
        throws ObjectCreationException;

    protected SubjectPublicKeyInfo getPublicKey()
        throws Exception {
      return embedsPulibcKey ? getSigner().getCertificate().getSubjectPublicKeyInfo() : null;
    } // method getPublicKey

    @Override
    protected EnrollCertRequest.Entry buildEnrollCertRequestEntry(String id, String profile,
        CertRequest certRequest)
            throws Exception {
      ConcurrentContentSigner signer = getSigner();

      ProofOfPossessionSigningKeyBuilder popBuilder =
          new ProofOfPossessionSigningKeyBuilder(certRequest);
      ConcurrentBagEntrySigner signer0 = signer.borrowSigner();
      POPOSigningKey popSk;
      try {
        popSk = popBuilder.build(signer0.value());
      } finally {
        signer.requiteSigner(signer0);
      }

      ProofOfPossession pop = new ProofOfPossession(popSk);
      final boolean caGenKeypair = false;
      final boolean kup = true;

      return new EnrollCertRequest.Entry(id, profile, certRequest, pop, caGenKeypair, kup);
    } // method buildEnrollCertRequestEntry

    @Override
    protected Object execute0()
        throws Exception {
      EnrollCertResult result = enroll();

      X509Cert cert = null;
      if (result != null) {
        String id = result.getAllIds().iterator().next();
        cert = result.getCertOrError(id).getCertificate();
      }

      if (cert == null) {
        throw new CmdFailure("no certificate received from the server");
      }

      saveVerbose("saved certificate to file", outputFile, encodeCert(cert.getEncoded(), outform));

      return null;
    } // method execute0

  } // class UpdateCertAction

}
