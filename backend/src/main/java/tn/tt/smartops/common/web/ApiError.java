package tn.tt.smartops.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path,
                       Map<String, String> fieldErrors, List<String> details) {
}
