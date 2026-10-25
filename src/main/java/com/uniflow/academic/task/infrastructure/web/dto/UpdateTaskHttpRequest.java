package com.uniflow.academic.task.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Partial update. Jackson only calls a setter for the properties present in the JSON, so the
 * setters record which fields were sent: an explicit {@code null} is told apart from an absent
 * field, which is what lets {@code actualTimeHours} be cleared.
 */
@Getter
@Schema(description = "Partial update of a task: only the fields sent are changed")
public class UpdateTaskHttpRequest {

    public static final String ACTUAL_TIME_HOURS = "actualTimeHours";

    private final Map<String, Object> provided = new HashMap<>();

    @Size(max = TaskPatterns.MAX_TITLE)
    @Pattern(regexp = TaskPatterns.NOT_BLANK, message = "must not be blank")
    private String title;

    @Size(max = TaskPatterns.MAX_DESCRIPTION)
    private String description;

    @Size(max = TaskPatterns.MAX_ID)
    @Pattern(regexp = TaskPatterns.NOT_BLANK, message = "must not be blank")
    private String courseId;

    @Size(max = TaskPatterns.MAX_ID)
    @Pattern(regexp = TaskPatterns.NOT_BLANK, message = "must not be blank")
    private String periodId;

    private Instant dueDate;

    @Pattern(regexp = TaskPatterns.PRIORITY)
    private String priority;

    @Pattern(regexp = TaskPatterns.TYPE)
    private String type;

    @PositiveOrZero
    private Integer estimatedTimeHours;

    @Schema(description = "Send null to clear it")
    @PositiveOrZero
    private Integer actualTimeHours;

    @Size(max = TaskPatterns.MAX_LIST_SIZE)
    private List<@NotBlank @Size(max = TaskPatterns.MAX_ELEMENT) String> tags;

    @Size(max = TaskPatterns.MAX_LIST_SIZE)
    private List<@NotBlank @Size(max = TaskPatterns.MAX_ELEMENT) String> groupMembers;

    @Size(max = TaskPatterns.MAX_LIST_SIZE)
    private List<@NotBlank @Size(max = TaskPatterns.MAX_ATTACHMENT)
            @Pattern(regexp = TaskPatterns.HTTP_URL, message = "must be an http or https URL") String> attachments;

    public void setTitle(String title) {
        this.title = title;
        provided.put("title", title);
    }

    public void setDescription(String description) {
        this.description = description;
        provided.put("description", description);
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
        provided.put("courseId", courseId);
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
        provided.put("periodId", periodId);
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
        provided.put("dueDate", dueDate);
    }

    public void setPriority(String priority) {
        this.priority = priority;
        provided.put("priority", priority);
    }

    public void setType(String type) {
        this.type = type;
        provided.put("type", type);
    }

    public void setEstimatedTimeHours(Integer estimatedTimeHours) {
        this.estimatedTimeHours = estimatedTimeHours;
        provided.put("estimatedTimeHours", estimatedTimeHours);
    }

    public void setActualTimeHours(Integer actualTimeHours) {
        this.actualTimeHours = actualTimeHours;
        provided.put(ACTUAL_TIME_HOURS, actualTimeHours);
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        provided.put("tags", tags);
    }

    public void setGroupMembers(List<String> groupMembers) {
        this.groupMembers = groupMembers;
        provided.put("groupMembers", groupMembers);
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
        provided.put("attachments", attachments);
    }

    public boolean clearsActualTimeHours() {
        return provided.containsKey(ACTUAL_TIME_HOURS) && actualTimeHours == null;
    }

    /** Names of required fields the client explicitly set to null. */
    public Stream<String> requiredFieldsSetToNull() {
        return provided.entrySet().stream()
                .filter(entry -> entry.getValue() == null && !ACTUAL_TIME_HOURS.equals(entry.getKey()))
                .map(Map.Entry::getKey)
                .sorted();
    }
}
