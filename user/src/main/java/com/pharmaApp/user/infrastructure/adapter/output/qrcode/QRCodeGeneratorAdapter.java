package com.pharmaApp.user.infrastructure.adapter.output.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pharmaApp.user.domain.port.output.QRCodeGeneratorPort;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Component
public class QRCodeGeneratorAdapter implements QRCodeGeneratorPort {

    private static final int WIDTH  = 300;
    private static final int HEIGHT = 300;

    @Override
    public String generate(String content) {
        try {
            // 1. Générer la BitMatrix (grille QR Code)
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    WIDTH,
                    HEIGHT
            );

            // 2. Convertir BitMatrix → image PNG en mémoire
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            // 3. Encoder en Base64 avec préfixe data URI
            String base64Image = Base64.getEncoder()
                    .encodeToString(outputStream.toByteArray());

            return "data:image/png;base64," + base64Image;

        } catch (WriterException | IOException e) {
            throw new RuntimeException(
                    "Erreur lors de la génération du QR Code pour : " + content, e
            );
        }
    }
}