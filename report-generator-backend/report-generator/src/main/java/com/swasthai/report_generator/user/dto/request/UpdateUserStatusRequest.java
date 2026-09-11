package com.swasthai.report_generator.user.dto.request;

import com.swasthai.report_generator.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;
}