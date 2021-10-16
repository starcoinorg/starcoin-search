package org.starcoin.scan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.starcoin")
public class ScanApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScanApplication.class, args);
    }
}
