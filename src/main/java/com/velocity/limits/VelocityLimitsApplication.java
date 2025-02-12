package com.velocity.limits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.limits.model.LoadRequest;
import com.velocity.limits.model.LoadResponse;
import com.velocity.limits.service.LoadLimitService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class VelocityLimitsApplication {
    private static final Logger logger = LoggerFactory.getLogger(VelocityLimitsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(VelocityLimitsApplication.class, args);
    }

    @Bean
    public CommandLineRunner processInputFile(LoadLimitService loadLimitService, ObjectMapper objectMapper) {
        return args -> {
            // Try Docker path first, then fallback to local path
            String inputPath = "/input.txt";
            String outputPath = "/output.txt";

            // If Docker paths don't exist, try local paths
            if (!Files.exists(Paths.get(inputPath))) {
                inputPath = "src/main/resources/input.txt";
                outputPath = "output.txt";
            }

            logger.info("Using input path: {}", inputPath);
            logger.info("Using output path: {}", outputPath);

            try (BufferedReader reader = new BufferedReader(new FileReader(inputPath));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        LoadRequest request = objectMapper.readValue(line, LoadRequest.class);
                        LoadResponse response = loadLimitService.processLoad(request);
                        
                        if (response != null) {
                            writer.write(objectMapper.writeValueAsString(response));
                            writer.newLine();
                        }
                    } catch (Exception e) {
                        logger.error("Error processing line: " + line, e);
                    }
                }
            }
        };
    }
} 