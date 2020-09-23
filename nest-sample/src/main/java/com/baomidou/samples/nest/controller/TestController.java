package com.baomidou.samples.nest.controller;

import com.baomidou.samples.nest.service.SchoolService;
import com.baomidou.samples.nest.service.StudentService;
import com.baomidou.samples.nest.service.TeacherService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TestController {

    private final SchoolService schoolService;
    private final TeacherService teacherService;
    private final StudentService studentService;

    @GetMapping("/tx1")
    public void tx1() {
        //外层不加事物，里面每个单独加事物（支持）
        schoolService.addTeacherAndStudent();
    }

    @GetMapping("/tx2")
    public void tx2() {
        //外层加事物，里面每个也加事物（不支持）
        schoolService.addTeacherAndStudentWithTx();
    }

    @GetMapping("/tx3")
    public void tx3() {
        //单独调用加事物单库（支持）
        teacherService.addTeacherWithTx("Mr Wang", 30);
        studentService.addStudentWithTx("Zhang San", 12);
    }
}
