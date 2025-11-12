package com.example.handson;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Testcontainers設定
 * PostgreSQLとLocalStack(S3)のコンテナを起動し、テスト用のBeanを提供
 * S3TemplateはSpring Cloud AWSが自動的にS3Clientから生成
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.5"))
				.withDatabaseName("test_app")
				.withUsername("test")
				.withPassword("test");
	}

	@Bean
	LocalStackContainer localStackContainer() {
		return new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
				.withServices(S3);
	}

	@Bean
	@Primary
	S3Client s3Client(LocalStackContainer localStackContainer) {
		return S3Client.builder()
				.endpointOverride(localStackContainer.getEndpointOverride(S3))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(
										localStackContainer.getAccessKey(),
										localStackContainer.getSecretKey()
								)
						)
				)
				.region(Region.of(localStackContainer.getRegion()))
				.forcePathStyle(true)
				.build();
	}

	// S3TemplateはSpring Cloud AWSが自動的にS3Clientから生成するため、
	// ここでは明示的にBeanを定義する必要はありません

}
