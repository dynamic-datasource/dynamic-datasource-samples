/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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