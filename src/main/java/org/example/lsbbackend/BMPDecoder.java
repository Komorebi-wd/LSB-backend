package org.example.lsbbackend;
import java.io.FileInputStream;
import java.io.InputStream;


public class BMPDecoder {
    public static void main(String[] args) {
//        String inputBmp = "output.bmp"; // 嵌入了消息的BMP文件
//        String extractedMessage = null;
//        try {
//            extractedMessage = extractMessageFromBmp(inputBmp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println("隐藏的信息是: " + extractedMessage);
    }
    public static String extractMessageFromBmp(InputStream fis, String key) throws Exception{
        byte[] header = new byte[54];
        byte[] pixelData;

            // 读取BMP文件头部
            fis.read(header);

            // 读取图像宽度和高度
            int width = byteArrayToInt(header, 18);
            int height = byteArrayToInt(header, 22);

            // 读取像素数据
            if (header[28] == 8) {
                // 256色灰度图，每个像素占用1字节
                pixelData = fis.readAllBytes();
            } else if (header[28] == 24) {
                // 24位真彩色图，每个像素占用3字节 (BGR顺序)
                pixelData = fis.readAllBytes();
            } else {
                throw new IllegalArgumentException("Unsupported BMP format. Must be 8-bit or 24-bit.");
            }
        int size = 0;
        StringBuilder binaryMessage = new StringBuilder();

        for (int byteIndex = 0; byteIndex < 18; byteIndex += 3){
            // 提取每个像素的RGB值
            int b = pixelData[byteIndex] & 1;
            int g = pixelData[byteIndex + 1] & 1;
            int r = pixelData[byteIndex + 2] & 1;

            // 提取最低有效位的信息
            binaryMessage.append(b);
            binaryMessage.append(g);
            binaryMessage.append(r);
            // 每收集8个bit则转换为字符
            if (binaryMessage.length() >= 16) {
                size = Integer.parseInt(binaryMessage.substring(0, 16), 2);
                binaryMessage.delete(0, 16);
            }
        }
        binaryMessage.delete(0,2);
        byte[] result=new byte[size/16];
        int index=0;
        int padding=0;
        while((size+padding)%3!=0){
            padding++;
        }

        int[] locations = AESUtil.getRandom(key,width*height,size+padding);
        for (int i=0;i<locations.length;i++){
            System.out.println(locations[i]+" ");
        }
        for (int byteIndex = 0; byteIndex < size+padding; byteIndex += 3) {
            int x1 = locations[byteIndex];
            int y1 = locations[byteIndex+1];
            int z1 = locations[byteIndex+2];
            // 提取每个像素的RGB值
            int b = pixelData[x1] & 1;
            int g = pixelData[y1] & 1;
            int r = pixelData[z1] & 1;
            // 提取最低有效位的信息
            if(byteIndex>=size&&padding==2){
                binaryMessage.append(g);
            }
            else if(byteIndex>=size&&padding==1){
                binaryMessage.append(b);
            }
            else{
                binaryMessage.append(b);
                binaryMessage.append(g);
                binaryMessage.append(r);
            }
            // 每收集8个bit则转换为字符
            if (binaryMessage.length() >= 16) {
                int byteValue = Integer.parseInt(binaryMessage.substring(0, 16), 2);

                result[index]=(byte)(byteValue);
                index++;
                binaryMessage.delete(0, 16);
                // 判断是否已经找到结束符号 '\0' 表示信息提取完成
                if (byteValue == 0) {
                    break;
                }
            }

        }
        for(int i=0;i<result.length;i++){
            System.out.print(result[i]+" ");
        }
        return AESUtil.decrypt(result,key,key, AESUtil.AES_CBC);
    }

    // 辅助方法：将字节数组中指定位置的4个字节转换为int
    private static int byteArrayToInt(byte[] bytes, int offset) {
        return ((bytes[offset + 3] & 0xFF) << 24) |
                ((bytes[offset + 2] & 0xFF) << 16) |
                ((bytes[offset + 1] & 0xFF) << 8) |
                (bytes[offset] & 0xFF);
    }
}
