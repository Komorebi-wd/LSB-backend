package org.example.lsbbackend;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class GaussianNoise {
    // 创建了一个Random对象rand，用于生成随机数，种子为当前时间的毫秒数。种子的作用是确保每次运行时生成的随机数序列不同。
    private Random rand = new Random(System.currentTimeMillis());

    private int generateNoise(int color) {
        double alpha, beta, sigma, value;
        // 生成一个随机的双精度浮点数alpha，取值范围在0到1之间。
        alpha = rand.nextDouble();
        if (alpha == 0.0)
            alpha = 1.0;

        // 定义了高斯分布的标准差SigmaGaussian，值为1.0。
        // 定义了高斯分布的另一个参数TauGaussian，值为5.0。这些参数用于控制噪声的分布。
        double SigmaGaussian = 1.0;
        double TauGaussian = 5.0;

        double tau;
        beta = rand.nextDouble();
        // 计算高斯噪声的sigma值和tau值，根据Box-Muller变换公式计算。
        sigma = Math.sqrt(-2.0 * Math.log(alpha)) * Math.cos(2.0 * Math.PI * beta);
        tau = Math.sqrt(-2.0 * Math.log(alpha)) * Math.sin(2.0 * Math.PI * beta);

        // 计算最终的噪声值。这里将输入的颜色值转换为双精度浮点数，并根据计算得到的sigma和tau值计算出噪声，加到颜色值上。
        value = (double) color + Math.sqrt(color) * SigmaGaussian * sigma + TauGaussian * tau;

        if (value < 0.0)
            return 0;
        if (value > 255)
            return 255;

        // 将计算得到的噪声值四舍五入并转换为整数后返回
        return (int) (value + 0.5);
    }

    public byte[] addNoiseImage(InputStream inputStream) throws IOException {
        try {
            // 读取BMP文件头部信息
            byte[] header = new byte[54];
            inputStream.read(header);

            // 读取图像宽度和高度
            int width = ByteBuffer.wrap(header, 18, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int height = ByteBuffer.wrap(header, 22, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

            // 读取像素数据
            byte[] pixelData = new byte[inputStream.available()];
            inputStream.read(pixelData);
            inputStream.close();

            // 添加噪声
            for (int i = 0; i < pixelData.length-3; i += 3) {
                // 读取RGB值
                int red = pixelData[i] & 0xFF;
                int green = pixelData[i + 1] & 0xFF;
                int blue = pixelData[i + 2] & 0xFF;

                // 添加噪声
                red = generateNoise(red);
                green = generateNoise(green);
                blue = generateNoise(blue);

                // 写回噪声后的RGB值
                pixelData[i] = (byte) red;
                pixelData[i + 1] = (byte) green;
                pixelData[i + 2] = (byte) blue;
            }
            System.out.println("ccc");

            // 构造最终的byte数组，包括头部和修改后的像素数据
            byte[] combinedArray = new byte[header.length + pixelData.length];
            System.arraycopy(header, 0, combinedArray, 0, header.length);
            System.arraycopy(pixelData, 0, combinedArray, header.length, pixelData.length);

            return combinedArray;

        } catch (IOException e) {

            e.printStackTrace();
            throw e; // 抛出异常以便调用者处理
        }
    }
}


