package org.example.gudyeeday.common.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gudyeeday.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Uploader {

    private final S3Client s3Client;
    private final AppProperties props;

    public String upload(MultipartFile file, String dirName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = cleanFilename(originalFilename);
        String storeFileName = dirName + "/" + UUID.randomUUID() + "_" + safeFilename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(props.getS3().getBucket())
                    .key(storeFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            // S3에 업로드된 객체의 URL 반환
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    props.getS3().getBucket(),
                    props.getS3().getRegion(),
                    storeFileName);

        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
        }
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(props.getS3().getBucket())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
        }
    }

    // S3 URL에서 Key(경로 + 파일명)를 추출해주는 헬퍼 메서드
    private String extractKeyFromUrl(String fileUrl) {
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/",
                props.getS3().getBucket(),
                props.getS3().getRegion());

        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }

        int slashIndex = fileUrl.indexOf(".amazonaws.com/");
        if (slashIndex != -1) {
            return fileUrl.substring(slashIndex + ".amazonaws.com/".length());
        }

        return fileUrl;
    }

    private String cleanFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }
        // # 및 ? 문자를 언더스코어(_)로 치환하여 S3 Key 및 URL 안전성 확보
        return originalFilename.replaceAll("[#?]", "_");
    }
}
