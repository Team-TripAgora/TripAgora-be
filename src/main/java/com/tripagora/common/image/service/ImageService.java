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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    private final AmazonS3 amazonS3;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    private static final String FOLDER_NAME = "images/";
    private static final Pattern EXTENSION = Pattern.compile("[a-z0-9]{1,5}");
    // 외부 URL에서 이미지를 받을 때 연결·읽기를 기다리는 최대 시간
    private static final int DOWNLOAD_TIMEOUT_MS = 5_000;

    public String uploadImage(MultipartFile file) {
        // 1. 파일 유효성 검사
        validateImageFile(file);

        // 2. 고유한 파일 이름 생성 (원본 확장자 유지)
        String fileName = createStoreFileName(extensionOf(file.getOriginalFilename(), file.getContentType()));

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

    // 외부 URL(소셜 프로필 사진)의 이미지를 받아 올린다. 응답의 Content-Type으로 형식을 정하고, 이미지가 아니면 올리지 않는다
    public String uploadImageFromUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        try {
            URLConnection connection = URI.create(imageUrl).toURL().openConnection();
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.connect();

            // 응답 코드를 먼저 읽어 타임아웃·오류 응답을 실패로 처리한다 (getContentType()은 읽기 오류를 삼키고 null을 돌려준다)
            if (connection instanceof HttpURLConnection httpConnection && httpConnection.getResponseCode() / 100 != 2) {
                throw new ImageUploadFailedException();
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new UnSupportedImageFormatException();
            }
            String mimeType = contentType.split(";")[0].trim();
            String fileName = createStoreFileName(extensionOf(mimeType));

            try (InputStream inputStream = connection.getInputStream()) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(mimeType);
                long contentLength = connection.getContentLengthLong();
                if (contentLength >= 0) {
                    metadata.setContentLength(contentLength);
                }

                amazonS3.putObject(new PutObjectRequest(bucketName, fileName, inputStream, metadata));
            }
            return amazonS3.getUrl(bucketName, fileName).toString();

        } catch (IOException | IllegalArgumentException e) {
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

    // 여러 파일을 지운다. 하나가 실패해도 나머지를 지우고, 실패는 로그만 남긴다 (파일이 남을 뿐 깨진 이미지는 생기지 않는다)
    public void deleteImagesQuietly(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            try {
                deleteImage(imageUrl);
            } catch (RuntimeException e) {
                log.warn("S3 이미지 삭제 실패, 파일이 남는다: {}", imageUrl, e);
            }
        }
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new UnSupportedImageFormatException();
        }
    }

    // images/{uuid}.{확장자}. 확장자를 정하지 못하면 붙이지 않는다
    private String createStoreFileName(String extension) {
        String uuid = UUID.randomUUID().toString();
        return extension == null ? FOLDER_NAME + uuid : FOLDER_NAME + uuid + "." + extension;
    }

    // 확장자는 원본 파일 이름에서 정한다. 없거나 형식이 이상하면 Content-Type으로 정한다 (image/png → png, image/jpeg → jpg)
    private String extensionOf(String originalFilename, String contentType) {
        String fromFilename = StringUtils.getFilenameExtension(originalFilename);
        if (fromFilename != null && EXTENSION.matcher(fromFilename.toLowerCase()).matches()) {
            return fromFilename.toLowerCase();
        }
        return extensionOf(contentType);
    }

    private String extensionOf(String contentType) {
        String subtype = contentType.substring(contentType.indexOf('/') + 1).split(";")[0].trim().toLowerCase();
        if (subtype.equals("jpeg")) {
            return "jpg";
        }
        return EXTENSION.matcher(subtype).matches() ? subtype : null;
    }

    // S3 이미지 주소(https://{버킷}.s3.{리전}.amazonaws.com/images/...)를 파싱해 파일 키를 꺼낸다.
    // 호스트가 이 버킷이고 경로가 images/ 아래일 때만 받는다. 경로는 %인코딩이 풀린 값이고 쿼리는 포함하지 않는다
    private String extractFileNameFromUrl(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            if (!"https".equals(uri.getScheme()) || host == null || !host.startsWith(bucketName + ".s3.")
                    || path == null || !path.startsWith("/" + FOLDER_NAME)) {
                throw new InvalidImageUrlFormatException();
            }
            return path.substring(1);
        } catch (IllegalArgumentException e) {
            throw new InvalidImageUrlFormatException();
        }
    }
}