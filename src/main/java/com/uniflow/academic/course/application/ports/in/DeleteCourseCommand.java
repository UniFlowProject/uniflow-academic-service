package com.uniflow.academic.course.application.ports.in;

public interface DeleteCourseCommand {

    void execute(String courseId, String userId);
}
