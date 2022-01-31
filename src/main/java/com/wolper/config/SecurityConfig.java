package com.wolper.config;


import com.wolper.antibrute.IpDisableFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;




@Configuration
@EnableWebSecurity
@ComponentScan(value = {"com.wolper.config", "com.wolper.antibrute"})           //for data source injection
@EnableGlobalMethodSecurity(prePostEnabled = true)                              //for annotations in contollers
public class SecurityConfig extends WebSecurityConfigurerAdapter {



        @Autowired
        DataSource dataSource;



        @Autowired
        //why? for teaching DefaultAuthenticationEventPublisher to publish
        private ApplicationEventPublisher applicationEventPublisher;


        @Autowired
        public void configAuthentication(AuthenticationManagerBuilder auth) throws Exception {

            auth
                    //setting up spring security events
                    .authenticationEventPublisher(new DefaultAuthenticationEventPublisher(applicationEventPublisher))
                    //setting up database realm
                    .jdbcAuthentication().dataSource(dataSource)
                    .usersByUsernameQuery(
                            "select login, password, enabled from spr_users where login=?")
                    .authoritiesByUsernameQuery(
                            "select login, authority from spr_auth where login=?")
		            .rolePrefix("");
        }


        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.authorizeRequests()
                    .antMatchers("/public/**").permitAll().anyRequest().authenticated()
                    .and()

                    .formLogin().loginPage("/public/index.html")
                    .usernameParameter("username").passwordParameter("password").loginProcessingUrl("/login")
                    .defaultSuccessUrl("/admin/startpage", true).failureUrl("/public/pass_error.html")
                    .and()

                    .logout().logoutUrl("/goout").logoutSuccessUrl("/public/index.html")
                    .invalidateHttpSession(true).deleteCookies("JSESSIONID", "SESSION")
                    .and()

                    .exceptionHandling().accessDeniedPage("/403")
                    .and()

                    .sessionManagement().invalidSessionUrl("/public/index.html")
                    .and()

                    .addFilterBefore(ipControlHandler(), UsernamePasswordAuthenticationFilter.class)

                    .csrf().disable()

                    //https setup
                    .requiresChannel().anyRequest().requiresSecure();
        }



    //to filter by ip and prevent brute force
    @Bean protected IpDisableFilter  ipControlHandler() throws Exception {
        return new IpDisableFilter();
    }
}


