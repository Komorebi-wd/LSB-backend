package org.example.lsbbackend;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BMPEncoder {
    public static void main(String[] args) {
        try {
            int[] locations = AESUtil.getRandom("mcx",100,10);
            int[] locations1 = AESUtil.getRandom("mcx",100,10);
            for (int i=0;i<locations.length;i++){
                System.out.println(locations[i]+" "+locations1[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static byte[] hideMessageInBmp(InputStream inputStream, String message, String key) throws Exception{
        try {

            // 读取BMP文件
            byte[] header = new byte[54];
            inputStream.read(header);
            // 读取图像宽度和高度
            int width = ByteBuffer.wrap(header, 18, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int height = ByteBuffer.wrap(header, 22, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            // 读取像素数据
            byte[] pixelData = new byte[inputStream.available()];
            inputStream.read(pixelData);
            inputStream.close();
            // 将消息转换为二进制字符串
            StringBuilder binaryMessage = new StringBuilder();
            System.out.println(message.getBytes(StandardCharsets.UTF_8).length);
            byte[] encryptedData= AESUtil.encrypt(message,key, key, AESUtil.AES_CBC);
            System.out.println(encryptedData.length);
            for (byte i:encryptedData){
                System.out.print(i+" ");
            }
            System.out.println();
            for (byte i : encryptedData) {
                String binaryChar = String.format("%16s", Integer.toBinaryString(i & 0xFF)).replace(' ', '0');
                System.out.println(binaryChar);
                binaryMessage.append(binaryChar);
            }
            System.out.println("MessageBinary:"+binaryMessage.length());
            int size=binaryMessage.length();
            // 补齐到3的倍数
            while (binaryMessage.length() % 3 != 0) {
                binaryMessage.append('0'); // 在末尾添加0，直到长度为3的倍数
            }

            System.out.println("MessageBinary:"+size);
            String binary = Integer.toBinaryString(size & 0xFFFF);
            // 如果字符串长度不足16位，前面补0直到长度为16位
            binary = String.format("%16s", binary).replace(' ', '0');
            binaryMessage.insert(0,binary);
            System.out.println("message"+binaryMessage.toString());
            int[] locations = AESUtil.getRandom(key,width*height,binaryMessage.length()-16);

            // 检查消息是否可以嵌入到图像中
            for (int i=0;i<locations.length;i++){
                System.out.println(locations[i]+" ");
            }
            int maxCapacity = pixelData.length * 8 / 3; // 每个像素可隐藏一个字符，每个像素占用3字节，一个字节8位
            if (binaryMessage.length() > maxCapacity) {
                throw new IllegalArgumentException("Message too long to embed in the image.");
            }
            for(int i=0;i<18;i+=3){
                int b = pixelData[i] & 0xFF;
                int g = pixelData[i + 1] & 0xFF;
                int r = pixelData[i + 2] & 0xFF;
                // 修改最低有效位为消息的一个bit
                b = (b & 0xFE) | Character.getNumericValue(binaryMessage.charAt(i));
                if(i==15){
                    g = 0;
                    r = 0;
                }
                else {
                    g = (g & 0xFE) | Character.getNumericValue(binaryMessage.charAt(i + 1));
                    r = (r & 0xFE) | Character.getNumericValue(binaryMessage.charAt(i + 2));
                }

                // 更新像素数据
                pixelData[i] = (byte) b;
                pixelData[i + 1] = (byte) g;
                pixelData[i + 2] = (byte) r;

            }
            System.out.println(binaryMessage.length());
            // 嵌入消息
            int index = 16;
            for (int byteIndex = 0; byteIndex < locations.length; byteIndex += 3) {
                if (index < binaryMessage.length()) {
                    int x1 = locations[byteIndex];
                    int y1 = locations[byteIndex+1];
                    int z1 = locations[byteIndex+2];
                    // 读取原始像素颜色值
                    int b = pixelData[x1] & 0xFF;
                    int g = pixelData[y1] & 0xFF;
                    int r = pixelData[z1] & 0xFF;
                    // 修改最低有效位为消息的一个bit
                    b = (b & 0xFE) | Character.getNumericValue(binaryMessage.charAt(index));
                    g = (g & 0xFE) | Character.getNumericValue(binaryMessage.charAt(index + 1));
                    r = (r & 0xFE) | Character.getNumericValue(binaryMessage.charAt(index + 2));
                    // 更新像素数据
                    pixelData[x1] = (byte) b;
                    pixelData[y1] = (byte) g;
                    pixelData[z1] = (byte) r;
                    index += 3;
                }
                else{
                    break;
                }
            }

            byte[] combinedArray = new byte[header.length + pixelData.length];
            System.arraycopy(header, 0, combinedArray, 0, header.length);
            System.arraycopy(pixelData, 0, combinedArray, header.length, pixelData.length);
            // 写入修改后的像素数据到新的BMP文件
//            FileOutputStream outputStream = new FileOutputStream(outputFilename);
//            outputStream.write(header);
//            outputStream.write(pixelData);
//            outputStream.close();
            return combinedArray;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
