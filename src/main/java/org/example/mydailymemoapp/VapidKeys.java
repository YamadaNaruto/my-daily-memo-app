package org.example.mydailymemoapp;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Properties;

public record VapidKeys (String publicKey, String privateKey){

    public static VapidKeys loadOrGenerate(Path file) throws Exception{
        if (Files.exists(file)){
            var p = new Properties();
            try (var in = Files.newInputStream(file)) {p.load(in); }
            return new VapidKeys(p.getProperty("public"),p.getProperty("private"));
        }
        var keys = generateP256();
        var p = new Properties();
        p.setProperty("public", keys.publicKey());
        p.setProperty("private", keys.privateKey());
        try (var out = Files.newOutputStream(file)) {
            p.store(out, "VAPID keys");
        }
        return keys;
    }

    private static VapidKeys generateP256() throws Exception {
        var gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        var pair = gen.generateKeyPair();

        var pub = (ECPublicKey) pair.getPublic();
        var priv = (ECPrivateKey) pair.getPrivate();

            // public = 0x04 || X || Y (65byte)
        byte[] x = toFixed(pub.getW().getAffineX(), 32);
        byte[] y = toFixed(pub.getW().getAffineY(), 32);
        byte[] pubBytes = new byte[65];
        pubBytes[0] = 0x04;
        System.arraycopy(x, 0, pubBytes, 1,  32);
        System.arraycopy(y, 0, pubBytes, 33, 32);

            // private = 32byte スカラ
        byte[] privBytes = toFixed(priv.getS(), 32);

        var enc = Base64.getUrlEncoder().withoutPadding();
        return new VapidKeys(enc.encodeToString(pubBytes), enc.encodeToString(privBytes));
    }
        // BigIntegerを固定長バイト配列に整える補助（先頭の符号バイト除去・ゼロ埋め）
    private static byte[] toFixed(BigInteger v, int len) {
        byte[] b = v.toByteArray();
        if (b.length == len) return b;
        byte[] out = new byte[len];
        if (b.length > len) System.arraycopy(b, b.length - len, out, 0, len);   // 余分な先頭を落とす
        else               System.arraycopy(b, 0, out, len - b.length, b.length); // 足りない分を左ゼロ埋め
        return out;
    }
}


