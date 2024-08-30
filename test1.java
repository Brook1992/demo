import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.FileItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class MyController {

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/callDownstreamApi")
    public ResponseEntity<String> callDownstreamApi(
            @RequestParam(value = "filePath", required = false) String filePath,
            @RequestParam(value = "applicationId", required = false) String applicationId) throws IOException {

        // Step 1: 构建请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        if (filePath != null && !filePath.isEmpty()) {
            // 从JAR包中加载文件
            Resource resource = new ClassPathResource(filePath);
            if (resource.exists()) {
                // 使用标准的MultipartFile实现
                FileItem fileItem = new DiskFileItem("file",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE, false, 
                        resource.getFilename(), 
                        (int) resource.contentLength(), 
                        resource.getFile().getParentFile());

                try (InputStream inputStream = resource.getInputStream()) {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputStream.readAllBytes());
                    fileItem.getOutputStream().write(byteArrayInputStream.readAllBytes());
                }

                MultipartFile multipartFile = new CommonsMultipartFile(fileItem);
                body.add("file", multipartFile.getResource());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found in classpath: " + filePath);
            }
        }

        if (applicationId != null) {
            body.add("applicationId", applicationId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Step 2: 调用下游接口
        String downstreamUrl = "http://downstream-api-endpoint";
        ResponseEntity<String> response = restTemplate.exchange(downstreamUrl, HttpMethod.POST, requestEntity, String.class);

        // Step 3: 返回结果
        return response;
    }
}
