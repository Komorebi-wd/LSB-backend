package org.example.lsbbackend.controller;

import org.example.lsbbackend.BMPDecoder;
import org.example.lsbbackend.BMPEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/lsb")
public class LSBController {

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image file.");
            }
            int maxEmbeddableLength = calculateMaxEmbeddableLength(image);
            return ResponseEntity.ok(maxEmbeddableLength);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image.");
        }
    }

    @PostMapping("/embed")
    public ResponseEntity<?> embedMessage(@RequestParam("image") MultipartFile imageFile,
                                          @RequestParam("message") String message) {
        try {

//            BufferedImage image = ImageIO.read(imageFile.getInputStream());
//            BufferedImage modifiedImage = embedMessageIntoImage(image, message);
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            ImageIO.write(modifiedImage, "bmp", outputStream);
//            byte[] imageBytes = outputStream.toByteArray();
            byte[] imageBytes = BMPEncoder.hideMessageInBmp(imageFile.getInputStream(),message);
            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);

            return ResponseEntity.ok(encodedImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error embedding message.");
        }
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extractMessage(@RequestParam("image") MultipartFile imageFile) {
        try {
            String message = BMPDecoder.extractMessageFromBmp(imageFile.getInputStream());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error extracting message.");
        }
    }

    private int calculateMaxEmbeddableLength(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int maxLength = (width * height * 3-18) / 16-1; // 3 bytes per pixel, 1 bit per byte for LSB
        return maxLength;
    }

    private BufferedImage embedMessageIntoImage(BufferedImage image, String message) {
        byte[] messageBytes = message.getBytes();
        int messageLength = messageBytes.length;

        int[] messageBits = new int[messageLength * 8];
        for (int i = 0; i < messageLength; i++) {
            for (int bit = 0; bit < 8; bit++) {
                messageBits[i * 8 + bit] = (messageBytes[i] >> (7 - bit)) & 1;
            }
        }

        BufferedImage modifiedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        int bitIndex = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);

                if (bitIndex < messageBits.length) {
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;

                    blue = (blue & 0xFE) | messageBits[bitIndex];
                    bitIndex++;

                    int newPixel = (red << 16) | (green << 8) | blue;
                    modifiedImage.setRGB(x, y, newPixel);
                } else {
                    modifiedImage.setRGB(x, y, pixel);
                }
            }
        }

        return modifiedImage;
    }

    private String extractMessageFromImage(BufferedImage image) {
        ByteBuffer buffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 3 / 8);

        int bitIndex = 0;
        byte currentByte = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int blue = pixel & 0xFF;

                currentByte = (byte) ((currentByte << 1) | (blue & 1));
                bitIndex++;

                if (bitIndex == 8) {
                    buffer.put(currentByte);
                    bitIndex = 0;
                    currentByte = 0;
                }
            }
        }

        buffer.flip();
        byte[] messageBytes = new byte[buffer.limit()];
        buffer.get(messageBytes);
        return new String(messageBytes).trim();
    }
}
