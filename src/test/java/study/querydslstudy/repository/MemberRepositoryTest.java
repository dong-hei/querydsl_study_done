package study.querydslstudy.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydslstudy.dto.MemberSearchCondition;
import study.querydslstudy.dto.MemberTeamDto;
import study.querydslstudy.entity.Member;
import study.querydslstudy.entity.QMember;
import study.querydslstudy.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydslstudy.entity.QMember.member;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() throws Exception {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);


        List<Member> result2 = memberRepository.findByUsername("member1");
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
        cond.setAgeLoe(60);
        cond.setTeamName("teamB");

//        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(cond);
        List<MemberTeamDto> result = memberRepository.search(cond);

        assertThat(result).extracting("username").containsExactly("m3");
    }

    @Test
    public void searchPageSimple() throws Exception {
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
        PageRequest pageReq = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(cond, pageReq);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("m1", "m2", "m3");
    }

    /**
     * 인터페이스 지원 - QuerydslPredicateExecutor
     * 단점 조인X (leftjoin 불가능)
     * 클라이언트가 Querydsl에 의존해야 한다.
     * 복잡한 실무 환경에서 사용하기엔 한계가 명확하다.
     */
    @Test
    public void querydslPredicateExecutorTest() throws Exception {
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

        Iterable<Member> result = memberRepository
                .findAll(member.age.between(10, 40)
                .and(member.username.eq("member1")));

       for (Member findMember : result) {
            System.out.println("findMember = " + findMember);
        }


    }

}