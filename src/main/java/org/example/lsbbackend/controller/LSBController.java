package org.example.lsbbackend.controller;

import org.example.lsbbackend.BMPDecoder;
import org.example.lsbbackend.BMPEncoder;
import org.example.lsbbackend.GaussianNoise;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/lsb")
public class LSBController {

    @PostMapping("/uploadWithKey")
    public ResponseEntity<?> uploadImageWithKey(@RequestParam("image") MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image file.");
            }
            int maxEmbeddableLength = calculateMaxEmbeddableLength(file,true);
            return ResponseEntity.ok(maxEmbeddableLength);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image.");
        }
    }
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image file.");
            }
            int maxEmbeddableLength = calculateMaxEmbeddableLength(file,false);
            return ResponseEntity.ok(maxEmbeddableLength);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image.");
        }
    }
    @PostMapping("/embedWithKey")
    public ResponseEntity<?> embedMessageWithKey(@RequestParam("image") MultipartFile imageFile,
                                          @RequestParam("message") String message,
                                          @RequestParam("key") String key
                                          ) {
        try {

//            BufferedImage image = ImageIO.read(imageFile.getInputStream());
//            BufferedImage modifiedImage = embedMessageIntoImage(image, message);
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            ImageIO.write(modifiedImage, "bmp", outputStream);
//            byte[] imageBytes = outputStream.toByteArray();
            byte[] imageBytes = BMPEncoder.hideMessageInBmp(imageFile.getInputStream(),message,key);
            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
            return ResponseEntity.ok(encodedImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error embedding message.");
        }
    }
    @PostMapping("/embed")
    public ResponseEntity<?> embedMessage(@RequestParam("image") MultipartFile imageFile,
                                                 @RequestParam("message") String message
    ) {
        try {

//            BufferedImage image = ImageIO.read(imageFile.getInputStream());
//            BufferedImage modifiedImage = embedMessageIntoImage(image, message);
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            ImageIO.write(modifiedImage, "bmp", outputStream);
//            byte[] imageBytes = outputStream.toByteArray();
            byte[] imageBytes = BMPEncoder.hideMessageInBmpWithoutKey(imageFile.getInputStream(),message);
            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
            return ResponseEntity.ok(encodedImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error embedding message.");
        }
    }
    @PostMapping("/extractWithKey")
    public ResponseEntity<?> extractMessageWithKey(@RequestParam("image") MultipartFile imageFile, @RequestParam("key") String key) {
        try {
            System.out.println(key);
            String message = BMPDecoder.extractMessageFromBmp(imageFile.getInputStream(),key);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error extracting message.");
        }
    }
    @PostMapping("/extract")
    public ResponseEntity<?> extractMessage(@RequestParam("image") MultipartFile imageFile) {
        try {
            String message = BMPDecoder.extractMessageFromBmpWithoutKey(imageFile.getInputStream());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error extracting message.");
        }
    }
    private int calculateMaxEmbeddableLength(MultipartFile image,boolean isKey) throws Exception {
        InputStream inputStream=image.getInputStream();
        byte[] header = new byte[54];
        inputStream.read(header);
        int width = ByteBuffer.wrap(header, 18, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int height = ByteBuffer.wrap(header, 22, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int maxLength = 0;
        if(isKey){
            if (header[28] == 24){
                maxLength = (int)Math.floor((width * height*3 -16) / 256)*16;
            }
            else if (header[28] == 8){
                maxLength = (int)Math.floor((width * height -16) / 256)*16;
            }
        }
        else{
            if (header[28] == 24){
                maxLength = (width * height*3)/8;
            }
            else if (header[28] == 8){
                maxLength = (width * height)/8;
            }
        }
        System.out.println(width+"  "+height+"  "+maxLength);
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

    @PostMapping("/noise")
    public ResponseEntity<?> addNoise(@RequestParam("image") MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image file.");
            }

            GaussianNoise gauss = new GaussianNoise();

            BufferedImage modifiedImage = gauss.addNoiseImage(image);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(modifiedImage, "bmp", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);

            return ResponseEntity.ok(encodedImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image.");
        }
    }
}
