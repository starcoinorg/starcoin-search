package org.starcoin.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableEncryptableProperties
public class JasyptConfig {
    @Bean(name = "encryptorBean")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("starcoin-scan");            // 这是加密的盐，建议直接硬编码，提高安全性
        config.setAlgorithm("PBEWithMD5AndDES");    // 加密算法
        config.setKeyObtentionIterations("1000");   // key 迭代次数
        config.setPoolSize("1");                    // 池大小
        config.setProviderName("SunJCE");           // 提供方
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");  // 随机盐生成器
        config.setStringOutputType("base64");       // 加密后输出字符串编码方式
        encryptor.setConfig(config);
        return encryptor;
    }
}
