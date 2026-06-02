package capstone2.team3.realplan.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "폴더 생성 요청")
public class FolderCreateRequest {

    @Schema(example = "학교 과제")
    @NotBlank(message = "폴더 이름은 필수입니다.")
    @Size(max = 50, message = "폴더 이름은 50자 이하여야 합니다.")
    private String name;
}
