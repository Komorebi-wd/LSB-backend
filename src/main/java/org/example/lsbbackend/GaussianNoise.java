package org.example.lsbbackend;

import java.awt.image.BufferedImage;
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
        value = (double) color + Math.sqrt((double) color) * SigmaGaussian * sigma + TauGaussian * tau;

        if (value < 0.0)
            return 0;
        if (value > 255)
            return (int) 255;

        // 将计算得到的噪声值四舍五入并转换为整数后返回
        return (int) (value + 0.5);
    }

    public BufferedImage addNoiseImage(BufferedImage image) {
        BufferedImage bimg = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB);
        Pixel pixel = new Pixel();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixel.setRGB(image.getRGB(x, y));
                pixel.red = generateNoise(pixel.red);
                pixel.green = generateNoise(pixel.green);
                pixel.blue = generateNoise(pixel.blue);
                bimg.setRGB(x, y, pixel.getRGB());
            }
        }

        return bimg;
    }
}


