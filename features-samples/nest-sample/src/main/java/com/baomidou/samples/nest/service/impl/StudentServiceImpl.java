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
import com.baomidou.samples.nest.entiry.Student;
import com.baomidou.samples.nest.mapper.StudentMapper;
import com.baomidou.samples.nest.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@DS("student")
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    @Transactional
    public boolean addStudentWithTx(String name, Integer age) {
        return studentMapper.addStudent(name, age);
    }

    @Override
    public boolean addStudentNoTx(String name, Integer age) {
        return studentMapper.addStudent(name, age);
    }

    @Override
    public List<Student> selectStudents() {
        return studentMapper.selectStudents();
    }
}