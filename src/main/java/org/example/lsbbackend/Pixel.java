package org.example.lsbbackend;

import java.awt.image.*;

public class Pixel {
    /*
     * red, green, blue: 分别存储像素的红色、绿色和蓝色分量（0-255范围）。
     * alpha: 存储像素的透明度分量，默认为不透明（255）。
     * hue, saturation, luminosity: 存储像素的色调、饱和度和亮度信息，这些在代码中没有具体计算实现，可能用于后续扩展或计算。
     * rgb: 存储整个像素的颜色信息（包括红、绿、蓝和透明度）
     */
    public int red;
    public int green;
    public int blue;
    public int alpha=0xFF;
    public double hue;
    public double saturation;
    public double luminosity;
    private int rgb;

    public Pixel() {
    }

    public void setRGB(int rgb) {
        red = (rgb & 0x00FF0000) / 0x00010000;
        green = (rgb & 0x0000FF00) / 0x00000100;
        blue = rgb & 0x000000FF;
        alpha = (rgb >> 24)&0x0ff;
        this.rgb = rgb;
    }

    public int getRGB() {
        rgb =  alpha<<24 | 0x00010000 * red | 0x00000100 * green | blue;
        return this.rgb;
    }

    public static void setRgb(BufferedImage image, int x, int y, int red, int green, int blue) {
        int rgb = 0xFF000000 | red * 0x00010000 | green * 0x00000100 | blue;
        image.setRGB(x, y, rgb);
    }

    public static int pixelIntensity(int rgb) {
        int red = (rgb&0x00FF0000)/0x00010000;
        int green = (rgb&0x0000FF00)/0x00000100;
        int blue = rgb&0x000000FF;
        return (int) (0.299 * red + 0.587 * green + 0.114 * blue + 0.5);
    }
}

