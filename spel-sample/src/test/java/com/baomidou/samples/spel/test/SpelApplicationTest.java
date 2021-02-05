package com.baomidou.samples.spel.test;

import com.baomidou.samples.spel.SpelApplication;
import com.baomidou.samples.spel.entity.User;
import com.baomidou.samples.spel.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpSession;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SpelApplication.class)
public class SpelApplicationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private HttpSession session;

    @BeforeAll
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void selectSpelBySession() {
        session.setAttribute("tenantName", "tenant1");
        userService.selectSpelBySession();
    }

    @Test
    public void selectSpelByHeader() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/header")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("tenantName", "tenant1")
        )
                .andDo(print()).andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsString();
    }

    @Test
    public void selectSpelByKey() {
        userService.selectSpelByKey("tenant1");
    }

    @Test
    public void selecSpelByTenant() {
        User user = new User();
        user.setTenantName("tenant2");
        userService.selecSpelByTenant(user);
    }
}
