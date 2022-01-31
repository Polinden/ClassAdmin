package com.wolper.config;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.JavaMailSender;
import com.wolper.services.RegisterServices;
import com.wolper.services.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:config.properties", encoding = "UTF-8")
@ComponentScan(basePackages = { "com.wolper.services"})

public class ServiceConfig {

    @Autowired
    Environment env;


    //datasource settings
    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        String host = env.getProperty("spring.datasource.url");
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(host);
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));

        return dataSource;
    }


    //transaction management
    @Bean
    public PlatformTransactionManager transactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }


    //dao been
    @Bean
    RegisterServices jrDAO() {
        RegisterService jrnDAO= new RegisterService();
        //set datasource
        jrnDAO.setDataSource(dataSource());
        //properties to be setup for being consistent with other modules
        //names of topic and works, which are used by users of Register module
        jrnDAO.setTOPIC_WORK(env.getProperty("register.topic.work"));
        jrnDAO.setWORKS(Arrays.asList(env.getProperty("register.works").split(":")));
        return jrnDAO;
    }


    //mail bean
    @Bean
    public JavaMailSender getJavaMailSender() {
       JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
       mailSender.setHost("smtp.gmail.com");
       mailSender.setPort(587);
       mailSender.setUsername(env.getProperty("gmail.login"));
       mailSender.setPassword(env.getProperty("gmail.password"));
       Properties props = mailSender.getJavaMailProperties();
       props.put("mail.transport.protocol", "smtp");
       props.put("mail.smtp.auth", "true");
       props.put("mail.smtp.starttls.enable", "true");
       props.put("mail.debug", "true");
       return mailSender;
   }





}
