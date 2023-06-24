package ctw.post.model.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 文件上传请求
 *
 * @author  liva
 * @date 2023/6/13
 */
@Data
public class UploadFileRequest implements Serializable {

    /**
     * 业务
     */
    private String biz;

    private static final long serialVersionUID = 1L;
}