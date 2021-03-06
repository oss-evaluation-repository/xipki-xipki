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

package org.xipki.security;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.xipki.security.ObjectIdentifiers.Shake;
import org.xipki.security.bc.XiShakeDigest;
import org.xipki.util.Args;

/**
 * Hash algorithm enum.
 *
 * @author Lijun Liao
 * @since 2.0.0
 */

public enum HashAlgo {

  SHA1(20,     "1.3.14.3.2.26", "SHA1"),
  SHA224(28,   "2.16.840.1.101.3.4.2.4",  "SHA224"),
  SHA256(32,   "2.16.840.1.101.3.4.2.1",  "SHA256"),
  SHA384(48,   "2.16.840.1.101.3.4.2.2",  "SHA384"),
  SHA512(64,   "2.16.840.1.101.3.4.2.3",  "SHA512"),
  SHA3_224(28, "2.16.840.1.101.3.4.2.7",  "SHA3-224"),
  SHA3_256(32, "2.16.840.1.101.3.4.2.8",  "SHA3-256"),
  SHA3_384(48, "2.16.840.1.101.3.4.2.9",  "SHA3-384"),
  SHA3_512(64, "2.16.840.1.101.3.4.2.10", "SHA3-512"),
  SM3(32,      "1.2.156.10197.1.401",     "SM3"),
  SHAKE128(32, Shake.id_shake128.getId(), "SHAKE128-256"),
  SHAKE256(64, Shake.id_shake256.getId(), "SHAKE256-512");

  private static final Map<String, HashAlgo> map = new HashMap<>();

  private final int length;

  private final ASN1ObjectIdentifier oid;

  private final AlgorithmIdentifier algId;

  private final String jceName;

  private final byte[] encoded;

  static {
    for (HashAlgo type : HashAlgo.values()) {
      map.put(type.oid.getId(), type);
      map.put(type.jceName, type);
    }

    map.put("SHA-1",   SHA1);
    map.put("SHA-224", SHA224);
    map.put("SHA-256", SHA256);
    map.put("SHA-384", SHA384);
    map.put("SHA-512", SHA512);
    map.put("SHA3224", SHA3_224);
    map.put("SHA3256", SHA3_256);
    map.put("SHA3384", SHA3_384);
    map.put("SHA3512", SHA3_512);
    map.put("SHAKE128", SHAKE128);
    map.put("SHAKE256", SHAKE256);
  }

  private HashAlgo(int length, String oid, String jceName) {
    this.length = length;
    this.oid = new ASN1ObjectIdentifier(oid).intern();
    if (this.oid.equals(Shake.id_shake128) || this.oid.equals(Shake.id_shake256)) {
      this.algId = new AlgorithmIdentifier(this.oid);
    } else {
      this.algId = new AlgorithmIdentifier(this.oid, DERNull.INSTANCE);
    }
    this.jceName = jceName;

    try {
      this.encoded = new ASN1ObjectIdentifier(oid).getEncoded();
    } catch (IOException ex) {
      throw new IllegalArgumentException("invalid oid: " + oid);
    }
  }

  public int getLength() {
    return length;
  }

  public ASN1ObjectIdentifier getOid() {
    return oid;
  }

  public String getJceName() {
    return jceName;
  }

  public boolean isShake() {
    switch (this) {
      case SHAKE128:
      case SHAKE256:
        return true;
      default:
        return false;
    }
  }

  public static HashAlgo getInstance(AlgorithmIdentifier id)
      throws NoSuchAlgorithmException {
    Args.notNull(id, "id");
    ASN1Encodable params = id.getParameters();
    if (params != null && !DERNull.INSTANCE.equals(params)) {
      throw new NoSuchAlgorithmException("params is present but is not NULL");
    }

    return getInstance(id.getAlgorithm());
  }

  public static HashAlgo getInstance(ASN1ObjectIdentifier oid)
      throws NoSuchAlgorithmException {
    Args.notNull(oid, "oid");
    for (HashAlgo hashAlgo : values()) {
      if (hashAlgo.oid.equals(oid)) {
        return hashAlgo;
      }
    }
    throw new NoSuchAlgorithmException("Unknown HashAlgo OID '" + oid.getId() + "'");
  }

  public static HashAlgo getInstance(String nameOrOid)
      throws NoSuchAlgorithmException {
    HashAlgo alg = map.get(nameOrOid.toUpperCase());
    if (alg == null) {
      throw new NoSuchAlgorithmException("Found no HashAlgo for name/OID '" + nameOrOid + "'");
    }
    return alg;
  }

  public static HashAlgo getInstanceForEncoded(byte[] encoded)
      throws NoSuchAlgorithmException {
    return getInstanceForEncoded(encoded, 0, encoded.length);
  }

  public static HashAlgo getInstanceForEncoded(byte[] encoded, int offset, int len)
      throws NoSuchAlgorithmException {
    for (HashAlgo value : values()) {
      byte[] ve = value.encoded;
      if (ve.length != len) {
        continue;
      }

      boolean equals = true;
      for (int i = 0; i < len; i++) {
        if (ve[i] != encoded[offset + i]) {
          equals = false;
          break;
        }
      }

      if (equals) {
        return value;
      }
    }
    throw new NoSuchAlgorithmException("Found no HashAlgo for encoded");
  }

  public AlgorithmIdentifier getAlgorithmIdentifier() {
    return algId;
  }

  public ExtendedDigest createDigest() {
    switch (this) {
      case SHA1:
        return new SHA1Digest();
      case SHA224:
        return new SHA224Digest();
      case SHA256:
        return new SHA256Digest();
      case SHA384:
        return new SHA384Digest();
      case SHA512:
        return new SHA512Digest();
      case SHA3_224:
        return new SHA3Digest(224);
      case SHA3_256:
        return new SHA3Digest(256);
      case SHA3_384:
        return new SHA3Digest(384);
      case SHA3_512:
        return new SHA3Digest(512);
      case SM3:
        return new SM3Digest();
      case SHAKE128:
        return new XiShakeDigest.XiShake128Digest();
      case SHAKE256:
        return new XiShakeDigest.XiShake256Digest();
      default:
        throw new IllegalStateException("should not reach here, unknown HashAlgo " + name());
    }
  }

  public String hexHash(byte[]... datas) {
    return HashCalculator.hexHash(this, datas);
  }

  public String hexHash(byte[] data, int offset, int len) {
    return HashCalculator.hexHash(this, data, offset, len);
  }

  public String base64Hash(byte[]... datas) {
    return HashCalculator.base64Hash(this, datas);
  }

  public String base64Hash(byte[] data, int offset, int len) {
    return HashCalculator.base64Hash(this, data, offset, len);
  }

  public byte[] hash(byte[]... datas) {
    return HashCalculator.hash(this, datas);
  }

  public byte[] hash(byte[] data, int offset, int len) {
    return HashCalculator.hash(this, data, offset, len);
  }

  public int getEncodedLength() {
    return encoded.length;
  }

  public int write(byte[] out, int offset) {
    System.arraycopy(encoded, 0, out, offset, encoded.length);
    return encoded.length;
  }
}
