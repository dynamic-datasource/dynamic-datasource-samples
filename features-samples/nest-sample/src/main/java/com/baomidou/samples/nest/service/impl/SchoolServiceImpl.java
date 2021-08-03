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
package com.baomidou.samples.nest.service.impl;

import com.baomidou.samples.nest.service.SchoolService;
import com.baomidou.samples.nest.service.StudentService;
import com.baomidou.samples.nest.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolServiceImpl implements SchoolService {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private StudentService studentService;

    @Override
    public void selectTeachersAndStudents() {
        teacherService.selectTeachers();
        studentService.selectStudents();
    }

    @Override
    public void selectTeachersInnerStudents() {
        teacherService.selectTeachersInnerStudents();
    }

    @Override
    public void addTeacherAndStudent() {
        teacherService.addTeacherWithTx("ss", 1);
        studentService.addStudentWithTx("tt", 2);
    }

    @Override
    @Transactional
    public void addTeacherAndStudentWithTx() {
        teacherService.addTeacherWithTx("ss", 1);
        studentService.addStudentNoTx("tt", 2);
    }
}
