package com.baomidou.samples.nest.test;


import com.baomidou.samples.nest.NestApplication;
import com.baomidou.samples.nest.service.SchoolService;
import com.baomidou.samples.nest.service.StudentService;
import com.baomidou.samples.nest.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Random;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = NestApplication.class)

public class NestApplicationTest {

    private final Random random = new Random();

    @Autowired
    private TeacherService teacherService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private SchoolService schoolService;

    @Test
    public void nest1() {
        //直接在controller
        teacherService.selectTeachers();
        studentService.selectStudents();
    }

    @Test
    public void nest2() {
        schoolService.selectTeachersAndStudents();
    }

    @Test
    public void nest3() {
        schoolService.selectTeachersInnerStudents();
    }

    @Test
    public void tx() {
        teacherService.selectTeachers();
    }
}