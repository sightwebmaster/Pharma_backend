package com.pharmaApp.user.domain.port.output;

public interface QRCodeGeneratorPort {
    String generate(String content);  // retourne base64 PNG
}