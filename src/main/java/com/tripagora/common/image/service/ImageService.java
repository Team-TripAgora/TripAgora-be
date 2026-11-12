package com.tripagora.common.image.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tripagora.common.image.exception.ImageDeletionFailedException;
import com.tripagora.common.image.exception.ImageUploadFailedException;
import com.tripagora.common.image.exception.InvalidImageUrlFormatException;
import com.tripagora.common.image.exception.UnSupportedImageFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private final AmazonS3 amazonS3;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private static final String FOLDER_NAME = "images/";

    public String uploadImage(MultipartFile file) {
        // 1. 파일 유효성 검사
        validateImageFile(file);

        // 2. 고유한 파일 이름 생성
        String fileName = createStoreFileName(Objects.requireNonNull(file.getOriginalFilename()));

        // 3. S3에 업로드
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
        } catch (IOException e) {
            throw new ImageUploadFailedException();
        }

        // 4. 업로드된 이미지의 URL 반환
        return amazonS3.getUrl(bucketName, fileName).toString();
    }

    public String uploadImageFromUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        String fileName = createStoreFileName(UUID.randomUUID() + ".jpg");

        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");

            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, inputStream, metadata));
            return amazonS3.getUrl(bucketName, fileName).toString();

        } catch (IOException e) {
            throw new ImageUploadFailedException();
        }
    }

    public String updateImage(String oldImageUrl, MultipartFile newFile) {
        // 새 이미지 업로드
        String newImageUrl = uploadImage(newFile);

        // 기존 이미지가 있다면 삭제
        if (StringUtils.hasText(oldImageUrl)) {
            try {
                deleteImage(oldImageUrl);
            } catch (Exception e) {
                // 기존 이미지 삭제 실패는 로깅만 하고, 새 이미지 URL은 그대로 반환
                log.warn("기존 이미지 삭제 실패: [URL: {}], [Error: {}]", oldImageUrl, e.getMessage());
            }
        }

        return newImageUrl;
    }

    public void deleteImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return; // URL이 없으면 삭제 로직을 실행하지 않음
        }
        try {
            String fileName = extractFileNameFromUrl(imageUrl);
            amazonS3.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            throw new ImageDeletionFailedException();
        }
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new UnSupportedImageFormatException();
        }
    }

    private String createStoreFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        return FOLDER_NAME + uuid + ".jpg";
    }

    private String extractFileNameFromUrl(String imageUrl) {
        try {
            String bucketUrl = "https://" + bucketName + ".s3.";
            int startIndex = imageUrl.indexOf(bucketUrl);
            if (startIndex == -1) {
                throw new InvalidImageUrlFormatException();
            }
            String path = imageUrl.substring(imageUrl.indexOf(FOLDER_NAME));
            return URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new InvalidImageUrlFormatException();
        }
    }
}