package com.swasthai.report_generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.swasthai.report_generator.auth.config.AuthRateLimitProperties;
import com.swasthai.report_generator.license.config.LicenseProperties;
import com.swasthai.report_generator.report.config.ReportRetentionProperties;
import com.swasthai.report_generator.security.jwt.JwtProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
		JwtProperties.class,
		ReportRetentionProperties.class,
		LicenseProperties.class,
		AuthRateLimitProperties.class
})
public class ReportGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportGeneratorApplication.class, args);
	}

}
