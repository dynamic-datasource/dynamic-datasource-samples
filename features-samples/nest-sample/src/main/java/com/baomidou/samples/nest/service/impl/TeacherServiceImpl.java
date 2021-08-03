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

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.samples.nest.entiry.Teacher;
import com.baomidou.samples.nest.mapper.TeacherMapper;
import com.baomidou.samples.nest.service.StudentService;
import com.baomidou.samples.nest.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@DS("teacher")
public class TeacherServiceImpl implements TeacherService {

    @Resource
    private TeacherMapper teacherMapper;
    @Autowired
    private StudentService studentService;

    @Override
    @Transactional
    public boolean addTeacherWithTx(String name, Integer age) {
        return teacherMapper.addTeacher(name, age);
    }

    @Override
    public boolean addTeacherNoTx(String name, Integer age) {
        return teacherMapper.addTeacher(name, age);
    }

    @Override
    public List<Teacher> selectTeachers() {
        return teacherMapper.selectTeachers();
    }

    @Override
    public void selectTeachersInnerStudents() {
        teacherMapper.selectTeachers();
        studentService.selectStudents();
    }
}
