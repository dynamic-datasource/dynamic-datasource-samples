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
