package org.kohsuke;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.*;

public class AppTest {
    @Test
    public void lookUpCat() {
        ApplicationContext context = new AnnotationConfigApplicationContext(App.class);
        Object o = context.getBean("cat");
        assertThat(o, Matchers.instanceOf(Cat.class));
    }
}
