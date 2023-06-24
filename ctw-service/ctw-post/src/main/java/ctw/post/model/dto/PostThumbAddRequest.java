package ctw.post.model.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 帖子点赞请求
 *
 * @author  liva
 * @date 2023/6/13
 */
@Data
public class PostThumbAddRequest implements Serializable {

    /**
     * 帖子 id
     */
    private Long postId;

    private static final long serialVersionUID = 1L;
}