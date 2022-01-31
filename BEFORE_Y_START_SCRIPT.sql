-- THIS IS
-- A SCRIPT TO INITIATE THE CLASS REGISTER DATABASES BEFORE THE FIRST START OF THE WEB APPLICATIONS
--
-- !!DEAR NEWCOMER!!
-- (!) FIRST GO TO THE END OF THE FILE (WITH THE SAME DUMB FACE) 
-- (!) AND ENTER THE ESSENTIAL DATA!
--
--                          ||||||||||||||
--                          ______________
--                         /              \
--                        |                |
--                       __ *****    ***** __ 
--                      (    (*)  ||   (*)   )
--                       (_       ||       _)
--                         |              |
--                          \    ====    /
--                            +________+
--
--
-- IT'S IMPORTENT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ONLY WHEN YOU FINISH WITH FILLING AT THE DATA IN THE END OF 
-- THIS FILE, YOU ARE FREE TO LAUNCH IT IN THE POSTGRES! 
-- GO DOWN NOW!





SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: active_prepod(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.active_prepod() RETURNS TABLE(f1 text, f2 text, f3 text)
    LANGUAGE sql
    AS $$
 with pppp as (select subject, class_name from journal where date>'2017.08.01' and mark<>0),  
  rrrr as (select class_name, subject, first_name||' '||second_name as fnsn from schedule_and_class 
   join prepod on prepod.schedule_id=schedule_and_class.schedules_id join users on users.login=prepod.login) 
     select distinct fnsn, rrrr.class_name, rrrr.subject from rrrr join pppp 
       on pppp.class_name=rrrr.class_name and pppp.subject=rrrr.subject order by fnsn;
$$;


ALTER FUNCTION public.active_prepod() OWNER TO postgres;

--
-- Name: class_aver(text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.class_aver(class text, subj text) RETURNS TABLE(s_name character varying, s_mark text)
    LANGUAGE plpgsql
    AS $$ 
BEGIN
return query select u.first_name, to_char(avg (CASE WHEN mark <> 0 THEN mark ELSE NULL END), '99D9') as av from journal 
inner join 
users as u on u.login=stud_login where class_name=class and subject=subj group by u.login order by av desc;
END
$$;


ALTER FUNCTION public.class_aver(class text, subj text) OWNER TO postgres;

--
-- Name: class_aver(text, text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.class_aver(class text, subj text, topic_work text) RETURNS TABLE(s_name text, s_mark numeric)
    LANGUAGE plpgsql
    AS $$
        BEGIN
        IF topic_work<>'' THEN
        return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        inner join
        users as u on u.login=stud_login where class_name=class and subject=subj and date>get_stud_year_startj()
         and work=topic_work
         group by u.login order by av desc;
        
        ELSE
        return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        inner join
        users as u on u.login=stud_login where class_name=class and subject=subj and date>get_stud_year_startj() group by u.login order by av desc;
        END IF;
        END
        $$;


ALTER FUNCTION public.class_aver(class text, subj text, topic_work text) OWNER TO postgres;

--
-- Name: class_aver_allsubj(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.class_aver_allsubj(class text) RETURNS TABLE(s_name character varying, s_mark text)
    LANGUAGE plpgsql
    AS $$ 
BEGIN
return query
select u.first_name, to_char(avg (CASE WHEN mark <> 0 THEN mark ELSE NULL END), '99D9') as av from journal 
inner join 
users as u on u.login=stud_login where class_name=class group by u.login order by av desc;
END
$$;


ALTER FUNCTION public.class_aver_allsubj(class text) OWNER TO postgres;

--
-- Name: class_aver_allsubj(text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.class_aver_allsubj(class text, topic_work text) RETURNS TABLE(s_name text, s_mark numeric)
    LANGUAGE plpgsql
    AS $$
        BEGIN
        IF topic_work<>'' THEN
        return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        inner join
        users as u on u.login=stud_login where class_name=class and date>=get_stud_year_startj()
        and work=topic_work
        group by u.login order by av desc;
        
        ELSE
        return query select u.first_name||' '||substring(u.second_name, 1,1)||'.', round(avg(CASE WHEN mark <> 0 THEN mark ELSE NULL END),1) as av from journal
        inner join
        users as u on u.login=stud_login where class_name=class and date>=get_stud_year_startj() group by u.login order by av desc;
        END IF;
        END
        $$;


ALTER FUNCTION public.class_aver_allsubj(class text, topic_work text) OWNER TO postgres;

--
-- Name: clean_jn(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.clean_jn() RETURNS void
    LANGUAGE plpgsql
    AS $$
        begin
        with dd as (select d,s,cn from
        (select date as d, subject as s, class_name as cn, sum(mark)
        as m from journal group by date, subject, class_name) as dd1 where m=0)
        delete from journal as j where (j.date, j.class_name, j.subject) in
        (select d,cn,s from dd);
        end;
        $$;


ALTER FUNCTION public.clean_jn() OWNER TO postgres;

--
-- Name: del_spaces(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.del_spaces() RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
        update users set first_name = regexp_replace(first_name, '[\s\t\r]', '');
        update users set second_name = regexp_replace(second_name, '[\s\t\r]', '');
        update users set third_name = regexp_replace(third_name, '[\s\t\r]', '');
        update users set login = regexp_replace(login, '[\s\t\r]', '');
        update users set password = regexp_replace(password, '[\s\t\r]', '');


END;
$$;


ALTER FUNCTION public.del_spaces() OWNER TO postgres;

--
-- Name: get_stud_year_startj(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_stud_year_startj() RETURNS timestamp without time zone
    LANGUAGE plpgsql
    AS $$
        BEGIN
        return case when (age(now(),  to_timestamp(date_part('year', now())||'-09-01', 'YYYY-MM-DD'))<interval '0') then (to_timestamp(date_part('year', now())||'-09-01', 'YYYY-MM-DD') - interval '1 years') else (to_timestamp(date_part('year', now())||'-09-01', 'YYYY-MM-DD'))  end;
        END
        $$;


ALTER FUNCTION public.get_stud_year_startj() OWNER TO postgres;

--
-- Name: get_stud_year_summary_end(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_stud_year_summary_end() RETURNS timestamp without time zone
    LANGUAGE plpgsql
    AS $$
        BEGIN
          return case when (age(now(),  to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD'))<interval '0')
          then (to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD') - interval '1 years')
          else (to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD'))  end;
        END
        $$;


ALTER FUNCTION public.get_stud_year_summary_end() OWNER TO postgres;

--
-- Name: get_stud_year_summary_start(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_stud_year_summary_start() RETURNS timestamp without time zone
    LANGUAGE plpgsql
    AS $$
        BEGIN
          return case when (age(now(),  to_timestamp(date_part('year', now())||'-08-31', 'YYYY-MM-DD'))<interval '0')
          then (to_timestamp(date_part('year', now())||'-08-01', 'YYYY-MM-DD') - interval '1 years')
          else (to_timestamp(date_part('year', now())||'-08-01', 'YYYY-MM-DD'))  end;
        END
        $$;


ALTER FUNCTION public.get_stud_year_summary_start() OWNER TO postgres;

--
-- Name: top21(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.top21(topic_work text) RETURNS TABLE(n text, m numeric)
    LANGUAGE plpgsql
    AS $_$
        BEGIN
        IF topic_work<>'' THEN
        return query
        with sss1 as
                (select stud_login as login, u.first_name||' '
                 ||substring(u.second_name, 1,1)||'.' as name,
                 round(avg(CASE WHEN mark <> 0 THEN mark ELSE null END),1) as av,
        count(mark) as cm from journal
        inner join users as u on u.login=stud_login
        where date>get_stud_year_startj() and work=topic_work group by stud_login, u.login),

        sss2 as
        (select max(cm)+0.1 as cccm, max(av)+0.1 as xxxm from sss1)

        select name||' '||cas.class_name as names, av from sss2, sss1
        inner join class_and_students as cas on cas.students_login=login
        where (cas.class_name ~ '\d{1,2}[а-я]{1,1}$')=true and av IS NOT NULL
        order by (7*av/xxxm+cm/cccm) desc limit 20;

        ELSE
        return query
        with sss1 as
                (select stud_login as login, u.first_name||' '
                 ||substring(u.second_name, 1,1)||'.' as name,
                 round(avg(CASE WHEN mark <> 0 THEN mark ELSE null END),1) as av,
        count(mark) as cm from journal
        inner join users as u on u.login=stud_login
        where date>get_stud_year_startj() group by stud_login, u.login),

        sss2 as
        (select max(cm)+0.1 as cccm, max(av)+0.1 as xxxm from sss1 where cm>30)

        select name||' '||cas.class_name as names, av from sss2, sss1
        inner join class_and_students as cas on cas.students_login=login
        where (cas.class_name ~ '\d{1,2}[а-я]{1,1}$')=true and cm>30 and av IS NOT NULL
        order by (7*av/xxxm+cm/cccm) desc limit 20;

        END IF;

        END
        $_$;


ALTER FUNCTION public.top21(topic_work text) OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: authorities; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.authorities (
    login character varying(20) NOT NULL,
    authority character varying(10) NOT NULL
);


ALTER TABLE public.authorities OWNER TO fmh;

--
-- Name: TABLE authorities; Type: COMMENT; Schema: public; Owner: fmh
--

COMMENT ON TABLE public.authorities IS 'рівень доступу';


--
-- Name: class; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.class (
    class_name character varying(8) NOT NULL
);


!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

ALTER TABLE public.class OWNER TO fmh;

--
-- Name: TABLE class; Type: COMMENT; Schema: public; Owner: fmh
--

COMMENT ON TABLE public.class IS 'довідник класів';


--
-- Name: class_and_students; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.class_and_students (
    class_name character varying(8) NOT NULL,
    students_login character varying(20) NOT NULL
);


ALTER TABLE public.class_and_students OWNER TO fmh;

--
-- Name: prepod; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.prepod (
    schedule_id bigint NOT NULL,
    subject character varying(30) NOT NULL,
    login character varying(20) NOT NULL
);


ALTER TABLE public.prepod OWNER TO fmh;

--
-- Name: schedule_and_class; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.schedule_and_class (
    schedules_id bigint NOT NULL,
    class_name character varying(8) NOT NULL
);


ALTER TABLE public.schedule_and_class OWNER TO fmh;

--
-- Name: subject_list; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.subject_list (
    subject character varying(30) NOT NULL
);


ALTER TABLE public.subject_list OWNER TO fmh;

--
-- Name: free_cl_and_subh; Type: VIEW; Schema: public; Owner: fmh
--

CREATE VIEW public.free_cl_and_subh AS
 SELECT x.c1,
    x.s1
   FROM ( SELECT class.class_name AS c1,
            subject_list.subject AS s1
           FROM public.class,
            public.subject_list) x
  WHERE ((NOT (((x.c1)::text || (x.s1)::text) IN ( SELECT ((schedule_and_class.class_name)::text || (prepod.subject)::text)
           FROM (public.prepod
             JOIN public.schedule_and_class ON ((prepod.schedule_id = schedule_and_class.schedules_id)))))) AND ("substring"((x.c1)::text, '[\d]+'::text) = "substring"((x.s1)::text, '[\d]+'::text)))
  ORDER BY x.c1;


ALTER TABLE public.free_cl_and_subh OWNER TO fmh;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: fmh
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO fmh;

--
-- Name: journal; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.journal (
    id bigint NOT NULL,
    comment character varying(40),
    date date,
    mark integer NOT NULL,
    present boolean DEFAULT true NOT NULL,
    show_date boolean DEFAULT true NOT NULL,
    subject character varying(30) NOT NULL,
    topic character varying(25),
    work character varying(25),
    stud_login character varying(20) NOT NULL,
    class_name character varying(8) NOT NULL,
    CONSTRAINT journal_mark_check CHECK (((mark >= 0) AND (mark <= 12)))
);


ALTER TABLE public.journal OWNER TO fmh;

--
-- Name: move; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.move (
    login character varying(20) NOT NULL,
    in_date date NOT NULL,
    in_order character varying(10),
    in_comment character varying(20),
    out_date date,
    out_comment character varying(20),
    out_order character varying(10)
);


ALTER TABLE public.move OWNER TO fmh;

--
-- Name: TABLE move; Type: COMMENT; Schema: public; Owner: fmh
--

COMMENT ON TABLE public.move IS 'рух учнів';


--
-- Name: prepod4cl_and_subh; Type: VIEW; Schema: public; Owner: fmh
--

CREATE VIEW public.prepod4cl_and_subh AS
 SELECT sl.class_name,
    p.subject,
    p.login
   FROM (public.prepod p
     JOIN public.schedule_and_class sl ON ((sl.schedules_id = p.schedule_id)));


ALTER TABLE public.prepod4cl_and_subh OWNER TO fmh;

--
-- Name: prepod_id; Type: SEQUENCE; Schema: public; Owner: fmh
--

CREATE SEQUENCE public.prepod_id
    START WITH 7
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.prepod_id OWNER TO fmh;

--
-- Name: prepod_id; Type: SEQUENCE OWNED BY; Schema: public; Owner: fmh
--

ALTER SEQUENCE public.prepod_id OWNED BY public.prepod.schedule_id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.roles (
    role character varying(10) NOT NULL
);


ALTER TABLE public.roles OWNER TO fmh;

--
-- Name: spr_auth; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.spr_auth (
    login character varying(20) NOT NULL,
    authority character varying(10) NOT NULL
);


ALTER TABLE public.spr_auth OWNER TO fmh;

--
-- Name: spr_users; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.spr_users (
    login character varying(20) NOT NULL,
    password character varying(20) NOT NULL,
    enabled boolean NOT NULL,
    first_name character varying(50) NOT NULL,
    second_name character varying(50) NOT NULL,
    third_name character varying(50)
);


ALTER TABLE public.spr_users OWNER TO fmh;

--
-- Name: users; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.users (
    login character varying(20) NOT NULL,
    password character varying(20) NOT NULL,
    enabled boolean NOT NULL,
    first_name character varying(50) NOT NULL,
    second_name character varying(50) NOT NULL,
    third_name character varying(50)
);


ALTER TABLE public.users OWNER TO fmh;

--
-- Name: students; Type: VIEW; Schema: public; Owner: fmh
--

CREATE VIEW public.students AS
 SELECT cs.class_name,
    users.first_name,
    users.second_name,
    au.login,
    users.password
   FROM ((public.users
     FULL JOIN public.class_and_students cs ON (((cs.students_login)::text = (users.login)::text)))
     JOIN public.authorities au ON (((au.login)::text = (users.login)::text)))
  WHERE ((au.authority)::text <> 'prepod'::text)
  ORDER BY cs.class_name, users.first_name;


ALTER TABLE public.students OWNER TO fmh;

--
-- Name: teachers; Type: VIEW; Schema: public; Owner: fmh
--

CREATE VIEW public.teachers AS
 SELECT p.subject,
    sc.class_name,
    u.first_name,
    u.second_name,
    u.login,
    sc.schedules_id
   FROM ((public.users u
     JOIN public.prepod p ON (((p.login)::text = (u.login)::text)))
     JOIN public.schedule_and_class sc ON ((sc.schedules_id = p.schedule_id)))
  ORDER BY p.subject, u.first_name;


ALTER TABLE public.teachers OWNER TO fmh;

--
-- Name: topics; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.topics (
    sched4t_id bigint NOT NULL,
    topic character varying(25)
);


ALTER TABLE public.topics OWNER TO fmh;

--
-- Name: works; Type: TABLE; Schema: public; Owner: fmh
--

CREATE TABLE public.works (
    sched4w_id bigint NOT NULL,
    work character varying(25)
);


ALTER TABLE public.works OWNER TO fmh;

--
-- Name: schedule_id; Type: DEFAULT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.prepod ALTER COLUMN schedule_id SET DEFAULT nextval('public.prepod_id'::regclass);


--
-- Name: class_and_students_class_name_students_login_key; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.class_and_students
    ADD CONSTRAINT class_and_students_class_name_students_login_key UNIQUE (class_name, students_login);


--
-- Name: class_and_students_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.class_and_students
    ADD CONSTRAINT class_and_students_pkey PRIMARY KEY (class_name, students_login);


--
-- Name: class_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.class
    ADD CONSTRAINT class_pkey PRIMARY KEY (class_name);


--
-- Name: journal_date_stud_login_subject_key; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.journal
    ADD CONSTRAINT journal_date_stud_login_subject_key UNIQUE (date, stud_login, subject);


--
-- Name: journal_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.journal
    ADD CONSTRAINT journal_pkey PRIMARY KEY (id);


--
-- Name: move_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.move
    ADD CONSTRAINT move_pkey PRIMARY KEY (login);


--
-- Name: prepod_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.prepod
    ADD CONSTRAINT prepod_pkey PRIMARY KEY (schedule_id);

ALTER TABLE public.prepod CLUSTER ON prepod_pkey;


--
-- Name: roles_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role);


--
-- Name: subject_list_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.subject_list
    ADD CONSTRAINT subject_list_pkey PRIMARY KEY (subject);


--
-- Name: uk_login_auth; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT uk_login_auth UNIQUE (login, authority);


--
-- Name: ukokrx10t2tq9d3cabgvdbbhawf; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.prepod
    ADD CONSTRAINT ukokrx10t2tq9d3cabgvdbbhawf UNIQUE (login, subject);


--
-- Name: uktkl0yoaivqdwvu8qp5iyjgcy4; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.schedule_and_class
    ADD CONSTRAINT uktkl0yoaivqdwvu8qp5iyjgcy4 UNIQUE (schedules_id, class_name);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (login);


--
-- Name: journal_class_name_date_subject_idx; Type: INDEX; Schema: public; Owner: fmh
--

CREATE INDEX journal_class_name_date_subject_idx ON public.journal USING btree (class_name, date, subject);


--
-- Name: authorities_authority_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT authorities_authority_fkey FOREIGN KEY (authority) REFERENCES public.roles(role) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: authorities_login_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT authorities_login_fkey FOREIGN KEY (login) REFERENCES public.users(login) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: class_and_students_class_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.class_and_students
    ADD CONSTRAINT class_and_students_class_name_fkey FOREIGN KEY (class_name) REFERENCES public.class(class_name) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: class_and_students_students_login_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.class_and_students
    ADD CONSTRAINT class_and_students_students_login_fkey FOREIGN KEY (students_login) REFERENCES public.users(login) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: journal_class_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.journal
    ADD CONSTRAINT journal_class_name_fkey FOREIGN KEY (class_name) REFERENCES public.class(class_name) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: journal_stud_login_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.journal
    ADD CONSTRAINT journal_stud_login_fkey FOREIGN KEY (stud_login) REFERENCES public.users(login) ON UPDATE CASCADE;


--
-- Name: journal_subject_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.journal
    ADD CONSTRAINT journal_subject_fkey FOREIGN KEY (subject) REFERENCES public.subject_list(subject) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: move_login_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.move
    ADD CONSTRAINT move_login_fkey FOREIGN KEY (login) REFERENCES public.users(login) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: prepod_login_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.prepod
    ADD CONSTRAINT prepod_login_fkey FOREIGN KEY (login) REFERENCES public.users(login) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: prepod_subject_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.prepod
    ADD CONSTRAINT prepod_subject_fkey FOREIGN KEY (subject) REFERENCES public.subject_list(subject) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: schedule_and_class_class_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.schedule_and_class
    ADD CONSTRAINT schedule_and_class_class_name_fkey FOREIGN KEY (class_name) REFERENCES public.class(class_name) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: schedule_and_class_schedules_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.schedule_and_class
    ADD CONSTRAINT schedule_and_class_schedules_id_fkey FOREIGN KEY (schedules_id) REFERENCES public.prepod(schedule_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: topics_sched4t_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.topics
    ADD CONSTRAINT topics_sched4t_id_fkey FOREIGN KEY (sched4t_id) REFERENCES public.prepod(schedule_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: works_sched4w_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: fmh
--

ALTER TABLE ONLY public.works
    ADD CONSTRAINT works_sched4w_id_fkey FOREIGN KEY (sched4w_id) REFERENCES public.prepod(schedule_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- Name: TABLE authorities; Type: ACL; Schema: public; Owner: fmh
--

REVOKE ALL ON TABLE public.authorities FROM PUBLIC;
REVOKE ALL ON TABLE public.authorities FROM fmh;
GRANT ALL ON TABLE public.authorities TO fmh;


--
-- Name: TABLE users; Type: ACL; Schema: public; Owner: fmh
--

REVOKE ALL ON TABLE public.users FROM PUBLIC;
REVOKE ALL ON TABLE public.users FROM fmh;
GRANT ALL ON TABLE public.users TO fmh;


COPY public.roles (role) FROM stdin;
stud
admin
secretar
clmaster
prepod
\.


COPY public.spr_auth (login, authority) FROM stdin;
admin     admin
director  director
\.






-- DEAR NEWCOMER!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
--
--                          ||||||||||||||
--                          ______________
--                         /              \
--                        |                |
--                       __ *****    ***** __ 
--                      (    (*)  ||   (*)   )
--                       (_       ||       _)
--                         |              |
--                          \    ====    /
--                            +________+
--
--
-- 1) ENTER YOUR CLASS STRUCTURE SPECIFIC FOR YOUR SCHOOL HERE:
--

COPY public.class (class_name) FROM stdin;
--
-- TYPE THE CLASS NAMES HERE IN A COLUMN (INSTEAD OF AN EXAMPLE BELLOW)
--
4а
4б
4в
4г
5а
5б
5в
5г
6а
6б
6в
6г
7а
7б
7в
7г
8а
8б
8в
8г
9а
9б
9в
9г
10а
10б
10в
10г
11а
11б
11в
11г
12а
12б
12в
12г
\.



-- 2) ENTER YOUR PASSWORDS TO LOGIN TO CLASS SETUP WEB APP
-- 'admin' is  allowed to edit all classes and schedules, add teachers and kids
-- 'director' - to view only (with a cup of coffe)


COPY public.spr_users (login, password, enabled, first_name, second_name, third_name) FROM stdin;
--
-- HERE CHANGE THE PASSWORD1 and PASSWORD2 TO THE REAL ONES
--
admin           password1                t       Иванов Иван Иванович
director        password2                t       Петров Петр Петрович
\.



-- WELL DONE!!!
-- LAUNCH IT NOW!
