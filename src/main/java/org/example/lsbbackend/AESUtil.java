package org.example.lsbbackend;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AESUtil {
    /** 加密模式之 ECB，算法/模式/补码方式 */
    public static final String AES_ECB = "AES/ECB/PKCS5Padding";

    /** 加密模式之 CBC，算法/模式/补码方式 */
    public static final String AES_CBC = "AES/CBC/PKCS5Padding";

    /** 加密模式之 CFB，算法/模式/补码方式 */
    public static final String AES_CFB = "AES/CFB/PKCS5Padding";

    /** AES 中的 IV 必须是 16 字节（128位）长 */
    public static final Integer IV_LENGTH = 16;

    public static boolean isEmpty(Object str) {
        return null == str || "".equals(str);
    }
    public static byte[] getBytes(String str){
        if (isEmpty(str)) {
            return null;
        }
        try {
            return str.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int[] getRandom(String seedString,int max,int nums) throws Exception {
        int min = 18;
        int[] locations=new int[nums];
        try {
            for(int i=0;i<nums;i++){
                // 计算字符串的哈希值
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] seed = md.digest(seedString.getBytes());
                // 使用哈希值作为种子创建安全的随机数生成器
                SecureRandom secureRandom = new SecureRandom(seed);
                // 生成指定范围的随机整数
                int randomNumber = secureRandom.nextInt(max - min + 1) + min;
                locations[i]=randomNumber;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return locations;
    }

    /***
     * <h2>获取一个 AES 密钥规范</h2>
     */
    public static SecretKeySpec getSecretKeySpec(String key){
        SecretKeySpec secretKeySpec = new SecretKeySpec(getBytes(key), "AES");
        return secretKeySpec;
    }
    public static byte[] encrypt(String text, String key, String iv, String mode){
        if (isEmpty(text) || isEmpty(key) || isEmpty(iv)) {
            return null;
        }

        try {
            // 创建AES加密器
            Cipher cipher = Cipher.getInstance(mode);

            SecretKeySpec secretKeySpec = getSecretKeySpec(key);

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(getBytes(iv)));

            // 加密字节数组
            byte[] encryptedBytes = cipher.doFinal(getBytes(text));

            // 将密文转换为 Base64 编码字符串
            return encryptedBytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String decrypt(byte[] textBytes, String key, String iv, String mode){
        try {
            // 创建AES加密器
            Cipher cipher = Cipher.getInstance(mode);

            SecretKeySpec secretKeySpec = getSecretKeySpec(key);

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(getBytes(iv)));

            // 解密字节数组
            byte[] decryptedBytes = cipher.doFinal(textBytes);

            // 将明文转换为字符串
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

    }

}
