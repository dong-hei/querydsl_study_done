package study.querydslstudy.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydslstudy.dto.MemberSearchCondition;
import study.querydslstudy.dto.MemberTeamDto;
import study.querydslstudy.entity.Member;
import study.querydslstudy.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() throws Exception {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAllQuerydsl();
        assertThat(result1).containsExactly(member);


        List<Member> result2 = memberJpaRepository.findByUserNameQuerydsl("member1");
        assertThat(result2).containsExactly(member);

    }

    @Test
    public void searchTest() throws Exception {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member m1 = new Member("m1", 15, teamA);
        Member m2 = new Member("m2", 21, teamA);

        Member m3 = new Member("m3", 52, teamB);
        Member m4 = new Member("m4", 62, teamB);

        em.persist(m1);
        em.persist(m2);
        em.persist(m3);
        em.persist(m4);

        MemberSearchCondition cond = new MemberSearchCondition();
        cond.setAgeGoe(50);
        cond.setAgeLoe(55);
        cond.setTeamName("teamB"); // 데이터에 조건이 없으면 쿼리를 다 끌고온다. 즉 조건은 꼭 필요하다.

//        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(cond);
        List<MemberTeamDto> result = memberJpaRepository.search(cond);

        assertThat(result).extracting("username").containsExactly("m3");
    }






}